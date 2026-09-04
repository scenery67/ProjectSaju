package io.sj.saju.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    /** 관리자 화면의 사용자 목록 — 최근 가입순 50명. */
    List<UserAccount> findTop50ByOrderByCreatedAtDesc();

    /** 관리자 화면의 닉네임 검색 — 최근 가입순 최대 50건. */
    List<UserAccount> findTop50ByNicknameContainingIgnoreCaseOrderByCreatedAtDesc(String nickname);
}
