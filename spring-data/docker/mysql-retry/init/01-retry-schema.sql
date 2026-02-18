CREATE DATABASE IF NOT EXISTS kafka_retry;
USE kafka_retry;

CREATE TABLE IF NOT EXISTS failed_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255),
    payload TEXT NOT NULL,
    error_message VARCHAR(1000) NOT NULL,
    stack_trace TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_retried_at TIMESTAMP(6),
    resolved_at TIMESTAMP(6),
    INDEX idx_status_retry (status, retry_count),
    INDEX idx_topic_status (topic, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    level VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    detail TEXT,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_acknowledged (acknowledged),
    INDEX idx_topic (topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
