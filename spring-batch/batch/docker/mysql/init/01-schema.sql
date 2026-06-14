-- 비즈니스 테이블. Spring Batch 메타테이블(BATCH_*)은 앱이 initialize-schema=always 로 생성한다.

CREATE TABLE IF NOT EXISTS reserved_notification (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    notification_type VARCHAR(32)  NOT NULL,   -- POST_COMMENT / KEYWORD_INTEREST / TICKET_REMINDER
    group_key         VARCHAR(128) NOT NULL,   -- postId / keyword / ticketId
    payload           VARCHAR(512) NOT NULL,
    sent              TINYINT(1)   NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    -- GROUP BY (user_id, notification_type, group_key) + sent 필터 + 파티셔닝(user_id 범위) 가속
    KEY idx_group_send (user_id, notification_type, group_key, sent)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
