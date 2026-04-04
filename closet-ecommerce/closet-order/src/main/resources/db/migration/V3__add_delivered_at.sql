-- 자동 구매확정 배치를 위한 배송완료 일시 추가 (PD-16, PD-17)
ALTER TABLE orders
    ADD COLUMN delivered_at DATETIME(6) NULL COMMENT '배송 완료 일시' AFTER ordered_at;

CREATE INDEX idx_orders_delivered_status ON orders (status, delivered_at);
