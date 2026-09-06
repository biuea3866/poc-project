-- 날짜 인덱스 유무 대조 실험용 스키마.
--
-- 재검증 본 실험은 세 테이블에 모두 idx_created_date 를 걸어 "파티셔닝이냐 인덱스냐" 를 물었다.
-- 여기서는 파티션 테이블에 날짜 인덱스를 걸지 않는다. 파티션 키로 이미 연 단위 국소화가 된
-- 테이블에 같은 컬럼 인덱스를 또 거는 구성은 재지 않고, PK 와 프루닝만으로 버티게 한다.
-- 세 테이블은 완전히 같은 행을 담는다. 댓글은 이 실험에 쓰지 않아 만들지 않는다.

-- 1. 비파티션 + 날짜 인덱스 (본 실험의 대조군)
CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_date DATE NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_created_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 비파티션, 날짜 인덱스 없음 — PK(id) 만 있는 상태
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

-- 3. 파티션, 날짜 인덱스 없음 — 프루닝만으로 버티는 구성
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
