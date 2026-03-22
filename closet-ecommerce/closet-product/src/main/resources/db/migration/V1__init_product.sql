-- ============================================================
-- Product Service Database Schema
-- ============================================================

CREATE TABLE category (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '카테고리 고유 식별자',
    parent_id         BIGINT          NULL     COMMENT '부모 카테고리 ID (최상위는 NULL)',
    name              VARCHAR(50)     NOT NULL COMMENT '카테고리명',
    depth             INT             NOT NULL COMMENT '깊이 (1=대분류, 2=중분류, 3=소분류)',
    sort_order        INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    status            TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부 (1=활성, 0=비활성)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품 카테고리 (최대 3depth 계층 구조)';

CREATE INDEX idx_category_parent_id ON category (parent_id, sort_order);

CREATE TABLE brand (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '브랜드 고유 식별자',
    name              VARCHAR(100)    NOT NULL COMMENT '브랜드명',
    logo_url          VARCHAR(500)    NULL     COMMENT '로고 이미지 URL',
    description       TEXT            NULL     COMMENT '브랜드 소개',
    seller_id         BIGINT          NOT NULL COMMENT '셀러 ID',
    status            TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부 (1=활성, 0=비활성)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='브랜드 정보';

CREATE INDEX idx_brand_seller_id ON brand (seller_id);

CREATE TABLE product (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '상품 고유 식별자',
    name              VARCHAR(100)    NOT NULL COMMENT '상품명 (2~100자)',
    description       TEXT            NOT NULL COMMENT '상품 상세 설명',
    brand_id          BIGINT          NOT NULL COMMENT '브랜드 ID',
    category_id       BIGINT          NOT NULL COMMENT '카테고리 ID',
    base_price        BIGINT          NOT NULL COMMENT '정가 (원 단위)',
    sale_price        BIGINT          NOT NULL COMMENT '판매가 (원 단위)',
    discount_rate     INT             NOT NULL DEFAULT 0 COMMENT '할인율 (%)',
    status            VARCHAR(30)     NOT NULL DEFAULT 'DRAFT' COMMENT '상품 상태 (DRAFT, ACTIVE, SOLD_OUT, INACTIVE)',
    season            VARCHAR(30)     NULL     COMMENT '시즌 (SS, FW, ALL)',
    fit_type          VARCHAR(30)     NULL     COMMENT '핏 타입 (OVERSIZED, REGULAR, SLIM)',
    gender            VARCHAR(30)     NULL     COMMENT '성별 (MALE, FEMALE, UNISEX)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품 기본 정보';

CREATE INDEX idx_product_category_id ON product (category_id, status, deleted_at);
CREATE INDEX idx_product_brand_id ON product (brand_id, status, deleted_at);
CREATE INDEX idx_product_status ON product (status, created_at);

CREATE TABLE product_option (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '상품 옵션 고유 식별자',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    size              VARCHAR(30)     NOT NULL COMMENT '사이즈 (XS, S, M, L, XL, XXL, XXXL, FREE)',
    color_name        VARCHAR(50)     NOT NULL COMMENT '색상명',
    color_hex         VARCHAR(7)      NOT NULL COMMENT '색상 HEX 코드',
    sku_code          VARCHAR(50)     NOT NULL COMMENT 'SKU 코드',
    additional_price  BIGINT          NOT NULL DEFAULT 0 COMMENT '추가 금액 (원 단위)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_code (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품 옵션 (사이즈/색상 조합)';

CREATE INDEX idx_product_option_product_id ON product_option (product_id);

CREATE TABLE product_image (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '상품 이미지 고유 식별자',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    image_url         VARCHAR(500)    NOT NULL COMMENT '이미지 URL',
    type              VARCHAR(30)     NOT NULL COMMENT '이미지 유형 (MAIN, DETAIL)',
    sort_order        INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품 이미지';

CREATE INDEX idx_product_image_product_id ON product_image (product_id, type, sort_order);

CREATE TABLE size_guide (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '사이즈 가이드 고유 식별자',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    size              VARCHAR(30)     NOT NULL COMMENT '사이즈명',
    shoulder_width    DECIMAL(6,1)    NULL     COMMENT '어깨너비 (cm)',
    chest_width       DECIMAL(6,1)    NULL     COMMENT '가슴단면 (cm)',
    total_length      DECIMAL(6,1)    NULL     COMMENT '총장 (cm)',
    sleeve_length     DECIMAL(6,1)    NULL     COMMENT '소매길이 (cm)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사이즈 가이드 (실측 정보)';

CREATE INDEX idx_size_guide_product_id ON size_guide (product_id);
