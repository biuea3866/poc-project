-- Point Balance (적립금 잔액)
CREATE TABLE point_balance (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '적립금 잔액 ID',
    member_id           BIGINT          NOT NULL                 COMMENT '회원 ID',
    total_points        INT             NOT NULL DEFAULT 0       COMMENT '총 적립금',
    available_points    INT             NOT NULL DEFAULT 0       COMMENT '사용 가능 적립금',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at          DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE INDEX ux_point_balance_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='적립금 잔액';

-- Point History (적립금 이력)
CREATE TABLE point_history (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '적립금 이력 ID',
    member_id           BIGINT          NOT NULL                 COMMENT '회원 ID',
    amount              INT             NOT NULL                 COMMENT '변동 금액 (양수: 적립, 음수: 차감)',
    balance_after       INT             NOT NULL                 COMMENT '변동 후 잔액',
    transaction_type    VARCHAR(30)     NOT NULL                 COMMENT '거래 유형 (EARN/USE/CANCEL_EARN/CANCEL_USE/EXPIRE)',
    reference_type      VARCHAR(30)     NULL                     COMMENT '참조 유형 (ORDER/REVIEW/EVENT)',
    reference_id        BIGINT          NULL                     COMMENT '참조 ID',
    expired_at          DATETIME(6)     NULL                     COMMENT '만료일시',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_point_history_member (member_id, created_at DESC),
    INDEX idx_point_history_reference (reference_type, reference_id),
    INDEX idx_point_history_transaction (transaction_type),
    INDEX idx_point_history_expired (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='적립금 이력';
