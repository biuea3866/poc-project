-- Settlement
CREATE TABLE settlement (
    id               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '정산 ID',
    seller_id        BIGINT          NOT NULL                COMMENT '셀러 ID',
    period_from      DATETIME(6)     NOT NULL                COMMENT '정산 시작일',
    period_to        DATETIME(6)     NOT NULL                COMMENT '정산 종료일',
    total_sales      DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '총 매출액',
    total_commission DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '총 수수료',
    total_refund     DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '총 환불액',
    net_amount       DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '정산 금액',
    status           VARCHAR(30)     NOT NULL                 COMMENT '정산 상태',
    created_at       DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at       DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_settlement_seller (seller_id),
    INDEX idx_settlement_period (period_from, period_to),
    INDEX idx_settlement_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='정산';

-- Settlement Item
CREATE TABLE settlement_item (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '정산 항목 ID',
    settlement_id     BIGINT          NOT NULL                COMMENT '정산 ID',
    order_id          BIGINT          NOT NULL                COMMENT '주문 ID',
    order_item_id     BIGINT          NOT NULL                COMMENT '주문 항목 ID',
    sale_amount       DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '매출 금액',
    commission_rate   DECIMAL(5,4)    NOT NULL                 COMMENT '수수료율',
    commission_amount DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '수수료 금액',
    created_at        DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_settlementitem_settlement (settlement_id),
    INDEX idx_settlementitem_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='정산 항목';

-- Commission Rate
CREATE TABLE commission_rate (
    id             BIGINT          NOT NULL AUTO_INCREMENT COMMENT '수수료율 ID',
    category_id    BIGINT          NOT NULL                COMMENT '카테고리 ID',
    rate           DECIMAL(5,4)    NOT NULL                 COMMENT '수수료율 (0.10~0.30)',
    effective_from DATETIME(6)     NOT NULL                 COMMENT '적용 시작일',
    created_at     DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_commissionrate_category (category_id),
    INDEX idx_commissionrate_effective (category_id, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='수수료율';
