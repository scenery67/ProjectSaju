package io.sj.saju.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reads "Authorization: Bearer <jwt>" and authenticates the request as that user. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final TokenRevocationService tokenRevocationService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserAccountRepository userAccountRepository,
            TokenRevocationService tokenRevocationService) {
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            // jti가 없는(구버전) 토큰은 무효화 대상으로 삼을 수 없으니 서명/만료만
            // 검증하는 parseUserAccountId로 그대로 인증한다 — parseClaims가
            // null이면 로그아웃 지원 이전에 발급된 토큰이라는 뜻일 뿐, 그
            // 자체로 무효는 아니다.
            JwtService.TokenClaims claims = jwtService.parseClaims(token);
            UUID userAccountId = claims != null ? claims.userAccountId() : jwtService.parseUserAccountId(token);
            boolean revoked = claims != null && tokenRevocationService.isRevoked(claims.jti());
            if (userAccountId != null && !revoked) {
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                // 매 요청마다 조회하는 이유: 관리자 권한은 DB에서 직접 바뀌는데,
                // JWT는 발급 시점 값을 그대로 담고 있어 갱신되지 않기 때문이다.
                userAccountRepository.findById(userAccountId)
                        .filter(UserAccount::isAdmin)
                        .ifPresent(account -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN")));
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        userAccountId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
