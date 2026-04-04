-- Processed Event (Kafka 이벤트 멱등성 보장)
CREATE TABLE processed_event (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '처리 이벤트 ID',
    event_id        VARCHAR(100)    NOT NULL                 COMMENT '이벤트 고유 식별자',
    topic           VARCHAR(200)    NOT NULL                 COMMENT 'Kafka 토픽명',
    consumer_group  VARCHAR(100)    NOT NULL                 COMMENT 'Consumer Group 이름',
    processed_at    DATETIME(6)     NOT NULL                 COMMENT '처리 일시',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_processed_event_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='처리 완료 이벤트 (멱등성)';
