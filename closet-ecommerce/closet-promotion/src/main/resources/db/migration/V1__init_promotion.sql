-- Coupon
CREATE TABLE coupon (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '쿠폰 ID',
    name                VARCHAR(100)    NOT NULL                 COMMENT '쿠폰명',
    coupon_type         VARCHAR(30)     NOT NULL                 COMMENT '쿠폰 유형 (FIXED_AMOUNT/PERCENTAGE)',
    discount_value      DECIMAL(15,2)   NOT NULL                 COMMENT '할인 값 (정액: 원, 정률: %)',
    max_discount_amount DECIMAL(15,2)   NULL                     COMMENT '최대 할인 금액 (정률 쿠폰용)',
    min_order_amount    DECIMAL(15,2)   NOT NULL DEFAULT 0       COMMENT '최소 주문 금액',
    scope               VARCHAR(30)     NOT NULL DEFAULT 'ALL'   COMMENT '적용 범위 (ALL/CATEGORY/BRAND/PRODUCT)',
    scope_ids           TEXT            NULL                     COMMENT '적용 대상 ID 목록 (콤마 구분)',
    total_quantity      INT             NOT NULL                 COMMENT '총 발급 수량',
    issued_count        INT             NOT NULL DEFAULT 0       COMMENT '발급된 수량',
    valid_from          DATETIME(6)     NOT NULL                 COMMENT '유효 시작일',
    valid_to            DATETIME(6)     NOT NULL                 COMMENT '유효 종료일',
    status              VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE/EXPIRED/EXHAUSTED)',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at          DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_coupon_status (status),
    INDEX idx_coupon_valid (valid_from, valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='쿠폰';

-- Member Coupon
CREATE TABLE member_coupon (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '회원 쿠폰 ID',
    coupon_id       BIGINT          NOT NULL                 COMMENT '쿠폰 ID',
    member_id       BIGINT          NOT NULL                 COMMENT '회원 ID',
    status          VARCHAR(30)     NOT NULL DEFAULT 'AVAILABLE' COMMENT '상태 (AVAILABLE/USED/EXPIRED)',
    used_order_id   BIGINT          NULL                     COMMENT '사용 주문 ID',
    issued_at       DATETIME(6)     NOT NULL                 COMMENT '발급일시',
    used_at         DATETIME(6)     NULL                     COMMENT '사용일시',
    expired_at      DATETIME(6)     NOT NULL                 COMMENT '만료일시',
    created_at      DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at      DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_member_coupon_member (member_id),
    INDEX idx_member_coupon_coupon (coupon_id),
    INDEX idx_member_coupon_status (member_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 쿠폰';

-- Time Sale
CREATE TABLE time_sale (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '타임세일 ID',
    product_id      BIGINT          NOT NULL                 COMMENT '상품 ID',
    sale_price      DECIMAL(15,2)   NOT NULL                 COMMENT '세일 가격',
    limit_quantity  INT             NOT NULL                 COMMENT '한정 수량',
    sold_count      INT             NOT NULL DEFAULT 0       COMMENT '판매 수량',
    start_at        DATETIME(6)     NOT NULL                 COMMENT '시작일시',
    end_at          DATETIME(6)     NOT NULL                 COMMENT '종료일시',
    status          VARCHAR(30)     NOT NULL DEFAULT 'SCHEDULED' COMMENT '상태 (SCHEDULED/ACTIVE/ENDED/EXHAUSTED)',
    created_at      DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at      DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_time_sale_status (status),
    INDEX idx_time_sale_product (product_id),
    INDEX idx_time_sale_period (start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='타임세일';

-- Point Policy
CREATE TABLE point_policy (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '적립금 정책 ID',
    event_type      VARCHAR(30)     NOT NULL                 COMMENT '이벤트 유형 (PURCHASE/REVIEW/PHOTO_REVIEW/SIGN_UP)',
    grade_type      VARCHAR(30)     NOT NULL DEFAULT 'ALL'   COMMENT '등급 유형 (NORMAL/SILVER/GOLD/PLATINUM/ALL)',
    point_amount    INT             NULL                     COMMENT '정액 적립금',
    point_rate      DECIMAL(5,2)    NULL                     COMMENT '적립률 (%)',
    description     VARCHAR(500)    NOT NULL                 COMMENT '설명',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '활성 여부',
    created_at      DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at      DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_point_policy_event (event_type),
    INDEX idx_point_policy_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='적립금 정책';
