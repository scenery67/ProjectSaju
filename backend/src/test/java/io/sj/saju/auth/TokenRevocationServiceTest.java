package io.sj.saju.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 실제 로컬 Postgres를 쓰는 통합 테스트 — CreditServiceTest와 같은 이유(원자적 SQL은 아니지만 스키마 검증 겸용). */
@SpringBootTest
@Transactional
class TokenRevocationServiceTest {

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Test
    void unrevokedJtiIsNotRevoked() {
        assertThat(tokenRevocationService.isRevoked(UUID.randomUUID())).isFalse();
    }

    @Test
    void revokingATokenMakesIsRevokedTrueForItsJti() {
        JwtService.TokenClaims claims = new JwtService.TokenClaims(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(3600));

        tokenRevocationService.revoke(claims);

        assertThat(tokenRevocationService.isRevoked(claims.jti())).isTrue();
    }

    @Test
    void revokingTheSameJtiTwiceDoesNotFail() {
        JwtService.TokenClaims claims = new JwtService.TokenClaims(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(3600));

        tokenRevocationService.revoke(claims);
        tokenRevocationService.revoke(claims);

        assertThat(tokenRevocationService.isRevoked(claims.jti())).isTrue();
    }
}
