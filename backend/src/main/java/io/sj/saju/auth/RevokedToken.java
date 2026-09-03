package io.sj.saju.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One explicitly logged-out JWT, keyed by its jti claim. See V7 migration. */
@Entity
@Table(name = "revoked_token")
public class RevokedToken {

    @Id
    private UUID jti;

    private Instant expiresAt;

    protected RevokedToken() {
        // JPA
    }

    public RevokedToken(UUID jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public UUID getJti() {
        return jti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
