package io.sj.saju.auth;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// TEMPORARY — remove once the KAKAO_CLIENT_ID env-var-not-taking-effect
// mystery (2026-09-02) is resolved. Logs only whether each client-id
// resolved to the "placeholder" default or something else, plus its length —
// never the actual value.
@Component
public class OAuthConfigDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(OAuthConfigDiagnostics.class);

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @PostConstruct
    void logDiagnostics() {
        log.info("[oauth-diag] kakao.client-id={} (len={})",
                "placeholder".equals(kakaoClientId) ? "STILL_PLACEHOLDER" : "REAL_VALUE", kakaoClientId.length());
        log.info("[oauth-diag] kakao.client-secret={} (len={})",
                "placeholder".equals(kakaoClientSecret) ? "STILL_PLACEHOLDER" : "REAL_VALUE", kakaoClientSecret.length());
    }
}
