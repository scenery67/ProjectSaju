-- LLM 사주 상담 세션/메시지. 관리자가 "결제 후 상담이 어떻게 됐는지 봐야
-- 한다"는 요구사항(2026-09-01)이 있어서 대화 내용을 실제로 저장한다 —
-- 최소 수집 원칙(CLAUDE.md 3.2)보다 이 감사 요구사항을 우선한 의도적 결정.
-- 보관 정책은 reading_record와 동일(2026-09-02 결정 연장): 계정 삭제 시
-- ON DELETE CASCADE로 함께 삭제, 별도 보관기간은 두지 않는다.
CREATE TABLE consultation_session (
    id                UUID PRIMARY KEY,
    user_account_id   UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    -- 상담의 근거가 된 사주 결과. 그 기록이 지워져도 상담 로그 자체는
    -- 감사 목적으로 남겨야 하므로 CASCADE가 아니라 SET NULL.
    reading_record_id UUID REFERENCES reading_record(id) ON DELETE SET NULL,
    persona_type      VARCHAR(30) NOT NULL
        CHECK (persona_type IN ('BREAKUP', 'COUPLE_COMPATIBILITY')),
    created_at        TIMESTAMPTZ NOT NULL
);

CREATE TABLE consultation_message (
    id           UUID PRIMARY KEY,
    session_id   UUID NOT NULL REFERENCES consultation_session(id) ON DELETE CASCADE,
    role         VARCHAR(10) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content      TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_consultation_session_user_account_id ON consultation_session(user_account_id);
CREATE INDEX idx_consultation_message_session_id ON consultation_message(session_id);
