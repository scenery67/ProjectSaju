-- 알림함(결제/충전 완료, 출석 보너스, 관리자 공지). 대상별로 한 행씩 넣는다 —
-- 관리자 공지도 발송 시점에 전체 사용자 수만큼 insert한다. 사용자 수가 아직
-- 적어(수십~수백 명) 별도 broadcast/구독 테이블 없이 이 구조로 충분하다.
CREATE TABLE notification (
    id               UUID PRIMARY KEY,
    user_account_id  UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    type             VARCHAR(30) NOT NULL
        CHECK (type IN ('PAYMENT_COMPLETED', 'ATTENDANCE_BONUS', 'ADMIN_ANNOUNCEMENT')),
    title            VARCHAR(200) NOT NULL,
    body             VARCHAR(500) NOT NULL,
    credit_amount    INT,
    is_read          BOOLEAN NOT NULL DEFAULT false,
    created_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_notification_user_account_id_created_at
    ON notification (user_account_id, created_at DESC);
