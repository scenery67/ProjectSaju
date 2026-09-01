package io.sj.saju.auth;

public enum OAuthProvider {
    KAKAO,
    GOOGLE,
    NAVER;

    /** Spring Security's registrationId (as configured in application.yml) is lowercase. */
    public static OAuthProvider fromRegistrationId(String registrationId) {
        return valueOf(registrationId.toUpperCase());
    }
}
