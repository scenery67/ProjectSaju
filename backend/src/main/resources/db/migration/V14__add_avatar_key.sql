-- 프로필 아바타(고정 이모지 프리셋 중 선택). 기존 계정은 특별한 의미 없는
-- 기본값(FOX)으로 채우고, 신규 가입부터는 항상 랜덤으로 배정된다.
ALTER TABLE user_account ADD COLUMN avatar_key VARCHAR(20) NOT NULL DEFAULT 'FOX';
