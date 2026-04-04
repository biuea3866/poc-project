-- Inventory (3단 재고 구조: total/available/reserved)
CREATE TABLE inventory (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '재고 ID',
    product_id          BIGINT          NOT NULL                 COMMENT '상품 ID',
    product_option_id   BIGINT          NOT NULL                 COMMENT '상품 옵션 ID',
    sku                 VARCHAR(50)     NOT NULL                 COMMENT 'SKU 코드',
    total_quantity      INT             NOT NULL DEFAULT 0       COMMENT '전체 수량',
    available_quantity  INT             NOT NULL DEFAULT 0       COMMENT '주문 가능 수량',
    reserved_quantity   INT             NOT NULL DEFAULT 0       COMMENT '예약된 수량',
    safety_threshold    INT             NOT NULL DEFAULT 10      COMMENT '안전 재고 기준',
    version             BIGINT          NOT NULL DEFAULT 0       COMMENT '낙관적 락 버전',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at          DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    deleted_at          DATETIME(6)     NULL                     COMMENT '삭제일시',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_product_option (product_id, product_option_id),
    UNIQUE INDEX idx_inventory_sku (sku),
    INDEX idx_inventory_available (available_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='재고';

-- Inventory History (재고 변동 이력)
CREATE TABLE inventory_history (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '이력 ID',
    inventory_id        BIGINT          NOT NULL                 COMMENT '재고 ID',
    change_type         VARCHAR(30)     NOT NULL                 COMMENT '변동 유형 (RESERVE/DEDUCT/RELEASE/INBOUND/RETURN_RESTORE/ADJUSTMENT)',
    quantity            INT             NOT NULL                 COMMENT '변동 수량',
    before_total        INT             NOT NULL                 COMMENT '변동 전 전체 수량',
    after_total         INT             NOT NULL                 COMMENT '변동 후 전체 수량',
    before_available    INT             NOT NULL                 COMMENT '변동 전 가용 수량',
    after_available     INT             NOT NULL                 COMMENT '변동 후 가용 수량',
    before_reserved     INT             NOT NULL                 COMMENT '변동 전 예약 수량',
    after_reserved      INT             NOT NULL                 COMMENT '변동 후 예약 수량',
    reference_id        VARCHAR(100)    NULL                     COMMENT '참조 ID (주문번호 등)',
    reference_type      VARCHAR(50)     NULL                     COMMENT '참조 유형 (ORDER/RETURN 등)',
    reason              VARCHAR(500)    NULL                     COMMENT '사유',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_history_inventory (inventory_id),
    INDEX idx_history_reference (reference_id, reference_type),
    INDEX idx_history_change_type (change_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='재고 변동 이력';
