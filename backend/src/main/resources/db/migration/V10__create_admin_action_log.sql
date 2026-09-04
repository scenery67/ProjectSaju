-- 관리자가 한 조치(권한 변경/탈퇴/환불/크레딧 수동 조정)를 감사 목적으로
-- 남긴다. admin_user_account_id/target_user_account_id 둘 다 그 계정이
-- 나중에 탈퇴돼도 로그 자체는 회계·감사 기록이라 지우지 않고 참조만
-- 끊는다(payment/credit_transaction과 같은 이유, V8 참고) — 특히
-- target_user_account_id는 "탈퇴 처리" 로그 자신이 가리키는 대상이 그
-- 처리로 인해 곧 사라지므로 반드시 이렇게 둬야 한다.
CREATE TABLE admin_action_log (
    id                     UUID PRIMARY KEY,
    admin_user_account_id  UUID REFERENCES user_account(id) ON DELETE SET NULL,
    target_user_account_id UUID REFERENCES user_account(id) ON DELETE SET NULL,
    action_type            VARCHAR(30) NOT NULL
        CHECK (action_type IN ('SET_ADMIN_TRUE', 'SET_ADMIN_FALSE', 'DELETE_USER', 'REFUND_PAYMENT', 'CREDIT_ADJUST')),
    detail                 TEXT,
    created_at             TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_admin_action_log_admin_user_account_id ON admin_action_log(admin_user_account_id);
CREATE INDEX idx_admin_action_log_target_user_account_id ON admin_action_log(target_user_account_id);
CREATE INDEX idx_admin_action_log_created_at ON admin_action_log(created_at);
