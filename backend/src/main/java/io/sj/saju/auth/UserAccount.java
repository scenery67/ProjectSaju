package io.sj.saju.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * One OAuth-authenticated user. provider+providerUserId is the real identity —
 * we deliberately don't store email/phone (CLAUDE.md 3.2), only a display
 * nickname pulled from the provider profile.
 */
@Entity
@Table(name = "user_account", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_user_id"}))
public class UserAccount {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    private String nickname;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastLoginAt;

    // 원장(io.sj.saju.billing.CreditTransaction)이 실제 근거이고, 이 컬럼은
    // 빠른 조회용 캐시다 — 직접 대입하지 말고 항상 CreditService를 통해 바꾼다.
    @Column(nullable = false)
    private int creditBalance;

    // 결제 내역 조회, 환불/크레딧 수동 지급 같은 관리자 조치를 할 수 있는지 여부.
    // 지금은 관리자 화면이 없어 DB에서 직접 true로 바꿔야 한다.
    @Column(name = "is_admin", nullable = false)
    private boolean admin;

    protected UserAccount() {
        // JPA
    }

    public UserAccount(OAuthProvider provider, String providerUserId, String nickname) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.nickname = nickname;
        this.createdAt = Instant.now();
        this.lastLoginAt = this.createdAt;
    }

    public void recordLogin(String latestNickname) {
        this.nickname = latestNickname;
        this.lastLoginAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getNickname() {
        return nickname;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public int getCreditBalance() {
        return creditBalance;
    }

    public boolean isAdmin() {
        return admin;
    }
}
