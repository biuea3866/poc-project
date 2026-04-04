-- Restock Notification (재입고 알림 신청)
CREATE TABLE restock_notification (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '알림 ID',
    product_option_id   BIGINT          NOT NULL                 COMMENT '상품 옵션 ID',
    member_id           BIGINT          NOT NULL                 COMMENT '회원 ID',
    status              VARCHAR(20)     NOT NULL                 COMMENT '상태 (WAITING/NOTIFIED/EXPIRED)',
    expired_at          DATETIME(6)     NOT NULL                 COMMENT '만료일시',
    notified_at         DATETIME(6)     NULL                     COMMENT '알림 발송일시',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_restock_option_member (product_option_id, member_id),
    INDEX idx_restock_status (product_option_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='재입고 알림 신청';
