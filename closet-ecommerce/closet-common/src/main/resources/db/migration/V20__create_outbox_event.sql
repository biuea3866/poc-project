-- Outbox Event (Transactional Outbox 패턴)
CREATE TABLE outbox_event (
    id             BIGINT        NOT NULL AUTO_INCREMENT  COMMENT 'Outbox 이벤트 ID',
    aggregate_type VARCHAR(100)  NOT NULL                 COMMENT '집계 타입 (예: Order, Product)',
    aggregate_id   VARCHAR(100)  NOT NULL                 COMMENT '집계 ID',
    event_type     VARCHAR(100)  NOT NULL                 COMMENT '이벤트 타입 (예: order.created)',
    topic          VARCHAR(200)  NOT NULL                 COMMENT 'Kafka 토픽',
    partition_key  VARCHAR(100)  NOT NULL                 COMMENT 'Kafka 파티션 키',
    payload        TEXT          NOT NULL                 COMMENT 'JSON 페이로드',
    status         VARCHAR(20)   NOT NULL                 COMMENT '상태 (PENDING, PUBLISHED, FAILED)',
    created_at     DATETIME(6)   NOT NULL                 COMMENT '생성일시',
    published_at   DATETIME(6)   NULL                     COMMENT '발행일시',
    PRIMARY KEY (id),
    INDEX idx_outbox_event_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Outbox 이벤트';
