-- Transactional Outbox
CREATE TABLE IF NOT EXISTS outbox_event (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Outbox PK',
    aggregate_type  VARCHAR(100)    NOT NULL                 COMMENT '집합체 타입',
    aggregate_id    VARCHAR(100)    NOT NULL                 COMMENT '집합체 ID',
    event_type      VARCHAR(100)    NOT NULL                 COMMENT '이벤트 타입',
    payload         TEXT            NOT NULL                 COMMENT '이벤트 페이로드 (JSON)',
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING' COMMENT '발행 상태',
    created_at      DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    published_at    DATETIME(6)     NULL                     COMMENT '발행일시',
    retry_count     INT             NOT NULL DEFAULT 0       COMMENT '재시도 횟수',
    PRIMARY KEY (id),
    INDEX idx_outbox_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Transactional Outbox';

-- 멱등성 보장
CREATE TABLE IF NOT EXISTS processed_event (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'PK',
    event_id        VARCHAR(100)    NOT NULL                 COMMENT '이벤트 ID',
    consumer_group  VARCHAR(100)    NOT NULL                 COMMENT '컨슈머 그룹',
    processed_at    DATETIME(6)     NOT NULL                 COMMENT '처리일시',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_processed_unique (event_id, consumer_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='멱등성 보장';
