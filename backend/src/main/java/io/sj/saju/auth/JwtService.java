package io.sj.saju.auth;

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
}
