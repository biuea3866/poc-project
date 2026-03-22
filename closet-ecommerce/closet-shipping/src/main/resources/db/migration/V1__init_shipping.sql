-- Shipment
CREATE TABLE shipment (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '배송 고유 식별자',
    order_id          BIGINT          NOT NULL COMMENT '주문 ID',
    seller_id         BIGINT          NOT NULL COMMENT '셀러 ID',
    carrier           VARCHAR(30)     NULL     COMMENT '택배사 코드 (CJ, HANJIN, LOTTE, LOGEN)',
    tracking_number   VARCHAR(50)     NULL     COMMENT '송장 번호',
    status            VARCHAR(30)     NOT NULL DEFAULT 'PENDING' COMMENT '배송 상태',
    receiver_name     VARCHAR(50)     NOT NULL COMMENT '수령인 이름',
    receiver_phone    VARCHAR(20)     NOT NULL COMMENT '수령인 전화번호',
    address           VARCHAR(500)    NOT NULL COMMENT '배송지 주소',
    shipped_at        DATETIME(6)     NULL     COMMENT '출고 일시',
    delivered_at      DATETIME(6)     NULL     COMMENT '배송 완료 일시',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_shipment_order_id (order_id),
    INDEX idx_shipment_seller_id (seller_id, status),
    INDEX idx_shipment_status (status),
    INDEX idx_shipment_tracking (carrier, tracking_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배송 정보';

-- Shipment Status History
CREATE TABLE shipment_status_history (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '배송 상태 이력 고유 식별자',
    shipment_id       BIGINT          NOT NULL COMMENT '배송 ID',
    from_status       VARCHAR(30)     NULL     COMMENT '이전 상태',
    to_status         VARCHAR(30)     NOT NULL COMMENT '변경 상태',
    reason            VARCHAR(500)    NULL     COMMENT '사유',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_shipment_status_history_shipment (shipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배송 상태 변경 이력';

-- Return Request
CREATE TABLE return_request (
    id                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '반품 요청 고유 식별자',
    order_id              BIGINT          NOT NULL COMMENT '주문 ID',
    order_item_id         BIGINT          NOT NULL COMMENT '주문 항목 ID',
    type                  VARCHAR(30)     NOT NULL COMMENT '반품 유형 (RETURN, EXCHANGE)',
    reason_type           VARCHAR(30)     NOT NULL COMMENT '반품 사유 유형 (CHANGE_OF_MIND, DEFECT, WRONG_ITEM, SIZE_MISMATCH)',
    reason_detail         VARCHAR(500)    NULL     COMMENT '반품 상세 사유',
    status                VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED' COMMENT '반품 상태',
    return_tracking_number VARCHAR(50)    NULL     COMMENT '반품 송장 번호',
    return_carrier        VARCHAR(30)     NULL     COMMENT '반품 택배사 코드',
    shipping_fee_bearer   VARCHAR(30)     NOT NULL COMMENT '배송비 부담 주체 (BUYER, SELLER)',
    return_shipping_fee   BIGINT          NOT NULL DEFAULT 0 COMMENT '반품 배송비 (원)',
    requested_at          DATETIME(6)     NOT NULL COMMENT '반품 요청 일시',
    approved_at           DATETIME(6)     NULL     COMMENT '승인 일시',
    rejected_at           DATETIME(6)     NULL     COMMENT '거절 일시',
    created_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_return_request_order (order_id, order_item_id),
    INDEX idx_return_request_status (status, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='반품 요청';
