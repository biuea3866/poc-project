-- 부분 환불 지원 (PD-12)
ALTER TABLE payment
    ADD COLUMN refund_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '환불 금액' AFTER final_amount;
