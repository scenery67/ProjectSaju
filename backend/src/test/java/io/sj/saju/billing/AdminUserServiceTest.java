package io.sj.saju.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sj.saju.auth.OAuthProvider;
import io.sj.saju.auth.UserAccount;
import io.sj.saju.auth.UserAccountRepository;
import io.sj.saju.persona.PersonaType;
import io.sj.saju.reading.ReadingRecord;
import io.sj.saju.reading.ReadingRecordRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 실제 로컬 Postgres를 쓰는 통합 테스트 — CreditServiceTest와 같은 이유(계정 삭제의 FK/CASCADE 동작은 실제 DB에서만 검증 의미가 있다). */
@SpringBootTest
@Transactional
class AdminUserServiceTest {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private CreditService creditService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CreditPackageRepository creditPackageRepository;

    @Autowired
    private ReadingRecordRepository readingRecordRepository;

    private UserAccount admin;
    private UserAccount target;

    @BeforeEach
    void setUp() {
        admin = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "admin-" + UUID.randomUUID(), "관리자"));
        target = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "target-" + UUID.randomUUID(), "탈퇴대상"));
    }

    @Test
    void listUsersIncludesNewlyCreatedAccounts() {
        assertThat(adminUserService.listUsers())
                .extracting(UserAccount::getId)
                .contains(admin.getId(), target.getId());
    }

    @Test
    void setAdminGrantsAndRevokesForAnotherAccount() {
        adminUserService.setAdmin(target.getId(), true, admin.getId());
        assertThat(userAccountRepository.findById(target.getId()).orElseThrow().isAdmin()).isTrue();

        adminUserService.setAdmin(target.getId(), false, admin.getId());
        assertThat(userAccountRepository.findById(target.getId()).orElseThrow().isAdmin()).isFalse();
    }

    @Test
    void adminCannotRevokeOwnAdminRole() {
        assertThatThrownBy(() -> adminUserService.setAdmin(admin.getId(), false, admin.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adminCannotDeleteOwnAccount() {
        assertThatThrownBy(() -> adminUserService.deleteUser(admin.getId(), admin.getId()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(userAccountRepository.findById(admin.getId())).isPresent();
    }

    @Test
    void deletingAUserRefundsRemainingCreditAndCascadesPersonalContentButKeepsFinancialRecords() {
        creditService.grantFree(target.getId(), 7, null, "seed");
        UUID grantTransactionId = creditTransactionRepository
                .findByUserAccountIdOrderByCreatedAtDesc(target.getId()).get(0).getId();
        CreditPackage pkg = creditPackageRepository.findByActiveTrueOrderBySortOrderAsc().get(0);
        Payment payment = paymentRepository.saveAndFlush(
                new Payment(target.getId(), pkg.getId(), pkg.getPriceKrw(), pkg.getCreditAmount()));
        ReadingRecord reading = readingRecordRepository.saveAndFlush(new ReadingRecord(
                PersonaType.BREAKUP, "탈퇴대상", null, "요약", "상세", target.getId(), null));

        adminUserService.deleteUser(target.getId(), admin.getId());

        // 계정 자체는 삭제된다.
        assertThat(userAccountRepository.findById(target.getId())).isEmpty();

        // 개인 콘텐츠(reading_record)는 CASCADE로 함께 삭제된다.
        assertThat(readingRecordRepository.findById(reading.getId())).isEmpty();

        // 결제(payment)와 크레딧 원장(credit_transaction) 행은 회계 기록이라
        // 삭제되지 않고 소유자만 NULL이 된다.
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getUserAccountId()).isNull();
        assertThat(creditTransactionRepository.findById(grantTransactionId).orElseThrow().getUserAccountId())
                .isNull();

        // 남은 크레딧(7개)은 삭제 직전에 REFUND로 원장에 남는다 — 계정이
        // 사라진 뒤라 findByUserAccountId로는 더 못 찾으니, 방금 남긴
        // 환급 사유로 찾는다.
        boolean refundLogged = creditTransactionRepository.findAll().stream()
                .anyMatch(t -> t.getType() == CreditTransactionType.REFUND
                        && t.getAmount() == -7
                        && "계정 탈퇴로 인한 잔여 크레딧 환급 처리".equals(t.getNote()));
        assertThat(refundLogged).isTrue();
    }

    @Test
    void deletingAUserWithZeroBalanceDoesNotCreateAnEmptyRefundEntry() {
        adminUserService.deleteUser(target.getId(), admin.getId());

        assertThat(userAccountRepository.findById(target.getId())).isEmpty();
    }

    @Test
    void deletingAnUnknownUserThrowsNotFound() {
        assertThatThrownBy(() -> adminUserService.deleteUser(UUID.randomUUID(), admin.getId()))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
