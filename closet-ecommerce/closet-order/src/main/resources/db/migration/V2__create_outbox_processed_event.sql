-- Outbox Event
CREATE TABLE outbox_event (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Outbox 이벤트 ID',
    aggregate_type  VARCHAR(100)    NOT NULL                 COMMENT '애그리거트 타입',
    aggregate_id    VARCHAR(100)    NOT NULL                 COMMENT '애그리거트 ID',
    event_type      VARCHAR(100)    NOT NULL                 COMMENT '이벤트 타입',
    topic           VARCHAR(200)    NOT NULL                 COMMENT 'Kafka 토픽',
    partition_key   VARCHAR(100)    NOT NULL                 COMMENT '파티션 키',
    payload         TEXT            NOT NULL                 COMMENT '이벤트 페이로드 (JSON)',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '발행 상태 (PENDING/PUBLISHED/FAILED)',
    created_at      DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    published_at    DATETIME(6)     NULL                     COMMENT '발행일시',
    PRIMARY KEY (id),
    INDEX idx_outbox_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Outbox 이벤트';

-- Processed Event (멱등성 보장)
CREATE TABLE processed_event (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '처리된 이벤트 ID',
    event_id        VARCHAR(100)    NOT NULL                 COMMENT '이벤트 고유 식별자',
    topic           VARCHAR(200)    NOT NULL                 COMMENT 'Kafka 토픽',
    consumer_group  VARCHAR(100)    NOT NULL                 COMMENT 'Consumer Group',
    processed_at    DATETIME(6)     NOT NULL                 COMMENT '처리일시',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_processed_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='처리된 이벤트 (멱등성)';
