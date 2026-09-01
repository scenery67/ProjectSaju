package io.sj.saju.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * After a successful OAuth2 login, issue our own JWT and hand it to the
 * frontend via a redirect — there's no shared cookie domain between the
 * GitHub Pages frontend and the Fly.io backend, so the token has to travel
 * in the URL of a real browser redirect, not a fetch response.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            JwtService jwtService,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        UUID userAccountId = UUID.fromString((String) oauth2User.getAttributes().get("sajuUserAccountId"));
        String token = jwtService.issueToken(userAccountId);

        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "#/auth/callback?token=" + encodedToken);
    }
}
