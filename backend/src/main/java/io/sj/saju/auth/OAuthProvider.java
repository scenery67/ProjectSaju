package io.sj.saju.auth;

public enum OAuthProvider {
    KAKAO,
    GOOGLE,
    NAVER,
    // 실제 OAuth 공급자가 아니다 — 상용화 전 팀 내부 테스트용 관리자 우회
    // 로그인(AuthController#devAdminLogin, ADMIN_BYPASS_ENABLED=true일 때만
    // 동작)에서 생성하는 고정 계정을 구분하기 위한 값.
    DEV_BYPASS;

    /** Spring Security's registrationId (as configured in application.yml) is lowercase. */
    public static OAuthProvider fromRegistrationId(String registrationId) {
        return valueOf(registrationId.toUpperCase());
    }
}
