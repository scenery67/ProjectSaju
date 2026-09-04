package io.sj.saju.auth;

import java.util.Map;

/**
 * Normalizes the very different attribute shapes each provider returns into
 * just providerUserId. Kakao/Naver aren't full OIDC providers so Spring
 * Security can't parse them generically — see 참고 comments below.
 *
 * 공급자가 주는 닉네임(카카오는 종종 실명)은 의도적으로 읽지 않는다 —
 * CLAUDE.md 3.2에 따라 실명이 노출될 수 있는 값을 아예 저장하지 않기
 * 위해서다. 표시 닉네임은 항상 RandomProfileGenerator가 만든다.
 */
final class OAuthUserInfo {

    private final String providerUserId;

    private OAuthUserInfo(String providerUserId) {
        this.providerUserId = providerUserId;
    }

    String providerUserId() {
        return providerUserId;
    }

    static OAuthUserInfo of(OAuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new OAuthUserInfo(String.valueOf(attributes.get("sub")));
            // 카카오: id는 최상위. https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info
            case KAKAO -> new OAuthUserInfo(String.valueOf(attributes.get("id")));
            // 네이버: 실제 필드가 응답의 "response" 하위에 한 번 더 감싸져 있다.
            // application.yml에서 user-name-attribute: response로 지정해뒀다.
            // https://developers.naver.com/docs/login/profile/profile.md
            case NAVER -> {
                Map<String, Object> response = asMap(attributes.get("response"));
                yield new OAuthUserInfo(String.valueOf(response.get("id")));
            }
            // DEV_BYPASS는 실제 OAuth2 등록 공급자가 아니라 이 경로(Spring
            // Security의 oauth2Login 콜백)로는 절대 들어오지 않는다 —
            // AuthController#devAdminLogin이 별도로 계정을 만든다.
            case DEV_BYPASS -> throw new IllegalStateException(
                    "DEV_BYPASS is not a real OAuth2 provider and should never reach this path");
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }
}
