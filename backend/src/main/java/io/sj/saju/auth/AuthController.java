package io.sj.saju.auth;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // 상용화 전 팀 내부에서만 쓰는 고정 계정 — 실제 OAuth 계정과 섞이지 않게
    // provider를 DEV_BYPASS로 둔다(V4 마이그레이션 참고).
    private static final String DEV_ADMIN_PROVIDER_USER_ID = "dev-admin-bypass";
    private static final String DEV_ADMIN_NICKNAME = "개발용 관리자";

    private final UserAccountRepository userAccountRepository;
    private final JwtService jwtService;
    private final String frontendUrl;
    private final boolean devAdminBypassEnabled;

    public AuthController(
            UserAccountRepository userAccountRepository,
            JwtService jwtService,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.dev-admin-bypass.enabled:false}") boolean devAdminBypassEnabled) {
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
        this.devAdminBypassEnabled = devAdminBypassEnabled;
    }

    /** For the frontend to check "am I logged in" and show a nickname — nothing else. */
    @GetMapping("/api/auth/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UUID userAccountId) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        return userAccountRepository.findById(userAccountId)
                .map(account -> ResponseEntity.ok(
                        new MeResponse(account.getProvider().name(), account.getNickname())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    /**
     * 실제 OAuth 앱 등록 전에 팀이 관리자 권한으로 써볼 수 있게 하는 임시
     * 우회 로그인. app.dev-admin-bypass.enabled(ADMIN_BYPASS_ENABLED)가
     * true일 때만 동작하고, 기본값은 false — 상용화 시점에 반드시 꺼야 한다.
     * 인증 없이 관리자 계정을 내주는 기능이라 매 사용을 로그로 남긴다.
     */
    @GetMapping("/api/auth/dev-admin-login")
    public void devAdminLogin(HttpServletResponse response) throws IOException {
        if (!devAdminBypassEnabled) {
            // sendError()가 아니라 setStatus() — sendError()는 컨테이너의
            // /error 포워딩을 태우는데, /error는 permitAll 목록에 없어서
            // Spring Security가 그 요청을 다시 막고 401로 덮어써 버린다
            // (오프일 때 404 대신 401이 나오던 원인 — GET /api/saju/breakup이
            // 405 대신 401을 반환하는 것도 같은 매커니즘).
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        log.warn("dev-admin-bypass login used — ADMIN_BYPASS_ENABLED must be off before going live");

        UserAccount account = userAccountRepository
                .findByProviderAndProviderUserId(OAuthProvider.DEV_BYPASS, DEV_ADMIN_PROVIDER_USER_ID)
                .orElseGet(() -> {
                    UserAccount created = new UserAccount(
                            OAuthProvider.DEV_BYPASS, DEV_ADMIN_PROVIDER_USER_ID, DEV_ADMIN_NICKNAME);
                    created.promoteToAdmin();
                    return created;
                });
        account.recordLogin(DEV_ADMIN_NICKNAME);
        account = userAccountRepository.save(account);

        String token = jwtService.issueToken(account.getId());
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "#/auth/callback?token=" + encodedToken);
    }

    public record MeResponse(String provider, String nickname) {
    }
}
