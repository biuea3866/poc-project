-- Claim Request (반품/교환 접수)
CREATE TABLE claim_request (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '클레임 ID',
    order_id            BIGINT          NOT NULL                 COMMENT '주문 ID',
    order_item_id       BIGINT          NOT NULL                 COMMENT '주문 항목 ID',
    member_id           BIGINT          NOT NULL                 COMMENT '회원 ID',
    claim_type          VARCHAR(30)     NOT NULL                 COMMENT '클레임 유형 (RETURN/EXCHANGE)',
    reason_category     VARCHAR(30)     NOT NULL                 COMMENT '사유 카테고리 (DEFECTIVE/WRONG_ITEM/SIZE_MISMATCH/CHANGE_OF_MIND/DAMAGED_IN_TRANSIT/OTHER)',
    reason_detail       VARCHAR(500)    NULL                     COMMENT '상세 사유',
    status              VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED' COMMENT '상태 (REQUESTED/APPROVED/COMPLETED/REJECTED)',
    refund_amount       DECIMAL(15,2)   NOT NULL DEFAULT 0       COMMENT '환불 금액',
    approved_at         DATETIME(6)     NULL                     COMMENT '승인일시',
    completed_at        DATETIME(6)     NULL                     COMMENT '완료일시',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at          DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_claim_request_order (order_id),
    INDEX idx_claim_request_member (member_id),
    INDEX idx_claim_request_status (status),
    INDEX idx_claim_request_type_status (claim_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='반품/교환 접수';
