-- ============================================================
-- Notification Preference & Topic Subscription
-- ============================================================

CREATE TABLE notification_preference (
    id                 BIGINT          NOT NULL AUTO_INCREMENT COMMENT '알림 설정 고유 식별자',
    member_id          BIGINT          NOT NULL COMMENT '회원 ID',
    email_enabled      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '이메일 수신 동의 (1=동의, 0=거부)',
    sms_enabled        TINYINT(1)      NOT NULL DEFAULT 1 COMMENT 'SMS 수신 동의 (1=동의, 0=거부)',
    push_enabled       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '푸시 수신 동의 (1=동의, 0=거부)',
    marketing_enabled  TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '마케팅 알림 동의 (1=동의, 0=거부)',
    night_enabled      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '야간 알림 동의 (1=동의, 0=거부, 21:00~08:00)',
    created_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at         DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='회원별 알림 수신 설정';

CREATE TABLE notification_topic_subscription (
    id                 BIGINT          NOT NULL AUTO_INCREMENT COMMENT '토픽 구독 고유 식별자',
    member_id          BIGINT          NOT NULL COMMENT '회원 ID',
    topic_type         VARCHAR(30)     NOT NULL COMMENT '토픽 유형 (PRODUCT, CATEGORY, BRAND, EVENT)',
    topic_id           BIGINT          NOT NULL COMMENT '토픽 대상 ID (productId, categoryId, brandId, eventId)',
    is_subscribed      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '구독 여부 (1=구독, 0=해제)',
    subscribed_at      DATETIME(6)     NOT NULL COMMENT '구독 일시',
    unsubscribed_at    DATETIME(6)     NULL     COMMENT '구독 해제 일시',
    created_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at         DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품/카테고리/브랜드/이벤트 알림 토픽 구독';

-- 인덱스
CREATE UNIQUE INDEX uk_notification_preference_member ON notification_preference (member_id);
CREATE INDEX idx_topic_sub_member ON notification_topic_subscription (member_id, is_subscribed);
CREATE INDEX idx_topic_sub_topic ON notification_topic_subscription (topic_type, topic_id, is_subscribed);
CREATE INDEX idx_topic_sub_member_topic ON notification_topic_subscription (member_id, topic_type, topic_id);
