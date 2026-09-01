package io.sj.saju.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    // 32+ bytes, matching the HS256 key-strength requirement the real
    // app.jwt.secret default in application.yml also satisfies.
    private final JwtService jwtService = new JwtService("test-only-jwt-secret-32-bytes-minimum-please");

    @Test
    void issuedTokenParsesBackToTheSameUserAccountId() {
        UUID userAccountId = UUID.randomUUID();

        String token = jwtService.issueToken(userAccountId);

        assertThat(jwtService.parseUserAccountId(token)).isEqualTo(userAccountId);
    }

    @Test
    void garbageTokenReturnsNullInsteadOfThrowing() {
        assertThat(jwtService.parseUserAccountId("not-a-real-jwt")).isNull();
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        UUID userAccountId = UUID.randomUUID();
        JwtService otherService = new JwtService("a-completely-different-secret-32-bytes-minimum");
        String token = otherService.issueToken(userAccountId);

        assertThat(jwtService.parseUserAccountId(token)).isNull();
    }
}
