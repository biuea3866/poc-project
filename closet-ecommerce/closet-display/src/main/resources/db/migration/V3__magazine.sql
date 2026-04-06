-- Magazine (스타일 매거진)
CREATE TABLE magazine (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '매거진 ID',
    title             VARCHAR(200)    NOT NULL COMMENT '매거진 제목',
    subtitle          VARCHAR(300)    NULL     COMMENT '매거진 부제목',
    content_body      TEXT            NOT NULL COMMENT '매거진 본문',
    thumbnail_url     VARCHAR(500)    NULL     COMMENT '썸네일 이미지 URL',
    category          VARCHAR(50)     NOT NULL COMMENT '카테고리 (TREND/STYLING/LOOKBOOK/NEWS)',
    author_name       VARCHAR(50)     NOT NULL COMMENT '작성자 이름',
    view_count        BIGINT          NOT NULL DEFAULT 0 COMMENT '조회수',
    is_published      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '발행 여부 (1=발행, 0=미발행)',
    published_at      DATETIME(6)     NULL     COMMENT '발행일시',
    created_at        DATETIME(6)     NOT NULL COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id),
    INDEX idx_magazine_published (is_published, published_at DESC),
    INDEX idx_magazine_category (category, is_published, published_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='스타일 매거진';

-- Magazine Product (매거진 상품 연결)
CREATE TABLE magazine_product (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '매거진 상품 ID',
    magazine_id       BIGINT          NOT NULL COMMENT '매거진 ID',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    sort_order        INT             NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    created_at        DATETIME(6)     NOT NULL COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_magazine_product_magazine (magazine_id, sort_order),
    INDEX idx_magazine_product_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='매거진 상품 연결';

-- Magazine Tag (매거진 태그)
CREATE TABLE magazine_tag (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '매거진 태그 ID',
    magazine_id       BIGINT          NOT NULL COMMENT '매거진 ID',
    tag_name          VARCHAR(50)     NOT NULL COMMENT '태그명',
    created_at        DATETIME(6)     NOT NULL COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_magazine_tag_magazine (magazine_id),
    INDEX idx_magazine_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='매거진 태그';
