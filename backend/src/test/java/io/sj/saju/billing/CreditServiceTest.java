package io.sj.saju.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sj.saju.auth.OAuthProvider;
import io.sj.saju.auth.UserAccount;
import io.sj.saju.auth.UserAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 로컬 Postgres(docker-compose)를 쓰는 통합 테스트다. CreditService는
 * 동시성 안전을 위해 RETURNING 절이 있는 원자적 SQL(UPDATE ... WHERE
 * credit_balance >= ?)을 쓰는데, 이건 실제 DB 방언 위에서만 검증 의미가
 * 있어 인메모리 DB로 대체하지 않았다. 클래스에 @Transactional을 걸어 각
 * 테스트가 끝나면 자동 롤백되게 해서 시드 데이터(credit_package)를 건드리지
 * 않는다.
 */
@SpringBootTest
@Transactional
class CreditServiceTest {

    @Autowired
    private CreditService creditService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CreditPackageRepository creditPackageRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private AdminActionLogRepository adminActionLogRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        // saveAndFlush: CreditService의 잔액 갱신은 JPA를 거치지 않는 원자적
        // JDBC(RETURNING)라서, Hibernate가 지연시킨 INSERT가 아직 실제 DB에
        // 반영되기 전이면 "대상 행 없음"으로 실패한다 — 즉시 flush해서 방지.
        user = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "test-" + UUID.randomUUID(), "테스터"));
    }

    @Test
    void grantFreeIncreasesBalanceAndLeavesALedgerRow() {
        creditService.grantFree(user.getId(), 5, null, "가입 보너스");

        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance()).isEqualTo(5);
        var transactions = creditTransactionRepository.findByUserAccountIdOrderByCreatedAtDesc(user.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getType()).isEqualTo(CreditTransactionType.FREE_GRANT);
        assertThat(transactions.get(0).getBalanceAfter()).isEqualTo(5);
    }

    @Test
    void consumeThrowsWhenBalanceIsInsufficientAndLeavesBalanceUnchanged() {
        creditService.grantFree(user.getId(), 2, null, "seed");

        assertThatThrownBy(() -> creditService.consume(user.getId(), 3, null, "질문"))
                .isInstanceOf(InsufficientCreditException.class);

        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance()).isEqualTo(2);
    }

    @Test
    void consumeSucceedsWhenBalanceIsExactlyEnough() {
        creditService.grantFree(user.getId(), 3, null, "seed");

        creditService.consume(user.getId(), 3, null, "질문");

        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance()).isEqualTo(0);
    }

    @Test
    void purchaseFlowCreatesPendingPaymentThenGrantsCreditsOnCompletion() {
        CreditPackage pkg = creditPackageRepository.findByActiveTrueOrderBySortOrderAsc().get(0);

        Payment pending = creditService.createPendingPurchase(user.getId(), pkg.getId());
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.PENDING);

        Payment completed = creditService.completePurchase(pending.getId(), "TEST_PG", "tx-1");

        assertThat(completed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance())
                .isEqualTo(pkg.getCreditAmount());
    }

    @Test
    void refundClampsBalanceAtZeroEvenIfCreditsWereAlreadySpent() {
        CreditPackage pkg = creditPackageRepository.findByActiveTrueOrderBySortOrderAsc().get(0);
        Payment payment = creditService.completePurchase(
                creditService.createPendingPurchase(user.getId(), pkg.getId()).getId(), "TEST_PG", "tx-2");
        // 지급받은 크레딧을 이미 다 써버린 상황을 재현한다 — 환불이 잔액을
        // 음수로 만들면 안 된다는 게 이 테스트의 핵심.
        creditService.consume(user.getId(), pkg.getCreditAmount(), null, "질문");
        UserAccount admin = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "admin-" + UUID.randomUUID(), "관리자"));

        Payment refunded = creditService.refund(payment.getId(), admin.getId(), "고객 요청");

        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refunded.getRefundedBy()).isEqualTo(admin.getId());
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance()).isEqualTo(0);

        boolean logged = adminActionLogRepository.findAll().stream()
                .anyMatch(l -> l.getActionType() == AdminActionType.REFUND_PAYMENT
                        && admin.getId().equals(l.getAdminUserAccountId())
                        && user.getId().equals(l.getTargetUserAccountId()));
        assertThat(logged).isTrue();
    }

    @Test
    void confirmTossPurchaseRejectsAPaymentBelongingToSomeoneElse() {
        CreditPackage pkg = creditPackageRepository.findByActiveTrueOrderBySortOrderAsc().get(0);
        Payment payment = creditService.createPendingPurchase(user.getId(), pkg.getId());
        UUID someoneElse = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "other-" + UUID.randomUUID(), "다른사람")).getId();

        assertThatThrownBy(() -> creditService.confirmTossPurchase(someoneElse, payment.getId(), "pay_1", pkg.getPriceKrw()))
                .isInstanceOf(java.util.NoSuchElementException.class);
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void confirmTossPurchaseRejectsAnAmountThatDoesNotMatchThePayment() {
        CreditPackage pkg = creditPackageRepository.findByActiveTrueOrderBySortOrderAsc().get(0);
        Payment payment = creditService.createPendingPurchase(user.getId(), pkg.getId());

        assertThatThrownBy(() -> creditService.confirmTossPurchase(
                user.getId(), payment.getId(), "pay_1", pkg.getPriceKrw() + 1_000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void confirmTossPurchaseRejectsAnAlreadyProcessedPayment() {
        CreditPackage pkg = creditPackageRepository.findByActiveTrueOrderBySortOrderAsc().get(0);
        Payment payment = creditService.completePurchase(
                creditService.createPendingPurchase(user.getId(), pkg.getId()).getId(), "TEST_PG", "tx-already");
        int balanceAfterFirstCompletion = userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance();

        assertThatThrownBy(() -> creditService.confirmTossPurchase(
                user.getId(), payment.getId(), "pay_1", pkg.getPriceKrw()))
                .isInstanceOf(IllegalArgumentException.class);
        // 이미 완료된 결제를 다시 확인하려 해도 크레딧이 또 지급되면 안 된다.
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance())
                .isEqualTo(balanceAfterFirstCompletion);
    }

    @Test
    void confirmTossPurchaseMarksThePaymentFailedWhenTossIsNotConfigured() {
        // 테스트 환경에는 TOSS_SECRET_KEY를 안 넣어뒀으니, TossPaymentsClient가
        // 항상 TossPaymentFailedException을 던진다 — 그 실패 경로(크레딧 미지급,
        // 상태를 FAILED로 남김)를 검증한다.
        CreditPackage pkg = creditPackageRepository.findByActiveTrueOrderBySortOrderAsc().get(0);
        Payment payment = creditService.createPendingPurchase(user.getId(), pkg.getId());

        assertThatThrownBy(() -> creditService.confirmTossPurchase(
                user.getId(), payment.getId(), "pay_1", pkg.getPriceKrw()))
                .isInstanceOf(TossPaymentFailedException.class);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(reloaded.getFailReason()).isNotBlank();
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance()).isZero();
    }

    @Test
    void adminAdjustCanGrantOrClawBackCredits() {
        // adminAdjust는 admin_action_log에도 감사 로그를 남기는데, 그 컬럼은
        // 실제 user_account를 가리키는 FK라 UUID.randomUUID() 같은 가짜 값을
        // 못 쓴다 — 실제로 저장된 관리자 계정이어야 한다.
        UserAccount admin = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "admin-" + UUID.randomUUID(), "관리자"));

        creditService.adminAdjust(user.getId(), 10, admin.getId(), "보상 지급");
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance()).isEqualTo(10);

        creditService.adminAdjust(user.getId(), -4, admin.getId(), "오지급 회수");
        assertThat(userAccountRepository.findById(user.getId()).orElseThrow().getCreditBalance()).isEqualTo(6);

        var logs = adminActionLogRepository.findAll().stream()
                .filter(l -> l.getActionType() == AdminActionType.CREDIT_ADJUST
                        && admin.getId().equals(l.getAdminUserAccountId())
                        && user.getId().equals(l.getTargetUserAccountId()))
                .toList();
        assertThat(logs).hasSize(2);
    }
}
