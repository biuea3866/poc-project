-- ============================================================
-- Display Service Database Schema
-- ============================================================

CREATE TABLE banner (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '배너 고유 식별자',
    title             VARCHAR(100)    NOT NULL COMMENT '배너 제목',
    image_url         VARCHAR(500)    NOT NULL COMMENT '배너 이미지 URL',
    link_url          VARCHAR(500)    NOT NULL COMMENT '배너 클릭 시 이동 URL',
    position          VARCHAR(30)     NOT NULL COMMENT '배너 위치 (MAIN_TOP, MAIN_MIDDLE, CATEGORY)',
    sort_order        INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    is_visible        TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '노출 여부 (1=노출, 0=숨김)',
    start_at          DATETIME(6)     NOT NULL COMMENT '노출 시작일시',
    end_at            DATETIME(6)     NOT NULL COMMENT '노출 종료일시',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='배너 관리';

CREATE INDEX idx_banner_position ON banner (position, is_visible, sort_order);
CREATE INDEX idx_banner_period ON banner (start_at, end_at, is_visible);

CREATE TABLE exhibition (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '기획전 고유 식별자',
    title             VARCHAR(100)    NOT NULL COMMENT '기획전 제목',
    description       TEXT            NULL     COMMENT '기획전 설명',
    thumbnail_url     VARCHAR(500)    NULL     COMMENT '기획전 썸네일 이미지 URL',
    status            VARCHAR(30)     NOT NULL DEFAULT 'DRAFT' COMMENT '기획전 상태 (DRAFT, ACTIVE, ENDED)',
    start_at          DATETIME(6)     NOT NULL COMMENT '기획전 시작일시',
    end_at            DATETIME(6)     NOT NULL COMMENT '기획전 종료일시',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='기획전 관리';

CREATE INDEX idx_exhibition_status ON exhibition (status, start_at, end_at);

CREATE TABLE exhibition_product (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '기획전 상품 고유 식별자',
    exhibition_id     BIGINT          NOT NULL COMMENT '기획전 ID',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    sort_order        INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    discount_rate     INT             NOT NULL DEFAULT 0 COMMENT '기획전 특별 할인율 (%)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='기획전 상품 매핑';

CREATE INDEX idx_exhibition_product_exhibition_id ON exhibition_product (exhibition_id, sort_order);
CREATE INDEX idx_exhibition_product_product_id ON exhibition_product (product_id);

CREATE TABLE ranking_snapshot (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '랭킹 스냅샷 고유 식별자',
    category_id       BIGINT          NOT NULL COMMENT '카테고리 ID',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    rank_position     INT             NOT NULL COMMENT '순위',
    score             DOUBLE          NOT NULL COMMENT '랭킹 점수',
    period_type       VARCHAR(30)     NOT NULL COMMENT '기간 유형 (REALTIME, DAILY, WEEKLY)',
    snapshot_date     DATETIME(6)     NOT NULL COMMENT '스냅샷 일시',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='랭킹 스냅샷';

CREATE INDEX idx_ranking_snapshot_category ON ranking_snapshot (category_id, period_type, snapshot_date, rank_position);
CREATE INDEX idx_ranking_snapshot_product ON ranking_snapshot (product_id, period_type);
