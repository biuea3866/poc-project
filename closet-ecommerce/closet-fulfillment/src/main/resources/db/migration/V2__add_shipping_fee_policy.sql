-- Shipping Fee Policy (배송비 정책, PD-15)
CREATE TABLE IF NOT EXISTS shipping_fee_policy (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '배송비 정책 PK',
    type            VARCHAR(20)     NOT NULL                COMMENT '유형 (RETURN, EXCHANGE)',
    reason          VARCHAR(30)     NOT NULL                COMMENT '사유 (DEFECTIVE, WRONG_ITEM, SIZE_MISMATCH, CHANGE_OF_MIND)',
    payer           VARCHAR(20)     NOT NULL                COMMENT '부담 주체 (BUYER, SELLER)',
    fee             DECIMAL(15,2)   NOT NULL DEFAULT 0      COMMENT '배송비 (원)',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '활성 여부',
    created_at      DATETIME(6)     NOT NULL                COMMENT '생성일시',
    updated_at      DATETIME(6)     NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_policy_type_reason (type, reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배송비 정책';

-- 초기 데이터: 반품 배송비 (PD-11)
INSERT INTO shipping_fee_policy (type, reason, payer, fee, is_active, created_at, updated_at) VALUES
('RETURN', 'DEFECTIVE', 'SELLER', 0, 1, NOW(6), NOW(6)),
('RETURN', 'WRONG_ITEM', 'SELLER', 0, 1, NOW(6), NOW(6)),
('RETURN', 'SIZE_MISMATCH', 'BUYER', 3000, 1, NOW(6), NOW(6)),
('RETURN', 'CHANGE_OF_MIND', 'BUYER', 3000, 1, NOW(6), NOW(6));

-- 초기 데이터: 교환 배송비 (왕복 6,000원)
INSERT INTO shipping_fee_policy (type, reason, payer, fee, is_active, created_at, updated_at) VALUES
('EXCHANGE', 'DEFECTIVE', 'SELLER', 0, 1, NOW(6), NOW(6)),
('EXCHANGE', 'WRONG_ITEM', 'SELLER', 0, 1, NOW(6), NOW(6)),
('EXCHANGE', 'SIZE_MISMATCH', 'BUYER', 6000, 1, NOW(6), NOW(6)),
('EXCHANGE', 'CHANGE_OF_MIND', 'BUYER', 6000, 1, NOW(6), NOW(6));
