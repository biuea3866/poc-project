-- Inventory Item (SKU별 재고)
CREATE TABLE inventory_item (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '재고 고유 식별자',
    product_option_id   BIGINT          NOT NULL                COMMENT '상품 옵션 ID (SKU)',
    total_quantity      INT             NOT NULL DEFAULT 0      COMMENT '실물 재고 수량',
    available_quantity  INT             NOT NULL DEFAULT 0      COMMENT '가용 재고 수량 (total - reserved)',
    reserved_quantity   INT             NOT NULL DEFAULT 0      COMMENT '예약 재고 수량',
    safety_threshold    INT             NOT NULL DEFAULT 10     COMMENT '안전재고 임계값',
    version             BIGINT          NOT NULL DEFAULT 0      COMMENT '낙관적 락 버전',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_option_id (product_option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SKU별 재고 정보';

-- Inventory Transaction (재고 변동 이력)
CREATE TABLE inventory_transaction (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '재고 트랜잭션 고유 식별자',
    inventory_item_id   BIGINT          NOT NULL                COMMENT '재고 항목 ID',
    type                VARCHAR(20)     NOT NULL                COMMENT '변경 유형: INBOUND, OUTBOUND, RESERVE, RELEASE, ADJUST',
    quantity            INT             NOT NULL                COMMENT '변경 수량',
    reason              VARCHAR(500)    NULL                    COMMENT '변경 사유',
    reference_id        VARCHAR(100)    NULL                    COMMENT '참조 ID (주문번호 등)',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_transaction_item (inventory_item_id),
    INDEX idx_transaction_reference (reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='재고 변동 이력';
