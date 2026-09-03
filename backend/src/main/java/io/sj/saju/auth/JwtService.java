package io.sj.saju.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues/validates our own JWT after a successful OAuth2 login. We use our
 * own token (not the provider's) because the frontend (GitHub Pages) and
 * backend (Fly.io) are different origins — a stateless bearer token avoids
 * cross-site cookie issues entirely.
 */
@Service
public class JwtService {

    private static final Duration TOKEN_TTL = Duration.ofDays(30);

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(UUID userAccountId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userAccountId.toString())
                // 로그아웃 시 이 토큰만 콕 집어 무효화하려면 개별 식별자가 필요하다 —
                // TokenRevocationService/revoked_token 테이블이 이 jti를 키로 쓴다.
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_TTL)))
                .signWith(key)
                .compact();
    }

    /** @return the user account id encoded in the token, or null if invalid/expired. */
    public UUID parseUserAccountId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return UUID.fromString(subject);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public record TokenClaims(UUID userAccountId, UUID jti, Instant expiresAt) {
    }

    /**
     * parseUserAccountId보다 더 필요한 정보(jti, 만료 시각)까지 함께 돌려준다 —
     * 로그아웃(TokenRevocationService)이 "이 토큰을 무효화 목록에 남기려면
     * jti와, revoked_token 정리 기준이 될 만료 시각"이 둘 다 필요해서 추가했다.
     * jti가 없는(구버전) 토큰이면 null — 그런 토큰은 애초에 무효화 대상으로
     * 삼을 수 없으니 로그아웃 호출 쪽에서 조용히 무시하면 된다.
     *
     * @return 클레임, 또는 토큰이 무효/만료/jti 누락이면 null
     */
    public TokenClaims parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (claims.getId() == null || claims.getExpiration() == null) {
                return null;
            }
            return new TokenClaims(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.getId()),
                    claims.getExpiration().toInstant());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
