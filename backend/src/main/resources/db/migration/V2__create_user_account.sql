-- OAuth 소셜 로그인 계정. 카카오/구글/네이버 중 하나로 로그인한 사용자 하나당
-- 한 행. provider + provider_user_id 조합이 실제 식별자이고, 그 외 개인정보는
-- 닉네임 정도만 최소로 저장한다(이메일/전화번호 등은 수집하지 않음).
CREATE TABLE user_account (
    id                UUID PRIMARY KEY,
    provider          VARCHAR(20) NOT NULL
        CHECK (provider IN ('KAKAO', 'GOOGLE', 'NAVER')),
    provider_user_id  VARCHAR(255) NOT NULL,
    nickname          VARCHAR(100),
    created_at        TIMESTAMPTZ NOT NULL,
    last_login_at     TIMESTAMPTZ NOT NULL,
    UNIQUE (provider, provider_user_id)
);
