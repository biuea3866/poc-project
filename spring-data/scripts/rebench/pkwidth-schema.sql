-- PK 폭과 파티셔닝을 분리하는 대조 실험. 세 테이블 모두 날짜 인덱스가 없다.
--   비파티션 PK(id)                → 기준
--   비파티션 PK(id, created_date)  → PK 폭만 다름
--   파티션   PK(id, created_date)  → 파티셔닝만 다름
-- 파티션 테이블은 파티션 키를 PK 에 포함해야 하므로, 그 비용을 파티셔닝 효과와 섞지 않으려면
-- 같은 복합 PK 를 가진 비파티션 테이블이 중간에 있어야 한다.

CREATE TABLE product_noidx (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_date DATE NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_composite_noidx (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_date DATE NOT NULL,
    PRIMARY KEY (id, created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_partitioned_noidx (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_date DATE NOT NULL,
    PRIMARY KEY (id, created_date)
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
