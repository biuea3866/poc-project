CREATE TABLE IF NOT EXISTS payment (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '결제 PK',
    order_id    BIGINT       NOT NULL                COMMENT '주문 ID',
    payment_key VARCHAR(200) NULL                    COMMENT 'PG 결제 키',
    method      VARCHAR(30)  NULL                    COMMENT '결제 수단',
    final_amount DECIMAL(15,2) NOT NULL              COMMENT '최종 결제 금액',
    status      VARCHAR(30)  NOT NULL                COMMENT '결제 상태',
    created_at  DATETIME(6)  NOT NULL                COMMENT '생성일시',
    updated_at  DATETIME(6)  NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_payment_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='결제';
