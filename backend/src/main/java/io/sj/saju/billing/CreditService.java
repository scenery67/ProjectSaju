package io.sj.saju.billing;

import io.sj.saju.notification.NotificationService;
import io.sj.saju.notification.NotificationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유일한 크레딧 증감 진입점. user_account.credit_balance를 직접 UPDATE하지
 * 않는 이유는 두 가지다: (1) 동시 요청에서의 이중 차감을 막으려면 SQL 조건절
 * (balance >= amount)로 원자적으로 검사+차감해야 하고, (2) 모든 증감을
 * credit_transaction 원장에 같은 트랜잭션으로 남겨야 감사(audit)가 된다.
 */
@Service
public class CreditService {

    private final JdbcTemplate jdbcTemplate;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final CreditPackageRepository creditPackageRepository;
    private final AdminActionLogService adminActionLogService;
    private final TossPaymentsClient tossPaymentsClient;
    private final NotificationService notificationService;

    // user_account.credit_balance는 JPA를 거치지 않는 원자적 raw SQL로
    // 바꾼다(동시 요청 이중 차감 방지). 그래서 같은 트랜잭션 안에서: (1) 이
    // 호출 전에 걸려 있는 Hibernate의 지연된 쓰기(예: payment 저장)가 아직
    // DB에 안 나갔으면 raw SQL이 그걸 못 보고, (2) 호출 후에는 Hibernate
    // 1차 캐시가 예전 값을 들고 있어 이후 JPA 조회가 stale한 값을 돌려준다.
    // flush()로 (1)을, clear()로 (2)를 막는다.
    @PersistenceContext
    private EntityManager entityManager;

    public CreditService(
            JdbcTemplate jdbcTemplate,
            CreditTransactionRepository creditTransactionRepository,
            PaymentRepository paymentRepository,
            CreditPackageRepository creditPackageRepository,
            AdminActionLogService adminActionLogService,
            TossPaymentsClient tossPaymentsClient,
            NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.creditTransactionRepository = creditTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.creditPackageRepository = creditPackageRepository;
        this.adminActionLogService = adminActionLogService;
        this.tossPaymentsClient = tossPaymentsClient;
        this.notificationService = notificationService;
    }

    /** LLM 상담 질문 1건에 크레딧을 차감한다. 잔액 부족이면 예외를 던진다. */
    @Transactional
    public void consume(UUID userAccountId, int amount, UUID referenceId, String note) {
        requirePositive(amount);
        int balanceAfter = decreaseBalance(userAccountId, amount);
        record(userAccountId, CreditTransactionType.CONSUME, -amount, balanceAfter, referenceId, note);
    }

    /** 무료 지급(가입 보너스, 출석 보상 등). */
    @Transactional
    public void grantFree(UUID userAccountId, int amount, UUID referenceId, String note) {
        grant(userAccountId, amount, CreditTransactionType.FREE_GRANT, referenceId, note);
    }

    /** 패키지를 고른 시점에 PENDING 결제 레코드를 만든다. 아직 크레딧은 지급하지 않는다. */
    @Transactional
    public Payment createPendingPurchase(UUID userAccountId, UUID creditPackageId) {
        CreditPackage creditPackage = creditPackageRepository.findById(creditPackageId)
                .filter(CreditPackage::isActive)
                .orElseThrow(() -> new IllegalArgumentException(
                        "credit package not found or inactive: " + creditPackageId));
        Payment payment = new Payment(
                userAccountId, creditPackage.getId(), creditPackage.getPriceKrw(), creditPackage.getCreditAmount());
        return paymentRepository.save(payment);
    }

    /**
     * PG 결제 확인 콜백에서 호출 — 결제를 COMPLETED로 바꾸고 같은 트랜잭션에서
     * 크레딧을 지급한다. 지급만 하고 결제 상태 갱신이 실패하는(또는 그 반대)
     * 상황을 막기 위해 하나의 트랜잭션으로 묶는다.
     */
    @Transactional
    public Payment completePurchase(UUID paymentId, String pgProvider, String pgTransactionId) {
        Payment payment = requirePayment(paymentId);
        payment.markCompleted(pgProvider, pgTransactionId);
        paymentRepository.save(payment);
        grant(payment.getUserAccountId(), payment.getCreditAmount(), CreditTransactionType.PURCHASE, payment.getId(), null);
        notificationService.notify(payment.getUserAccountId(), NotificationType.PAYMENT_COMPLETED, "충전 완료",
                "%,d 크레딧이 충전됐어요.".formatted(payment.getCreditAmount()), payment.getCreditAmount());
        return payment;
    }

    /** PG 승인 확인이 실패했을 때 호출 — 크레딧은 지급하지 않고 상태만 남긴다. */
    @Transactional
    public Payment failPurchase(UUID paymentId, String reason) {
        Payment payment = requirePayment(paymentId);
        payment.markFailed(reason);
        return paymentRepository.save(payment);
    }

    /**
     * 토스 결제창에서 돌아온 뒤 호출 — 프론트가 보낸 paymentKey/amount를
     * 그대로 믿지 않고 토스 서버에 직접 재확인한 뒤에만 크레딧을 지급한다.
     * 본인 소유의 PENDING 결제가 아니거나(다른 사람 결제 가로채기 시도),
     * 이미 처리된 결제거나, 금액이 안 맞으면 토스를 부르기 전에 막는다.
     */
    @Transactional
    public Payment confirmTossPurchase(UUID userAccountId, UUID paymentId, String paymentKey, int amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .filter(p -> userAccountId.equals(p.getUserAccountId()))
                .orElseThrow(() -> new NoSuchElementException("payment not found: " + paymentId));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("이미 처리된 결제예요");
        }
        if (payment.getAmountKrw() != amount) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않아요");
        }
        try {
            tossPaymentsClient.confirmPayment(paymentKey, paymentId.toString(), amount);
        } catch (TossPaymentFailedException e) {
            failPurchase(paymentId, e.getMessage());
            throw e;
        }
        return completePurchase(paymentId, "TOSS", paymentKey);
    }

    /**
     * 관리자 환불 처리. 이미 상담에 써버린 크레딧이 있어도 환불 승인 자체는
     * 관리자 판단이므로 막지 않되, 잔액이 음수가 되지는 않게 0에서 clamp한다.
     */
    @Transactional
    public Payment refund(UUID paymentId, UUID adminUserAccountId, String reason) {
        Payment payment = requirePayment(paymentId);
        payment.markRefunded(adminUserAccountId, reason);
        paymentRepository.save(payment);
        int balanceAfter = decreaseBalanceClamped(payment.getUserAccountId(), payment.getCreditAmount());
        record(payment.getUserAccountId(), CreditTransactionType.REFUND, -payment.getCreditAmount(), balanceAfter,
                payment.getId(), reason);
        adminActionLogService.log(adminUserAccountId, payment.getUserAccountId(), AdminActionType.REFUND_PAYMENT,
                "결제 %s 환불 (%d크레딧) — %s".formatted(payment.getId(), payment.getCreditAmount(), reason));
        return payment;
    }

    /**
     * 계정 탈퇴 처리 중 호출 — 남은 크레딧을 전부 REFUND로 원장에 남기고 0으로
     * 만든다. 특정 결제 하나가 아니라 "이 시점의 잔액 전체"가 대상이라
     * refund(paymentId, ...)와는 별도 메서드다. 잔액이 이미 0이면 아무 것도
     * 안 한다(빈 원장 행을 남기지 않는다).
     */
    @Transactional
    public void refundRemainingBalanceOnAccountDeletion(UUID userAccountId, UUID adminUserAccountId) {
        entityManager.flush();
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT credit_balance FROM user_account WHERE id = ?", Integer.class, userAccountId);
        if (balance == null || balance <= 0) {
            return;
        }
        int balanceAfter = decreaseBalanceClamped(userAccountId, balance);
        record(userAccountId, CreditTransactionType.REFUND, -balance, balanceAfter, adminUserAccountId,
                "계정 탈퇴로 인한 잔여 크레딧 환급 처리");
    }

    /** 관리자가 임의로 크레딧을 더하거나(양수) 회수하는(음수) 수동 조정. */
    @Transactional
    public void adminAdjust(UUID targetUserAccountId, int amount, UUID adminUserAccountId, String reason) {
        if (amount == 0) {
            throw new IllegalArgumentException("amount must not be zero");
        }
        int balanceAfter = amount > 0
                ? increaseBalance(targetUserAccountId, amount)
                : decreaseBalanceClamped(targetUserAccountId, -amount);
        record(targetUserAccountId, CreditTransactionType.ADMIN_ADJUST, amount, balanceAfter,
                adminUserAccountId, reason);
        adminActionLogService.log(adminUserAccountId, targetUserAccountId, AdminActionType.CREDIT_ADJUST,
                "%+d크레딧 — %s".formatted(amount, reason));
    }

    private Payment requirePayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("payment not found: " + paymentId));
    }

    private void grant(UUID userAccountId, int amount, CreditTransactionType type, UUID referenceId, String note) {
        requirePositive(amount);
        int balanceAfter = increaseBalance(userAccountId, amount);
        record(userAccountId, type, amount, balanceAfter, referenceId, note);
    }

    private void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private int increaseBalance(UUID userAccountId, int amount) {
        entityManager.flush();
        int balanceAfter = jdbcTemplate.queryForObject(
                "UPDATE user_account SET credit_balance = credit_balance + ? WHERE id = ? RETURNING credit_balance",
                Integer.class, amount, userAccountId);
        entityManager.clear();
        return balanceAfter;
    }

    /** balance >= amount일 때만 차감 — 동시 요청에 의한 잔액 음수화(이중 사용)를 DB에서 막는다. */
    private int decreaseBalance(UUID userAccountId, int amount) {
        entityManager.flush();
        try {
            int balanceAfter = jdbcTemplate.queryForObject(
                    "UPDATE user_account SET credit_balance = credit_balance - ? "
                            + "WHERE id = ? AND credit_balance >= ? RETURNING credit_balance",
                    Integer.class, amount, userAccountId, amount);
            entityManager.clear();
            return balanceAfter;
        } catch (EmptyResultDataAccessException e) {
            throw new InsufficientCreditException(userAccountId);
        }
    }

    /** 환불/관리자 회수 전용 — 잔액이 모자라도 실패시키지 않고 0에서 멈춘다. */
    private int decreaseBalanceClamped(UUID userAccountId, int amount) {
        entityManager.flush();
        int balanceAfter = jdbcTemplate.queryForObject(
                "UPDATE user_account SET credit_balance = GREATEST(credit_balance - ?, 0) "
                        + "WHERE id = ? RETURNING credit_balance",
                Integer.class, amount, userAccountId);
        entityManager.clear();
        return balanceAfter;
    }

    private void record(
            UUID userAccountId, CreditTransactionType type, int amount, int balanceAfter,
            UUID referenceId, String note) {
        creditTransactionRepository.save(
                new CreditTransaction(userAccountId, type, amount, balanceAfter, referenceId, note));
    }
}
