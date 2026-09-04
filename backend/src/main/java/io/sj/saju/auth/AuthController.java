package io.sj.saju.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final TokenRevocationService tokenRevocationService;
    private final String frontendUrl;
    private final boolean devAdminBypassEnabled;
    private final String devAdminBypassSecret;

    public AuthController(
            UserAccountRepository userAccountRepository,
            JwtService jwtService,
            TokenRevocationService tokenRevocationService,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.dev-admin-bypass.enabled:false}") boolean devAdminBypassEnabled,
            @Value("${app.dev-admin-bypass.secret:}") String devAdminBypassSecret) {
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
        this.frontendUrl = frontendUrl;
        this.devAdminBypassEnabled = devAdminBypassEnabled;
        this.devAdminBypassSecret = devAdminBypassSecret;
    }

    /**
     * 이 토큰만 즉시 무효화한다. JwtAuthenticationFilter가 이미 서명/만료를
     * 검증해 인증을 통과시켰으므로, 여기서는 그 토큰의 jti를 꺼내 revoked_token에
     * 남기기만 하면 된다 — jti가 없는 구버전 토큰이면 무효화할 게 없으니
     * 조용히 넘어간다(로그아웃 자체는 항상 성공한 것처럼 응답).
     */
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            JwtService.TokenClaims claims = jwtService.parseClaims(header.substring("Bearer ".length()));
            if (claims != null) {
                tokenRevocationService.revoke(claims);
            }
        }
        return ResponseEntity.noContent().build();
    }

    /** For the frontend to check "am I logged in" and show a nickname/avatar — nothing else. */
    @GetMapping("/api/auth/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UUID userAccountId) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        return userAccountRepository.findById(userAccountId)
                .map(account -> ResponseEntity.ok(toResponse(account)))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    /** 닉네임/아바타 편집(설정 화면) — 둘 다 매번 같이 보낸다. */
    @PatchMapping("/api/auth/me")
    public ResponseEntity<MeResponse> updateMe(
            @AuthenticationPrincipal UUID userAccountId, @Valid @RequestBody UpdateProfileRequest request) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        AvatarPreset avatarKey;
        try {
            avatarKey = AvatarPreset.valueOf(request.avatarKey());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return userAccountRepository.findById(userAccountId)
                .map(account -> {
                    account.updateProfile(request.nickname().trim(), avatarKey);
                    userAccountRepository.save(account);
                    return ResponseEntity.ok(toResponse(account));
                })
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    private MeResponse toResponse(UserAccount account) {
        return new MeResponse(
                account.getProvider().name(), account.getNickname(), account.getAvatarKey().name(),
                account.isAdmin());
    }

    /**
     * 실제 OAuth 앱 등록 전에 팀이 관리자 권한으로 써볼 수 있게 하는 임시
     * 우회 로그인. app.dev-admin-bypass.enabled(ADMIN_BYPASS_ENABLED)가
     * true이고, 쿼리파라미터 key가 app.dev-admin-bypass.secret(ADMIN_BYPASS_SECRET)과
     * 일치할 때만 동작한다 — permitAll 경로라 URL만 알면 누구나 호출할 수
     * 있어서, 비밀값 없이는 절대 열리지 않게 이중으로 막는다(플래그만으로는
     * URL을 아는 사람 누구나 관리자가 될 수 있었다). 시크릿을 아예 설정 안
     * 했으면(빈 문자열) 플래그를 켜도 항상 막힌다 — 잠금을 깜빡하는 실수보다
     * 안전한 쪽으로 fail closed. 상용화 시점엔 반드시 꺼야 한다. 인증 없이
     * 관리자 계정을 내주는 기능이라 매 사용을 로그로 남긴다.
     */
    @GetMapping("/api/auth/dev-admin-login")
    public void devAdminLogin(
            @RequestParam(required = false) String key, HttpServletResponse response) throws IOException {
        if (!devAdminBypassEnabled || !secretMatches(key)) {
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
        account.recordLogin();
        account = userAccountRepository.save(account);

        String token = jwtService.issueToken(account.getId());
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "#/auth/callback?token=" + encodedToken);
    }

    // 타이밍 공격으로 시크릿을 한 글자씩 추측하지 못하게 상수 시간 비교를
    // 쓴다. 시크릿을 아예 설정 안 했으면(빈 문자열) 무조건 실패시킨다 —
    // key도 안 보냈을 때 "빈 문자열 == 빈 문자열"로 통과해 버리는 것을 막는다.
    private boolean secretMatches(String key) {
        if (devAdminBypassSecret.isEmpty()) {
            return false;
        }
        String provided = key == null ? "" : key;
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                devAdminBypassSecret.getBytes(StandardCharsets.UTF_8));
    }

    public record MeResponse(String provider, String nickname, String avatarKey, boolean isAdmin) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 1, max = 20) String nickname,
            @NotBlank String avatarKey) {
    }
}
