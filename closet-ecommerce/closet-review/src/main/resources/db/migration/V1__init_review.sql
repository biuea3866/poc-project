-- ============================================================
-- Review Service Database Schema
-- ============================================================

CREATE TABLE review (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '리뷰 고유 식별자',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    order_item_id     BIGINT          NOT NULL COMMENT '주문 상품 ID',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    rating            INT             NOT NULL COMMENT '별점 (1~5)',
    content           TEXT            NOT NULL COMMENT '리뷰 내용',
    height            INT             NULL     COMMENT '작성자 키 (cm)',
    weight            INT             NULL     COMMENT '작성자 몸무게 (kg)',
    size_feeling      VARCHAR(20)     NULL     COMMENT '사이즈 체감 (SMALL, JUST_RIGHT, LARGE)',
    helpful_count     INT             NOT NULL DEFAULT 0 COMMENT '도움돼요 수',
    status            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '리뷰 상태 (ACTIVE, HIDDEN, DELETED)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item_id (order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품 리뷰';

CREATE INDEX idx_review_product_id ON review (product_id, status, created_at DESC);
CREATE INDEX idx_review_member_id ON review (member_id, status);

CREATE TABLE review_image (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '리뷰 이미지 고유 식별자',
    review_id         BIGINT          NOT NULL COMMENT '리뷰 ID',
    image_url         VARCHAR(500)    NOT NULL COMMENT '이미지 URL (S3)',
    sort_order        INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='리뷰 이미지';

CREATE INDEX idx_review_image_review_id ON review_image (review_id, sort_order);
