package io.sj.saju.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
