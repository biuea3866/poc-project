-- ============================================================
-- Notification Service Database Schema
-- ============================================================

CREATE TABLE notification (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '알림 고유 식별자',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    channel           VARCHAR(30)     NOT NULL COMMENT '알림 채널 (EMAIL, SMS, PUSH)',
    type              VARCHAR(30)     NOT NULL COMMENT '알림 유형 (ORDER, SHIPPING, MARKETING, RESTOCK)',
    title             VARCHAR(200)    NOT NULL COMMENT '알림 제목',
    content           TEXT            NOT NULL COMMENT '알림 내용',
    is_read           TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '읽음 여부 (1=읽음, 0=미읽음)',
    sent_at           DATETIME(6)     NOT NULL COMMENT '발송 일시',
    read_at           DATETIME(6)     NULL     COMMENT '읽음 일시',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='알림';

CREATE TABLE notification_template (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '템플릿 고유 식별자',
    type              VARCHAR(30)     NOT NULL COMMENT '알림 유형 (ORDER, SHIPPING, MARKETING, RESTOCK)',
    channel           VARCHAR(30)     NOT NULL COMMENT '알림 채널 (EMAIL, SMS, PUSH)',
    title_template    VARCHAR(500)    NOT NULL COMMENT '제목 템플릿',
    content_template  TEXT            NOT NULL COMMENT '내용 템플릿',
    is_active         TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부 (1=활성, 0=비활성)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='알림 템플릿';

CREATE TABLE restock_subscription (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '재입고 구독 고유 식별자',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    product_option_id BIGINT          NOT NULL COMMENT '상품 옵션 ID',
    is_notified       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '알림 발송 여부 (1=발송됨, 0=미발송)',
    subscribed_at     DATETIME(6)     NOT NULL COMMENT '구독 일시',
    notified_at       DATETIME(6)     NULL     COMMENT '알림 발송 일시',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='재입고 알림 구독';

-- 인덱스
CREATE INDEX idx_notification_member ON notification (member_id, is_read, created_at DESC);
CREATE INDEX idx_notification_type ON notification (type, channel);
CREATE INDEX idx_template_type_channel ON notification_template (type, channel, is_active);
CREATE INDEX idx_restock_member ON restock_subscription (member_id, is_notified);
CREATE INDEX idx_restock_product_option ON restock_subscription (product_option_id, is_notified);
