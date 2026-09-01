package io.sj.saju.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A purchasable credit pack (질문 N회). Priced as data, not code, so the
 * lineup can change without a deploy — see V3__create_credit_billing.sql for
 * the current ladder and its pricing rationale.
 */
@Entity
@Table(name = "credit_package")
public class CreditPackage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int creditAmount;

    @Column(nullable = false)
    private int priceKrw;

    // 컬럼명이 is_active라 명시 매핑 필요 — 기본 네이밍 전략은 active를
    // is_active가 아니라 active로 매핑해 컬럼을 못 찾는다.
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private Instant createdAt;

    protected CreditPackage() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCreditAmount() {
        return creditAmount;
    }

    public int getPriceKrw() {
        return priceKrw;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
