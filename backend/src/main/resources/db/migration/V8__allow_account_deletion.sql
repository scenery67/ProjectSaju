-- 관리자 화면에 "유저 탈퇴" 기능을 추가하면서 필요해진 변경.
-- 개인 콘텐츠(reading_record, consultation_*)는 기존 정책대로 계정 삭제 시
-- CASCADE로 함께 지운다. 반면 결제(payment)·크레딧 원장(credit_transaction)은
-- 회계 감사 기록이라 계정이 없어져도 남겨야 한다 — NOT NULL + 캐스케이드
-- 없는 지금 FK로는 참조가 남아있어 계정 삭제 자체가 막히므로, 소유자
-- 컬럼만 NULL로 만들고(ON DELETE SET NULL) 행 자체는 보존한다.
ALTER TABLE payment ALTER COLUMN user_account_id DROP NOT NULL;
ALTER TABLE payment DROP CONSTRAINT payment_user_account_id_fkey;
ALTER TABLE payment ADD CONSTRAINT payment_user_account_id_fkey
    FOREIGN KEY (user_account_id) REFERENCES user_account(id) ON DELETE SET NULL;

-- refunded_by(환불 처리한 관리자)도 마찬가지 — 그 관리자 계정이 나중에
-- 지워져도 "누가 환불했는지" 기록 자체는 남기되, 참조만 끊는다.
ALTER TABLE payment DROP CONSTRAINT payment_refunded_by_fkey;
ALTER TABLE payment ADD CONSTRAINT payment_refunded_by_fkey
    FOREIGN KEY (refunded_by) REFERENCES user_account(id) ON DELETE SET NULL;

ALTER TABLE credit_transaction ALTER COLUMN user_account_id DROP NOT NULL;
ALTER TABLE credit_transaction DROP CONSTRAINT credit_transaction_user_account_id_fkey;
ALTER TABLE credit_transaction ADD CONSTRAINT credit_transaction_user_account_id_fkey
    FOREIGN KEY (user_account_id) REFERENCES user_account(id) ON DELETE SET NULL;
