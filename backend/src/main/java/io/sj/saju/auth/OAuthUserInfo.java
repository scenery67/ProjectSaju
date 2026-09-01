package io.sj.saju.auth;

import java.util.Map;

/**
 * Normalizes the very different attribute shapes each provider returns into
 * (providerUserId, nickname). Kakao/Naver aren't full OIDC providers so
 * Spring Security can't parse them generically — see 참고 comments below.
 */
final class OAuthUserInfo {

    private final String providerUserId;
    private final String nickname;

    private OAuthUserInfo(String providerUserId, String nickname) {
        this.providerUserId = providerUserId;
        this.nickname = nickname;
    }

    String providerUserId() {
        return providerUserId;
    }

    String nickname() {
        return nickname;
    }

    static OAuthUserInfo of(OAuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new OAuthUserInfo(
                    String.valueOf(attributes.get("sub")),
                    String.valueOf(attributes.getOrDefault("name", "")));
            // 카카오: id는 최상위, 닉네임은 kakao_account.profile.nickname.
            // https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info
            case KAKAO -> {
                Map<String, Object> kakaoAccount = asMap(attributes.get("kakao_account"));
                Map<String, Object> profile = asMap(kakaoAccount.get("profile"));
                yield new OAuthUserInfo(
                        String.valueOf(attributes.get("id")),
                        String.valueOf(profile.getOrDefault("nickname", "")));
            }
            // 네이버: 실제 필드가 응답의 "response" 하위에 한 번 더 감싸져 있다.
            // application.yml에서 user-name-attribute: response로 지정해뒀다.
            // https://developers.naver.com/docs/login/profile/profile.md
            case NAVER -> {
                Map<String, Object> response = asMap(attributes.get("response"));
                yield new OAuthUserInfo(
                        String.valueOf(response.get("id")),
                        String.valueOf(response.getOrDefault("nickname", "")));
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }
}
