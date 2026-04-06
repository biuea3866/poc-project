-- ============================================================
-- CS Service Database Schema
-- ============================================================

CREATE TABLE inquiry (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '문의 고유 식별자',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    order_id          BIGINT          NULL     COMMENT '주문 ID (주문 관련 문의 시)',
    category          VARCHAR(30)     NOT NULL COMMENT '문의 카테고리 (PRODUCT, SHIPPING, PAYMENT, REFUND, OTHER)',
    title             VARCHAR(200)    NOT NULL COMMENT '문의 제목',
    content           TEXT            NOT NULL COMMENT '문의 내용',
    status            VARCHAR(30)     NOT NULL DEFAULT 'OPEN' COMMENT '문의 상태 (OPEN, ANSWERED, CLOSED)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='1:1 문의';

CREATE TABLE inquiry_reply (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '답변 고유 식별자',
    inquiry_id        BIGINT          NOT NULL COMMENT '문의 ID',
    reply_type        VARCHAR(30)     NOT NULL COMMENT '답변 유형 (SELLER, ADMIN)',
    content           TEXT            NOT NULL COMMENT '답변 내용',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='문의 답변';

CREATE TABLE faq (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'FAQ 고유 식별자',
    category          VARCHAR(30)     NOT NULL COMMENT 'FAQ 카테고리 (PRODUCT, SHIPPING, PAYMENT, REFUND, MEMBERSHIP, OTHER)',
    question          VARCHAR(500)    NOT NULL COMMENT '질문',
    answer            TEXT            NOT NULL COMMENT '답변',
    sort_order        INT             NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    is_visible        TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '노출 여부 (1=노출, 0=숨김)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='자주 묻는 질문';

-- 인덱스
CREATE INDEX idx_inquiry_member ON inquiry (member_id, created_at DESC);
CREATE INDEX idx_inquiry_status ON inquiry (status);
CREATE INDEX idx_inquiry_reply_inquiry ON inquiry_reply (inquiry_id, created_at);
CREATE INDEX idx_faq_category ON faq (category, sort_order, is_visible);
