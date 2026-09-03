package io.sj.saju.auth;

import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 로그아웃한 JWT를 만료 전까지 거부하기 위한 최소 상태(revoked_token). JWT
 * 검증 자체(서명/만료)는 여전히 JwtService가 하고, 여기서는 "서명은 유효하지만
 * 이미 로그아웃된 토큰인지"만 추가로 확인한다.
 */
@Service
public class TokenRevocationService {

    private final RevokedTokenRepository repository;

    public TokenRevocationService(RevokedTokenRepository repository) {
        this.repository = repository;
    }

    public void revoke(JwtService.TokenClaims claims) {
        if (!repository.existsById(claims.jti())) {
            repository.save(new RevokedToken(claims.jti(), claims.expiresAt()));
        }
    }

    public boolean isRevoked(UUID jti) {
        return repository.existsById(jti);
    }
}
