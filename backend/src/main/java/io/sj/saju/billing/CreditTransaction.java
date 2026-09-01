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
 * Append-only ledger row for every credit change. This — not
 * user_account.creditBalance — is the source of truth for "왜 잔액이
 * 이렇게 됐는지"; the balance column is just a cache kept in sync with it.
 */
@Entity
@Table(name = "credit_transaction")
public class CreditTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_account_id", nullable = false)
    private UUID userAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTransactionType type;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private int balanceAfter;

    private UUID referenceId;

    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    protected CreditTransaction() {
        // JPA
    }

    public CreditTransaction(
            UUID userAccountId, CreditTransactionType type, int amount, int balanceAfter,
            UUID referenceId, String note) {
        this.userAccountId = userAccountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public CreditTransactionType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
