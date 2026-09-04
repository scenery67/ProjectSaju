-- 결제 승인(PG 확인) 실패 사유를 남긴다 — refund_reason과 같은 이유로,
-- "왜 이 결제가 실패했는지" 감사/디버깅 목적.
ALTER TABLE payment ADD COLUMN fail_reason TEXT;
