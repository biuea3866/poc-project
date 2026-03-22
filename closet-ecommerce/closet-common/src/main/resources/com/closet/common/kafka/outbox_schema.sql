-- Transactional Outbox 이벤트 테이블
-- 각 서비스의 Flyway 마이그레이션에서 이 SQL을 참고하여 적용한다.
CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    aggregate_type VARCHAR(100) NOT NULL COMMENT 'Aggregate 타입 (Order, Payment 등)',
    aggregate_id VARCHAR(100) NOT NULL COMMENT 'Aggregate ID',
    event_type VARCHAR(100) NOT NULL COMMENT '이벤트 타입 (OrderCreated 등)',
    payload TEXT NOT NULL COMMENT '이벤트 페이로드 (JSON)',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / PUBLISHED / FAILED',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) DEFAULT NULL,
    published_at DATETIME(6) COMMENT '발행 시각',
    retry_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_outbox_status_created (status, created_at)
) COMMENT 'Transactional Outbox 이벤트';
