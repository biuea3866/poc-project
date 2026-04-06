-- Snap (OOTD 스냅)
CREATE TABLE snap (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '스냅 ID',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    image_url         VARCHAR(500)    NOT NULL COMMENT '이미지 URL',
    description       VARCHAR(1000)   NULL     COMMENT '설명',
    like_count        BIGINT          NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    report_count      INT             NOT NULL DEFAULT 0 COMMENT '신고 수',
    status            VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE/HIDDEN/REPORTED)',
    created_at        DATETIME(6)     NOT NULL COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id),
    INDEX idx_snap_member (member_id, created_at DESC),
    INDEX idx_snap_status (status, created_at DESC),
    INDEX idx_snap_like (like_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OOTD 스냅';

-- Snap Product Tag (스냅 상품 태그)
CREATE TABLE snap_product_tag (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '스냅 상품 태그 ID',
    snap_id           BIGINT          NOT NULL COMMENT '스냅 ID',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    position_x        DOUBLE          NOT NULL COMMENT '태그 X 좌표 (0.0~1.0)',
    position_y        DOUBLE          NOT NULL COMMENT '태그 Y 좌표 (0.0~1.0)',
    created_at        DATETIME(6)     NOT NULL COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_snap_product_tag_snap (snap_id),
    INDEX idx_snap_product_tag_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='스냅 상품 태그';

-- Snap Like (스냅 좋아요)
CREATE TABLE snap_like (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '스냅 좋아요 ID',
    snap_id           BIGINT          NOT NULL COMMENT '스냅 ID',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    created_at        DATETIME(6)     NOT NULL COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE INDEX ux_snap_like (snap_id, member_id),
    INDEX idx_snap_like_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='스냅 좋아요';
