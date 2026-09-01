package io.sj.saju.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OAuthUserInfoTest {

    @Test
    void extractsGoogleAttributesByOidcStandardClaims() {
        Map<String, Object> attributes = Map.of(
                "sub", "1234567890",
                "name", "홍길동",
                "email", "unused@example.com");

        OAuthUserInfo info = OAuthUserInfo.of(OAuthProvider.GOOGLE, attributes);

        assertThat(info.providerUserId()).isEqualTo("1234567890");
        assertThat(info.nickname()).isEqualTo("홍길동");
    }

    @Test
    void extractsKakaoAttributesFromNestedKakaoAccountProfile() {
        // https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info
        Map<String, Object> attributes = Map.of(
                "id", 987654321L,
                "kakao_account", Map.of(
                        "profile", Map.of("nickname", "다숨이")));

        OAuthUserInfo info = OAuthUserInfo.of(OAuthProvider.KAKAO, attributes);

        assertThat(info.providerUserId()).isEqualTo("987654321");
        assertThat(info.nickname()).isEqualTo("다숨이");
    }

    @Test
    void extractsNaverAttributesFromNestedResponseObject() {
        // https://developers.naver.com/docs/login/profile/profile.md
        Map<String, Object> attributes = Map.of(
                "resultcode", "00",
                "response", Map.of("id", "naver-abc123", "nickname", "설레이"));

        OAuthUserInfo info = OAuthUserInfo.of(OAuthProvider.NAVER, attributes);

        assertThat(info.providerUserId()).isEqualTo("naver-abc123");
        assertThat(info.nickname()).isEqualTo("설레이");
    }

    @Test
    void missingNestedStructureDoesNotThrow() {
        OAuthUserInfo info = OAuthUserInfo.of(OAuthProvider.KAKAO, Map.of("id", 1L));

        assertThat(info.providerUserId()).isEqualTo("1");
        assertThat(info.nickname()).isEqualTo("");
    }
}
