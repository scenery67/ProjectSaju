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

    @Enumerated(EnumType.STRING)
    @Column(name = "avatar_key", nullable = false)
    private AvatarPreset avatarKey;

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

    // 테스트/개발용 우회 계정처럼 아바타가 중요하지 않은 곳에서 쓰는 생성자 —
    // 기본 아바타(FOX)로 고정한다. 실제 가입 흐름은 아래 4-인자 생성자로
    // RandomProfileGenerator가 만든 닉네임/아바타를 명시적으로 넘긴다.
    public UserAccount(OAuthProvider provider, String providerUserId, String nickname) {
        this(provider, providerUserId, nickname, AvatarPreset.FOX);
    }

    public UserAccount(OAuthProvider provider, String providerUserId, String nickname, AvatarPreset avatarKey) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.nickname = nickname;
        this.avatarKey = avatarKey;
        this.createdAt = Instant.now();
        this.lastLoginAt = this.createdAt;
    }

    /**
     * 로그인 시각만 갱신한다 — 닉네임은 여기서 절대 건드리지 않는다. 예전엔
     * 매 로그인마다 공급자가 주는 닉네임(카카오는 종종 실명)으로 덮어써서,
     * 가입 때 랜덤 별명을 줘도 다음 로그인에 다시 실명으로 돌아가는 문제가
     * 있었다. 닉네임/아바타 변경은 오직 updateProfile()로만 한다.
     */
    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }

    public void updateProfile(String nickname, AvatarPreset avatarKey) {
        this.nickname = nickname;
        this.avatarKey = avatarKey;
    }

    // 상용화 전 관리자 우회 로그인(dev-admin-bypass)에서 고정 계정을 만들 때만
    // 쓴다 — 그 외에는 setAdmin()을 관리자 화면(AdminUserService)에서 쓴다.
    public void promoteToAdmin() {
        this.admin = true;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
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

    public AvatarPreset getAvatarKey() {
        return avatarKey;
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
