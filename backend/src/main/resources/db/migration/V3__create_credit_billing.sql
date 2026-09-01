-- LLM 사주 상담 질문 횟수 기반 과금(고정 크레딧 패키지) 스키마.
-- 잔액(user_account.credit_balance)은 빠른 조회용 캐시고, 실제 근거는
-- credit_transaction 원장이다 — 잔액만 두면 "왜 이렇게 줄었는지" 감사가
-- 안 되므로 모든 증감을 이벤트로 남긴다.

ALTER TABLE user_account
    ADD COLUMN credit_balance INT NOT NULL DEFAULT 0,
    -- 결제 내역 조회, 환불/크레딧 수동 지급 같은 관리자 조치를 할 수 있는지
    -- 여부. 지금은 관리자 화면이 없어 DB에서 직접 true로 바꿔야 한다.
    ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT false;

-- 판매 단위. 가격을 코드가 아니라 데이터로 둬서 배포 없이 조정할 수 있게 한다.
CREATE TABLE credit_package (
    id             UUID PRIMARY KEY,
    name           VARCHAR(50) NOT NULL,
    credit_amount  INT NOT NULL CHECK (credit_amount > 0),
    price_krw      INT NOT NULL CHECK (price_krw > 0),
    is_active      BOOLEAN NOT NULL DEFAULT true,
    sort_order     INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL
);

-- 기준 단가 290원/질문에서 패키지가 커질수록 실질 단가가 낮아지는 물량 할인
-- 구조(경쟁 서비스 요금표를 참고해 비율만 유지, 실제 출시 전 재검토 필요).
-- "보너스 캐럿" 같은 표시는 프론트에서 credit_amount와 기준단가로 계산해서
-- 보여주면 되므로 스키마에 별도 컬럼을 두지 않았다.
INSERT INTO credit_package (id, name, credit_amount, price_krw, sort_order, created_at) VALUES
    (gen_random_uuid(), '10회 질문',  10,  2900,  1, now()), -- 할인 없음(기준 단가)
    (gen_random_uuid(), '20회 질문',  20,  4900,  2, now()), -- 약 16% 할인
    (gen_random_uuid(), '50회 질문',  50,  9900,  3, now()), -- 약 32% 할인
    (gen_random_uuid(), '110회 질문', 110, 19900, 4, now()); -- 약 38% 할인

-- PG 연동 전이라 provider/외부 거래ID는 아직 비워둘 수 있게 nullable로 둔다.
-- refunded_by는 처리한 관리자를 남겨서 "누가 이 환불을 승인했는지" 추적 가능하게 한다.
CREATE TABLE payment (
    id                 UUID PRIMARY KEY,
    user_account_id    UUID NOT NULL REFERENCES user_account(id),
    credit_package_id  UUID NOT NULL REFERENCES credit_package(id),
    pg_provider        VARCHAR(30),
    pg_transaction_id  VARCHAR(255),
    amount_krw         INT NOT NULL,
    credit_amount      INT NOT NULL,
    status             VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    created_at         TIMESTAMPTZ NOT NULL,
    completed_at       TIMESTAMPTZ,
    refunded_at        TIMESTAMPTZ,
    refunded_by        UUID REFERENCES user_account(id),
    refund_reason      VARCHAR(255)
);

-- 크레딧 증감 원장. reference_id는 payment.id 또는(향후) LLM 질문 로그의 id를
-- 가리킬 수 있는데, 가리키는 테이블이 상황마다 달라 강한 FK 대신 느슨하게
-- UUID만 남긴다(다형 참조).
CREATE TABLE credit_transaction (
    id               UUID PRIMARY KEY,
    user_account_id  UUID NOT NULL REFERENCES user_account(id),
    type             VARCHAR(20) NOT NULL
        CHECK (type IN ('FREE_GRANT', 'PURCHASE', 'CONSUME', 'REFUND', 'ADMIN_ADJUST')),
    amount           INT NOT NULL,
    balance_after    INT NOT NULL,
    reference_id     UUID,
    note             VARCHAR(255),
    created_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_credit_transaction_user_account_id ON credit_transaction(user_account_id);
CREATE INDEX idx_payment_user_account_id ON payment(user_account_id);
CREATE INDEX idx_payment_status ON payment(status);

-- 아직 안 만든 것: LLM 상담(채팅) 로그 테이블. 관리자가 "결제 후 상담이 어떻게
-- 됐는지" 봐야 한다는 요구사항(2026-09-01)이 있어서 나중에 질문/답변 내용을
-- 저장하는 테이블이 필요하다 — 다만 실제 LLM 상담 기능 자체를 아직 설계하지
-- 않아서 세션/메시지 구조를 여기서 미리 확정하지 않는다. credit_transaction.
-- reference_id가 느슨한 UUID 참조라 그 테이블이 생겨도 이 스키마는 안 바뀐다.
