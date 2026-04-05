-- Exchange Request (교환 요청, CP-28)
CREATE TABLE IF NOT EXISTS exchange_request (
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '교환 요청 PK',
    order_id                    BIGINT          NOT NULL                COMMENT '주문 ID',
    order_item_id               BIGINT          NOT NULL                COMMENT '주문 항목 ID',
    member_id                   BIGINT          NOT NULL                COMMENT '회원 ID',
    seller_id                   BIGINT          NOT NULL                COMMENT '셀러 ID',
    original_product_option_id  BIGINT          NOT NULL                COMMENT '기존 상품 옵션 ID',
    new_product_option_id       BIGINT          NOT NULL                COMMENT '새 상품 옵션 ID',
    quantity                    INT             NOT NULL                COMMENT '교환 수량',
    reason                      VARCHAR(30)     NOT NULL                COMMENT '교환 사유 (DEFECTIVE, WRONG_ITEM, SIZE_MISMATCH, CHANGE_OF_MIND)',
    reason_detail               VARCHAR(500)    NULL                    COMMENT '교환 상세 사유',
    status                      VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED' COMMENT '교환 상태 (REQUESTED, PICKUP_SCHEDULED, PICKUP_COMPLETED, RESHIPPING, COMPLETED, REJECTED)',
    shipping_fee                DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '교환 배송비 (왕복)',
    shipping_fee_payer          VARCHAR(20)     NOT NULL                COMMENT '배송비 부담 주체 (BUYER, SELLER)',
    pickup_tracking_number      VARCHAR(30)     NULL                    COMMENT '수거 송장 번호',
    new_tracking_number         VARCHAR(30)     NULL                    COMMENT '재배송 송장 번호',
    completed_at                DATETIME(6)     NULL                    COMMENT '완료 일시',
    created_at                  DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at                  DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_exchange_order (order_id),
    INDEX idx_exchange_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='교환 요청';
