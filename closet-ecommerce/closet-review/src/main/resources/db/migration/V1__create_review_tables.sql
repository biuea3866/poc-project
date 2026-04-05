-- Review 도메인 테이블 (US-801 ~ US-804)

CREATE TABLE review (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '리뷰 ID',
    product_id      BIGINT          NOT NULL COMMENT '상품 ID',
    order_item_id   BIGINT          NOT NULL COMMENT '주문 항목 ID',
    member_id       BIGINT          NOT NULL COMMENT '작성자 회원 ID',
    rating          TINYINT UNSIGNED NOT NULL COMMENT '별점 1-5',
    content         VARCHAR(2000)   NOT NULL COMMENT '리뷰 내용 (20-1000자)',
    status          VARCHAR(20)     NOT NULL DEFAULT 'VISIBLE' COMMENT '상태 (VISIBLE/HIDDEN/DELETED)',
    edit_count      INT             NOT NULL DEFAULT 0 COMMENT '수정 횟수 (최대 3회)',
    has_image       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '이미지 포함 여부',
    height          INT             NULL COMMENT '작성자 키 (cm)',
    weight          INT             NULL COMMENT '작성자 몸무게 (kg)',
    normal_size     VARCHAR(20)     NULL COMMENT '평소 사이즈',
    purchased_size  VARCHAR(20)     NULL COMMENT '구매 사이즈',
    fit_type        VARCHAR(20)     NULL COMMENT '핏 타입 (SMALL/PERFECT/LARGE)',
    helpful_count   INT             NOT NULL DEFAULT 0 COMMENT '도움이 됐어요 수',
    created_at      DATETIME(6)     NOT NULL COMMENT '작성일시',
    updated_at      DATETIME(6)     NOT NULL COMMENT '수정일시',
    deleted_at      DATETIME(6)     NULL COMMENT '삭제일시',
    PRIMARY KEY (id),
    INDEX idx_review_product_status (product_id, status),
    INDEX idx_review_member (member_id),
    INDEX idx_review_order_item (order_item_id, member_id),
    INDEX idx_review_product_helpful (product_id, status, helpful_count),
    INDEX idx_review_product_rating (product_id, status, rating),
    INDEX idx_review_similar_body (product_id, status, height, weight)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리뷰';

CREATE TABLE review_image (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '리뷰 이미지 ID',
    review_id       BIGINT          NOT NULL COMMENT '리뷰 ID',
    image_url       VARCHAR(500)    NOT NULL COMMENT '원본 이미지 URL',
    thumbnail_url   VARCHAR(500)    NOT NULL COMMENT '썸네일 이미지 URL',
    display_order   INT             NOT NULL COMMENT '표시 순서',
    created_at      DATETIME(6)     NOT NULL COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_review_image_review (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리뷰 이미지';

CREATE TABLE review_edit_history (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '수정 이력 ID',
    review_id        BIGINT        NOT NULL COMMENT '리뷰 ID',
    previous_content VARCHAR(2000) NOT NULL COMMENT '수정 전 내용',
    new_content      VARCHAR(2000) NOT NULL COMMENT '수정 후 내용',
    edit_count       INT           NOT NULL COMMENT '수정 회차',
    created_at       DATETIME(6)   NOT NULL COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_review_edit_history_review (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리뷰 수정 이력';

CREATE TABLE review_summary (
    id                    BIGINT   NOT NULL AUTO_INCREMENT COMMENT '집계 ID',
    product_id            BIGINT   NOT NULL COMMENT '상품 ID',
    total_count           INT      NOT NULL DEFAULT 0 COMMENT '전체 리뷰 수',
    avg_rating            DOUBLE   NOT NULL DEFAULT 0.0 COMMENT '평균 별점',
    total_rating_sum      BIGINT   NOT NULL DEFAULT 0 COMMENT '별점 합계',
    rating_1_count        INT      NOT NULL DEFAULT 0 COMMENT '1점 리뷰 수',
    rating_2_count        INT      NOT NULL DEFAULT 0 COMMENT '2점 리뷰 수',
    rating_3_count        INT      NOT NULL DEFAULT 0 COMMENT '3점 리뷰 수',
    rating_4_count        INT      NOT NULL DEFAULT 0 COMMENT '4점 리뷰 수',
    rating_5_count        INT      NOT NULL DEFAULT 0 COMMENT '5점 리뷰 수',
    fit_small_count       INT      NOT NULL DEFAULT 0 COMMENT '작아요 수',
    fit_perfect_count     INT      NOT NULL DEFAULT 0 COMMENT '딱 맞아요 수',
    fit_large_count       INT      NOT NULL DEFAULT 0 COMMENT '커요 수',
    photo_review_count    INT      NOT NULL DEFAULT 0 COMMENT '포토 리뷰 수',
    created_at            DATETIME(6) NOT NULL COMMENT '생성일시',
    updated_at            DATETIME(6) NOT NULL COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_review_summary_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리뷰 집계';

CREATE TABLE review_helpful (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '도움이 됐어요 ID',
    review_id       BIGINT      NOT NULL COMMENT '리뷰 ID',
    member_id       BIGINT      NOT NULL COMMENT '회원 ID',
    created_at      DATETIME(6) NOT NULL COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_review_helpful_review_member (review_id, member_id),
    INDEX idx_review_helpful_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리뷰 도움이 됐어요';

-- Outbox, processed_event 테이블은 closet-common에서 공통 관리
CREATE TABLE IF NOT EXISTS outbox_event (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    aggregate_type  VARCHAR(50)   NOT NULL,
    aggregate_id    VARCHAR(50)   NOT NULL,
    event_type      VARCHAR(50)   NOT NULL,
    topic           VARCHAR(100)  NOT NULL,
    partition_key   VARCHAR(100)  NOT NULL,
    payload         TEXT          NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count     INT           NOT NULL DEFAULT 0,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_outbox_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
