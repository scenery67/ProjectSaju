package io.sj.saju.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One credit-package purchase attempt. PG 연동 전이라 pgProvider/pgTransactionId는
 * 비어 있을 수 있다 — completePayment()가 실제 결제 확인 후 채운다.
 */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    // 계정 탈퇴 시 이 결제 기록 자체는 회계 감사를 위해 남기고 소유자만 NULL로
    // 바뀐다(V8 마이그레이션, ON DELETE SET NULL) — 그래서 nullable이다.
    @Column(name = "user_account_id")
    private UUID userAccountId;

    @Column(name = "credit_package_id", nullable = false)
    private UUID creditPackageId;

    private String pgProvider;

    private String pgTransactionId;

    @Column(nullable = false)
    private int amountKrw;

    @Column(nullable = false)
    private int creditAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;

    private Instant refundedAt;

    private UUID refundedBy;

    private String refundReason;

    private String failReason;

    protected Payment() {
        // JPA
    }

    public Payment(UUID userAccountId, UUID creditPackageId, int amountKrw, int creditAmount) {
        this.userAccountId = userAccountId;
        this.creditPackageId = creditPackageId;
        this.amountKrw = amountKrw;
        this.creditAmount = creditAmount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markCompleted(String pgProvider, String pgTransactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
        this.completedAt = Instant.now();
    }

    public void markRefunded(UUID adminUserAccountId, String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = Instant.now();
        this.refundedBy = adminUserAccountId;
        this.refundReason = reason;
    }

    /** PG(토스) 승인 확인이 실패했을 때 — 크레딧은 지급되지 않는다. */
    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public UUID getCreditPackageId() {
        return creditPackageId;
    }

    public String getPgProvider() {
        return pgProvider;
    }

    public String getPgTransactionId() {
        return pgTransactionId;
    }

    public int getAmountKrw() {
        return amountKrw;
    }

    public int getCreditAmount() {
        return creditAmount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public UUID getRefundedBy() {
        return refundedBy;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public String getFailReason() {
        return failReason;
    }
}
