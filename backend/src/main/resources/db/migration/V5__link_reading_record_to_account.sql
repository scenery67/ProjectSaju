-- 로그인 계정과 사주 기록을 연동하기 위한 컬럼. 보관 정책(2026-09-02 결정,
-- CLAUDE.md 3.2 — 정책 없이는 식별자를 추가하지 않는다는 규칙에 따른 것):
-- - 비로그인 요청은 계속 식별자 없이 저장한다(기존과 동일).
-- - 로그인한 사용자의 기록만 user_account_id로 연결하고, 계정이 삭제되면
--   ON DELETE CASCADE로 기록도 함께 삭제한다 — 별도 보관기간 없이 "계정
--   삭제 = 기록 삭제"를 보관기간 정책으로 삼는다.
-- - result_json은 "내 사주" 화면에서 결과를 다시 열어볼 수 있게 전체 결과
--   (SajuReadingResult 직렬화)를 저장한다 — 로그인 사용자 기록에만 채워지고,
--   비로그인 기록은 계속 null(기존 동작 그대로).
ALTER TABLE reading_record
    ADD COLUMN user_account_id UUID REFERENCES user_account(id) ON DELETE CASCADE,
    ADD COLUMN result_json TEXT;

CREATE INDEX idx_reading_record_user_account_id ON reading_record(user_account_id);
