-- Time Sale Order (타임세일 주문)
CREATE TABLE time_sale_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '타임세일 주문 ID',
    time_sale_id    BIGINT          NOT NULL                 COMMENT '타임세일 ID',
    order_id        BIGINT          NOT NULL                 COMMENT '주문 ID',
    member_id       BIGINT          NOT NULL                 COMMENT '회원 ID',
    quantity        INT             NOT NULL                 COMMENT '구매 수량',
    purchased_at    DATETIME(6)     NOT NULL                 COMMENT '구매일시',
    created_at      DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_time_sale_order_time_sale (time_sale_id),
    INDEX idx_time_sale_order_order (order_id),
    INDEX idx_time_sale_order_member (member_id),
    INDEX idx_time_sale_order_purchased (purchased_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='타임세일 주문';
