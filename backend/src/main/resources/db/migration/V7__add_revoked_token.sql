-- JWT는 원래 무상태(stateless)라 서버가 강제로 무효화할 방법이 없어서,
-- 로그아웃해도 탈취된 토큰이 만료(30일)까지 계속 유효했다. 로그아웃 시점의
-- 토큰 id(jti)만 최소한으로 기록해서, 그 토큰만 즉시 거부할 수 있게 한다.
-- expires_at은 원래 토큰의 만료 시각 그대로 저장 — 그 시각이 지나면 어차피
-- 토큰 자체가 만료라 이 행도 의미가 없어진다(정리는 필요해지면 별도로 추가).
CREATE TABLE revoked_token (
    jti        UUID PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_revoked_token_expires_at ON revoked_token(expires_at);
