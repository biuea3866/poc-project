-- Cart
CREATE TABLE cart (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '장바구니 ID',
    member_id   BIGINT          NOT NULL                COMMENT '회원 ID',
    created_at  DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at  DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장바구니';

-- Cart Item
CREATE TABLE cart_item (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '장바구니 항목 ID',
    cart_id           BIGINT          NOT NULL                COMMENT '장바구니 ID',
    product_id        BIGINT          NOT NULL                COMMENT '상품 ID',
    product_option_id BIGINT          NOT NULL                COMMENT '상품 옵션 ID',
    quantity          INT             NOT NULL                COMMENT '수량',
    unit_price        DECIMAL(15,2)   NOT NULL                COMMENT '단가',
    created_at        DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장바구니 항목';

-- Orders
CREATE TABLE orders (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '주문 ID',
    order_number            VARCHAR(30)     NOT NULL                 COMMENT '주문번호',
    member_id               BIGINT          NOT NULL                 COMMENT '회원 ID',
    seller_id               BIGINT          NOT NULL                 COMMENT '셀러 ID',
    total_amount            DECIMAL(15,2)   NOT NULL DEFAULT 0       COMMENT '총 상품금액',
    discount_amount         DECIMAL(15,2)   NOT NULL DEFAULT 0       COMMENT '할인금액',
    shipping_fee            DECIMAL(15,2)   NOT NULL DEFAULT 0       COMMENT '배송비',
    payment_amount          DECIMAL(15,2)   NOT NULL DEFAULT 0       COMMENT '결제금액',
    status                  VARCHAR(30)     NOT NULL                 COMMENT '주문상태',
    receiver_name           VARCHAR(50)     NOT NULL                 COMMENT '수령인명',
    receiver_phone          VARCHAR(20)     NOT NULL                 COMMENT '수령인 전화번호',
    zip_code                VARCHAR(10)     NOT NULL                 COMMENT '우편번호',
    address                 VARCHAR(200)    NOT NULL                 COMMENT '주소',
    detail_address          VARCHAR(200)    NOT NULL                 COMMENT '상세주소',
    reservation_expires_at  DATETIME(6)     NULL                     COMMENT '재고 예약 만료 시각',
    ordered_at              DATETIME(6)     NULL                     COMMENT '주문일시',
    created_at              DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at              DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    deleted_at              DATETIME(6)     NULL                     COMMENT '삭제일시',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_order_number (order_number),
    INDEX idx_order_member (member_id),
    INDEX idx_order_seller_status (seller_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문';

-- Order Item
CREATE TABLE order_item (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '주문항목 ID',
    order_id          BIGINT          NOT NULL                COMMENT '주문 ID',
    product_id        BIGINT          NOT NULL                COMMENT '상품 ID',
    product_option_id BIGINT          NOT NULL                COMMENT '상품 옵션 ID',
    product_name      VARCHAR(200)    NOT NULL                COMMENT '상품명',
    option_name       VARCHAR(200)    NOT NULL                COMMENT '옵션명',
    category_id       BIGINT          NOT NULL                COMMENT '카테고리 ID',
    quantity          INT             NOT NULL                COMMENT '수량',
    unit_price        DECIMAL(15,2)   NOT NULL                COMMENT '단가',
    total_price       DECIMAL(15,2)   NOT NULL                COMMENT '합계금액',
    status            VARCHAR(30)     NOT NULL                COMMENT '주문항목 상태',
    created_at        DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_orderitem_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 항목';

-- Order Status History
CREATE TABLE order_status_history (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '상태이력 ID',
    order_id    BIGINT          NOT NULL                COMMENT '주문 ID',
    from_status VARCHAR(30)     NULL                    COMMENT '이전 상태',
    to_status   VARCHAR(30)     NOT NULL                COMMENT '변경 상태',
    reason      VARCHAR(500)    NULL                    COMMENT '사유',
    changed_by  VARCHAR(100)    NULL                    COMMENT '변경자',
    created_at  DATETIME(6)     NOT NULL                COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_statushistory_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 상태 이력';
