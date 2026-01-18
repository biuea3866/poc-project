-- Database creation (already created by docker-compose)
USE partition_test;

-- 1. 파티셔닝 미적용 상품 테이블
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_created_date (created_date),
    INDEX idx_category (category),
    INDEX idx_price (price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 파티셔닝 적용 상품 테이블 (RANGE 파티셔닝 - 연도별)
CREATE TABLE IF NOT EXISTS product_partitioned (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_date),
    INDEX idx_category (category),
    INDEX idx_price (price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (YEAR(created_date)) (
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 3. 파티셔닝 적용 댓글 테이블 (RANGE 파티셔닝 - 연도별)
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_date),
    INDEX idx_product_id (product_id),
    INDEX idx_rating (rating),
    INDEX idx_product_created (product_id, created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (YEAR(created_date)) (
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 파티션 정보 확인 쿼리 (참고용)
-- SELECT
--     TABLE_NAME,
--     PARTITION_NAME,
--     PARTITION_METHOD,
--     PARTITION_EXPRESSION,
--     TABLE_ROWS
-- FROM INFORMATION_SCHEMA.PARTITIONS
-- WHERE TABLE_SCHEMA = 'partition_test'
--   AND TABLE_NAME IN ('product_partitioned', 'comment')
-- ORDER BY TABLE_NAME, PARTITION_ORDINAL_POSITION;
