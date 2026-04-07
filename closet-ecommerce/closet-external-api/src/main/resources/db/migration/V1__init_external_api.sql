-- Mock 결제 PG 데이터
CREATE TABLE mock_payment (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    provider        VARCHAR(30)     NOT NULL COMMENT 'PG사: TOSS, KAKAO_PAY, NAVER_PAY, DANAL',
    payment_key     VARCHAR(200)    NOT NULL COMMENT 'PG사 결제키 (tid 등)',
    order_id        VARCHAR(100)    NOT NULL COMMENT '가맹점 주문 ID',
    status          VARCHAR(30)     NOT NULL COMMENT 'READY, DONE, CANCELED, REFUNDED',
    method          VARCHAR(30)     NULL COMMENT '결제 수단: CARD, MONEY, PHONE 등',
    total_amount    BIGINT          NOT NULL COMMENT '총 결제 금액',
    balance_amount  BIGINT          NOT NULL COMMENT '잔여 금액 (부분취소 후)',
    cancel_amount   BIGINT          NOT NULL DEFAULT 0 COMMENT '취소 금액',
    cancel_reason   VARCHAR(200)    NULL COMMENT '취소 사유',
    order_name      VARCHAR(200)    NULL COMMENT '상품명',
    buyer_name      VARCHAR(50)     NULL COMMENT '구매자명',
    buyer_tel       VARCHAR(20)     NULL COMMENT '구매자 연락처',
    card_number     VARCHAR(30)     NULL COMMENT '마스킹된 카드번호',
    card_type       VARCHAR(20)     NULL COMMENT '카드 유형: 신용, 체크',
    approve_no      VARCHAR(30)     NULL COMMENT '승인번호',
    approved_at     DATETIME(6)     NULL COMMENT '승인 일시',
    canceled_at     DATETIME(6)     NULL COMMENT '취소 일시',
    extra_data      TEXT            NULL COMMENT 'PG사별 추가 데이터 문자열',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_key (payment_key),
    INDEX idx_order_id (order_id),
    INDEX idx_provider_status (provider, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mock PG 결제 데이터';

-- Mock 택배 배송 데이터
CREATE TABLE mock_shipment (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    carrier             VARCHAR(30)     NOT NULL COMMENT '택배사: CJ_LOGISTICS, LOGEN, LOTTE_GLOBAL, EPOST',
    tracking_number     VARCHAR(50)     NOT NULL COMMENT '운송장 번호',
    order_id            VARCHAR(100)    NOT NULL COMMENT '주문 ID',
    status              VARCHAR(30)     NOT NULL COMMENT 'ACCEPTED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED',
    sender_name         VARCHAR(50)     NOT NULL COMMENT '보내는 사람',
    receiver_name       VARCHAR(50)     NOT NULL COMMENT '받는 사람',
    receiver_address    VARCHAR(500)    NOT NULL COMMENT '받는 주소',
    receiver_phone      VARCHAR(20)     NOT NULL COMMENT '받는 연락처',
    delivered_at        DATETIME(6)     NULL COMMENT '배송 완료 일시',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tracking_number (tracking_number),
    INDEX idx_order_id (order_id),
    INDEX idx_carrier_status (carrier, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mock 택배 배송 데이터';

-- Mock 배송 추적 이력
CREATE TABLE mock_tracking_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    shipment_id     BIGINT          NOT NULL COMMENT '배송 ID',
    status          VARCHAR(30)     NOT NULL COMMENT '상태',
    description     VARCHAR(200)    NOT NULL COMMENT '상세 설명',
    location        VARCHAR(100)    NOT NULL COMMENT '위치',
    tracked_at      DATETIME(6)     NOT NULL COMMENT '추적 일시',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_shipment_id (shipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mock 배송 추적 이력';
