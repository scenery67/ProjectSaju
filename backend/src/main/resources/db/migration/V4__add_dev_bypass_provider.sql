-- 상용화 전, 실제 OAuth 앱 등록 없이 팀 내부에서 관리자 권한으로 써볼 수
-- 있게 하는 임시 우회 로그인(ADMIN_BYPASS_ENABLED=true일 때만 동작,
-- AuthController#devAdminLogin)이 만드는 고정 계정을 위해 provider 값을
-- 하나 추가한다. 실제 카카오/구글/네이버 계정과 섞이지 않도록 구분한다.
ALTER TABLE user_account DROP CONSTRAINT user_account_provider_check;
ALTER TABLE user_account ADD CONSTRAINT user_account_provider_check
    CHECK (provider::text = ANY (ARRAY['KAKAO', 'GOOGLE', 'NAVER', 'DEV_BYPASS']::text[]));
