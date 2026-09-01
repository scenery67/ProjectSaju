-- 사주 풀이 요청/결과 기록. io.sj.saju.reading.ReadingRecord와 1:1로 대응한다.
-- Persisted history of a single reading request/result pair.
CREATE TABLE reading_record (
    id            UUID PRIMARY KEY,
    persona_type  VARCHAR(255) NOT NULL
        CHECK (persona_type IN ('BREAKUP', 'COUPLE_COMPATIBILITY')),
    self_name     VARCHAR(255) NOT NULL,
    partner_name  VARCHAR(255),
    summary       VARCHAR(255) NOT NULL,
    detail        TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);
