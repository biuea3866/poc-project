-- 재검증용 스키마. `:db` 를 호출측에서 치환해 partition_uniform / partition_skewed 두 벌을 만든다.
--
-- 기존 스키마와의 차이:
--   1. product_partitioned 에 idx_created_date 추가 → product 와 인덱스 구조 동일
--   2. product_composite_pk 추가 → PK 폭 변화와 파티셔닝 효과를 분리하기 위한 중간 대조군
--   3. 세 상품 테이블은 완전히 동일한 행을 담는다 (시드 단계에서 복제)

-- 1. 대조군: 일반 테이블, PK(id)
CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_date DATE NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_created_date (created_date),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 중간 대조군: 파티셔닝 없이 PK 만 (id, created_date) 로 넓힌 테이블
--    파티션 테이블은 파티션 키를 PK 에 포함해야 하므로, 그 비용을 분리해 측정한다.
CREATE TABLE product_composite_pk (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_date DATE NOT NULL,
    PRIMARY KEY (id, created_date),
    INDEX idx_created_date (created_date),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 실험군: 위와 동일한 구조 + RANGE 파티셔닝
CREATE TABLE product_partitioned (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_date DATE NOT NULL,
    PRIMARY KEY (id, created_date),
    INDEX idx_created_date (created_date),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (YEAR(created_date)) (
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 4. 댓글: 파티셔닝 적용. created_date 는 시드 단계에서 상품과 같은 해로 맞춘다.
CREATE TABLE comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    rating INT NOT NULL,
    created_date DATE NOT NULL,
    PRIMARY KEY (id, created_date),
    INDEX idx_product_created (product_id, created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (YEAR(created_date)) (
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
