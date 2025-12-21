-- 데이터베이스 선택
USE waiting_number_db;

-- 상품 대기번호 카운터 테이블 (ProductWaiting)
CREATE TABLE IF NOT EXISTS product_waiting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    curr_no INT NOT NULL DEFAULT 1,
    product_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상품 테이블
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 대기번호 카운터 테이블 (비관적/낙관적 락 모두 사용)
CREATE TABLE IF NOT EXISTS waiting_number_counters (
    product_id BIGINT PRIMARY KEY,
    current_number BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 대기번호 히스토리 테이블 (선택사항 - 감사 목적)
CREATE TABLE IF NOT EXISTS waiting_numbers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    waiting_number BIGINT NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_user_id (user_id),
    UNIQUE INDEX idx_product_waiting (product_id, waiting_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 테스트용 상품 데이터
INSERT INTO products (name, description, is_active) VALUES
('상품 A', '인기 상품 A - 대량 구매 가능', TRUE),
('상품 B', '한정 판매 상품 B - 선착순 100명', TRUE),
('상품 C', '프리미엄 상품 C - VIP 전용', TRUE);

-- 초기 카운터 데이터 (각 상품별 대기번호 카운터)
INSERT INTO waiting_number_counters (product_id, current_number, version) VALUES
(1, 0, 0),
(2, 0, 0),
(3, 0, 0);
