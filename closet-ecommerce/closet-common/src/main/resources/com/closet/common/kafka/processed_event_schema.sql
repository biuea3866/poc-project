-- 처리된 이벤트 (멱등성) 테이블
-- 각 서비스의 Flyway 마이그레이션에서 이 SQL을 참고하여 적용한다.
CREATE TABLE IF NOT EXISTS processed_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(100) NOT NULL COMMENT '이벤트 ID (UUID)',
    consumer_group VARCHAR(100) NOT NULL COMMENT '컨슈머 그룹',
    processed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_processed_event_unique (event_id, consumer_group)
) COMMENT '처리된 이벤트 (멱등성)';
