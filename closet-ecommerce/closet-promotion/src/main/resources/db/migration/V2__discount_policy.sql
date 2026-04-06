-- Discount Policy (할인 정책)
CREATE TABLE discount_policy (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '할인 정책 ID',
    name                VARCHAR(100)    NOT NULL                 COMMENT '할인 정책 이름',
    discount_type       VARCHAR(20)     NOT NULL                 COMMENT '할인 유형 (PERCENT/FIXED)',
    discount_value      DECIMAL(10,2)   NOT NULL                 COMMENT '할인값 (PERCENT: 비율, FIXED: 금액)',
    max_discount_amount DECIMAL(12,2)   NULL                     COMMENT '최대 할인 금액 (PERCENT 전용)',
    condition_type      VARCHAR(20)     NOT NULL                 COMMENT '조건 유형 (CATEGORY/BRAND/AMOUNT_RANGE/ALL)',
    condition_value     VARCHAR(100)    NOT NULL DEFAULT ''       COMMENT '조건값 (카테고리ID/브랜드ID/최소금액/빈값)',
    priority            INT             NOT NULL DEFAULT 1        COMMENT '우선순위 (낮을수록 높은 우선순위)',
    is_stackable        TINYINT(1)      NOT NULL DEFAULT 0        COMMENT '중복 적용 가능 여부 (1: 가능, 0: 불가)',
    started_at          DATETIME(6)     NOT NULL                 COMMENT '할인 시작일시',
    ended_at            DATETIME(6)     NOT NULL                 COMMENT '할인 종료일시',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1        COMMENT '활성 여부 (1: 활성, 0: 비활성)',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at          DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    deleted_at          DATETIME(6)     NULL                     COMMENT '삭제일시',
    PRIMARY KEY (id),
    INDEX idx_discount_policy_active (is_active, started_at, ended_at),
    INDEX idx_discount_policy_condition (condition_type, condition_value),
    INDEX idx_discount_policy_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='할인 정책';

-- Discount History (할인 적용 이력)
CREATE TABLE discount_history (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '할인 이력 ID',
    discount_policy_id  BIGINT          NOT NULL                 COMMENT '할인 정책 ID',
    order_id            BIGINT          NOT NULL                 COMMENT '주문 ID',
    member_id           BIGINT          NOT NULL                 COMMENT '회원 ID',
    original_amount     DECIMAL(12,2)   NOT NULL                 COMMENT '원래 금액',
    discount_amount     DECIMAL(12,2)   NOT NULL                 COMMENT '할인 금액',
    applied_at          DATETIME(6)     NOT NULL                 COMMENT '적용일시',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at          DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    deleted_at          DATETIME(6)     NULL                     COMMENT '삭제일시',
    PRIMARY KEY (id),
    INDEX idx_discount_history_policy (discount_policy_id),
    INDEX idx_discount_history_order (order_id),
    INDEX idx_discount_history_member (member_id),
    INDEX idx_discount_history_applied (applied_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='할인 적용 이력';
