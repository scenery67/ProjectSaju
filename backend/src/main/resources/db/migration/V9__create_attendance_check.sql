-- 출석 체크(일일 로그인 보상) 기록. 하루 최대 1회만 체크할 수 있도록
-- (user_account_id, checked_date) 유니크 제약으로 DB 레벨에서도 중복을
-- 막는다. streak_count는 그날 체크 시점의 연속 출석 일수(끊기면 1로
-- 리셋)를 저장해서, 다음 체크 때 "어제도 체크했는지"만 보면 스트릭을
-- 이어갈 수 있다 — 매번 전체 이력을 훑을 필요가 없다.
CREATE TABLE attendance_check (
    id              UUID PRIMARY KEY,
    user_account_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    checked_date    DATE NOT NULL,
    streak_count    INT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    UNIQUE (user_account_id, checked_date)
);

CREATE INDEX idx_attendance_check_user_account_id ON attendance_check(user_account_id);
