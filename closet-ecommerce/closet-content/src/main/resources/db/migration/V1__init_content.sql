-- ============================================================
-- Content Service Database Schema
-- ============================================================

CREATE TABLE magazine (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '매거진 고유 식별자',
    title             VARCHAR(200)    NOT NULL COMMENT '매거진 제목',
    subtitle          VARCHAR(300)    NULL     COMMENT '매거진 부제목',
    content           TEXT            NOT NULL COMMENT '매거진 본문 내용',
    thumbnail_url     VARCHAR(500)    NULL     COMMENT '썸네일 이미지 URL',
    author            VARCHAR(100)    NOT NULL COMMENT '작성자명',
    status            VARCHAR(30)     NOT NULL DEFAULT 'DRAFT' COMMENT '매거진 상태 (DRAFT, PUBLISHED, ARCHIVED)',
    published_at      DATETIME(6)     NULL     COMMENT '발행일시',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='스타일 매거진';

CREATE INDEX idx_magazine_status ON magazine (status, published_at);

CREATE TABLE magazine_tag (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '매거진 태그 고유 식별자',
    magazine_id       BIGINT          NOT NULL COMMENT '매거진 ID',
    tag_name          VARCHAR(50)     NOT NULL COMMENT '태그명',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='매거진 태그';

CREATE INDEX idx_magazine_tag_magazine_id ON magazine_tag (magazine_id);
CREATE INDEX idx_magazine_tag_name ON magazine_tag (tag_name);

CREATE TABLE coordination (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '코디 고유 식별자',
    title             VARCHAR(200)    NOT NULL COMMENT '코디 제목',
    description       TEXT            NULL     COMMENT '코디 설명',
    thumbnail_url     VARCHAR(500)    NULL     COMMENT '썸네일 이미지 URL',
    style             VARCHAR(30)     NOT NULL COMMENT '스타일 (CASUAL, STREET, MINIMAL, CLASSIC)',
    season            VARCHAR(30)     NOT NULL COMMENT '시즌 (SS, FW, ALL)',
    gender            VARCHAR(30)     NOT NULL COMMENT '성별 (MALE, FEMALE, UNISEX)',
    status            VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE' COMMENT '코디 상태 (ACTIVE, INACTIVE)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='코디 추천';

CREATE INDEX idx_coordination_style ON coordination (style, status);
CREATE INDEX idx_coordination_season ON coordination (season, gender, status);

CREATE TABLE coordination_product (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '코디 상품 고유 식별자',
    coordination_id   BIGINT          NOT NULL COMMENT '코디 ID',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    sort_order        INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    description       VARCHAR(200)    NULL     COMMENT '상품 설명 (코디 내 역할)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='코디 추천 상품';

CREATE INDEX idx_coordination_product_coordination_id ON coordination_product (coordination_id, sort_order);

CREATE TABLE ootd_snap (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'OOTD 스냅 고유 식별자',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    image_url         VARCHAR(500)    NOT NULL COMMENT '스냅 이미지 URL',
    content           VARCHAR(500)    NULL     COMMENT '스냅 설명',
    like_count        INT             NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    status            VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE' COMMENT '스냅 상태 (ACTIVE, HIDDEN, DELETED)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='OOTD 스냅 (오늘의 착장)';

CREATE INDEX idx_ootd_snap_member_id ON ootd_snap (member_id, status);
CREATE INDEX idx_ootd_snap_status ON ootd_snap (status, created_at);
