-- Shipment (배송 정보)
CREATE TABLE IF NOT EXISTS shipment (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '배송 PK',
    order_id          BIGINT          NOT NULL                COMMENT '주문 ID',
    seller_id         BIGINT          NOT NULL                COMMENT '셀러 ID',
    member_id         BIGINT          NOT NULL                COMMENT '회원 ID',
    carrier           VARCHAR(20)     NULL                    COMMENT '택배사 코드 (CJ, LOGEN, LOTTE, EPOST)',
    tracking_number   VARCHAR(30)     NULL                    COMMENT '송장 번호',
    status            VARCHAR(20)     NOT NULL DEFAULT 'READY' COMMENT '배송 상태 (READY, IN_TRANSIT, DELIVERED)',
    receiver_name     VARCHAR(50)     NOT NULL                COMMENT '수령인 이름',
    receiver_phone    VARCHAR(20)     NOT NULL                COMMENT '수령인 전화번호',
    zip_code          VARCHAR(10)     NOT NULL                COMMENT '우편번호',
    address           VARCHAR(200)    NOT NULL                COMMENT '배송지 주소',
    detail_address    VARCHAR(200)    NOT NULL                COMMENT '배송지 상세 주소',
    shipped_at        DATETIME(6)     NULL                    COMMENT '출고 일시',
    delivered_at      DATETIME(6)     NULL                    COMMENT '배송 완료 일시',
    created_at        DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_shipment_order_id (order_id),
    INDEX idx_shipment_seller_id (seller_id, status),
    INDEX idx_shipment_status (status),
    INDEX idx_shipment_delivered (status, delivered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배송 정보';

-- Shipping Tracking Log (배송 추적 이력)
CREATE TABLE IF NOT EXISTS shipping_tracking_log (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '추적 이력 PK',
    shipping_id       BIGINT          NOT NULL                COMMENT '배송 ID',
    carrier_status    VARCHAR(30)     NOT NULL                COMMENT 'Mock 서버 원본 상태 (ACCEPTED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED)',
    mapped_status     VARCHAR(20)     NOT NULL                COMMENT '매핑된 배송 상태 (READY, IN_TRANSIT, DELIVERED)',
    location          VARCHAR(200)    NULL                    COMMENT '위치',
    description       VARCHAR(500)    NULL                    COMMENT '설명',
    tracked_at        DATETIME(6)     NOT NULL                COMMENT '추적 일시',
    created_at        DATETIME(6)     NOT NULL                COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_tracking_log_shipping (shipping_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배송 추적 이력';

-- Return Request (반품 요청)
CREATE TABLE IF NOT EXISTS return_request (
    id                      BIGINT          NOT NULL AUTO_INCREMENT COMMENT '반품 요청 PK',
    order_id                BIGINT          NOT NULL                COMMENT '주문 ID',
    order_item_id           BIGINT          NOT NULL                COMMENT '주문 항목 ID',
    member_id               BIGINT          NOT NULL                COMMENT '회원 ID',
    seller_id               BIGINT          NOT NULL                COMMENT '셀러 ID',
    product_option_id       BIGINT          NOT NULL                COMMENT '상품 옵션 ID',
    quantity                INT             NOT NULL                COMMENT '반품 수량',
    reason                  VARCHAR(30)     NOT NULL                COMMENT '반품 사유 (DEFECTIVE, WRONG_ITEM, SIZE_MISMATCH, CHANGE_OF_MIND)',
    reason_detail           VARCHAR(500)    NULL                    COMMENT '반품 상세 사유',
    status                  VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED' COMMENT '반품 상태',
    shipping_fee            DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '반품 배송비',
    shipping_fee_payer      VARCHAR(20)     NOT NULL                COMMENT '배송비 부담 주체 (BUYER, SELLER)',
    refund_amount           DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '환불 금액',
    pickup_tracking_number  VARCHAR(30)     NULL                    COMMENT '수거 송장 번호',
    inspected_at            DATETIME(6)     NULL                    COMMENT '검수 일시',
    completed_at            DATETIME(6)     NULL                    COMMENT '완료 일시',
    created_at              DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at              DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_return_order (order_id),
    INDEX idx_return_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='반품 요청';
