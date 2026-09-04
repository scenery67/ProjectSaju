-- 관리자 공지 발송(ANNOUNCEMENT)도 admin_action_log에 남기기 위해 CHECK
-- 제약을 넓힌다. target_user_account_id는 이미 nullable이라 그대로 둔다 —
-- 공지는 특정 한 명이 아니라 전체 발송이라 대상이 없다.
ALTER TABLE admin_action_log DROP CONSTRAINT admin_action_log_action_type_check;
ALTER TABLE admin_action_log ADD CONSTRAINT admin_action_log_action_type_check
    CHECK (action_type IN
        ('SET_ADMIN_TRUE', 'SET_ADMIN_FALSE', 'DELETE_USER', 'REFUND_PAYMENT', 'CREDIT_ADJUST', 'ANNOUNCEMENT'));
