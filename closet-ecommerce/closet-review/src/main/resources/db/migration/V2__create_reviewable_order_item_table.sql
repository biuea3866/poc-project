-- 리뷰 작성 가능 주문 아이템 테이블 (US-801)
-- 구매확정(CONFIRMED) 이벤트 수신 시 기록하여 리뷰 작성 자격을 검증한다.

CREATE TABLE reviewable_order_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '리뷰 가능 주문 아이템 ID',
    order_item_id   BIGINT          NOT NULL COMMENT '주문 항목 ID',
    member_id       BIGINT          NOT NULL COMMENT '회원 ID',
    product_id      BIGINT          NOT NULL COMMENT '상품 ID',
    created_at      DATETIME(6)     NOT NULL COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_reviewable_order_item (order_item_id),
    INDEX idx_reviewable_order_item_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리뷰 작성 가능 주문 아이템';
