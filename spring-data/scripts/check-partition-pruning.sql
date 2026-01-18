-- 파티셔닝 프루닝 확인 쿼리
-- EXPLAIN을 통해 실제로 어떤 파티션이 스캔되는지 확인

USE partition_test;

-- ====================================
-- 1. 파티션 정보 조회
-- ====================================
SELECT
    TABLE_NAME,
    PARTITION_NAME,
    PARTITION_METHOD,
    PARTITION_EXPRESSION,
    TABLE_ROWS,
    AVG_ROW_LENGTH,
    DATA_LENGTH / 1024 / 1024 AS DATA_MB
FROM INFORMATION_SCHEMA.PARTITIONS
WHERE TABLE_SCHEMA = 'partition_test'
  AND TABLE_NAME IN ('product_partitioned', 'comment')
ORDER BY TABLE_NAME, PARTITION_ORDINAL_POSITION;

-- ====================================
-- 2. 파티셔닝 미적용 vs 적용 비교
-- ====================================

-- 2-1. 파티셔닝 미적용 (전체 스캔)
EXPLAIN
SELECT * FROM product
WHERE created_date BETWEEN '2024-01-01' AND '2024-12-31'
LIMIT 100;

-- 2-2. 파티셔닝 적용 (파티션 프루닝)
EXPLAIN
SELECT * FROM product_partitioned
WHERE created_date BETWEEN '2024-01-01' AND '2024-12-31'
LIMIT 100;

-- ====================================
-- 3. 파티션 키 포함 여부에 따른 차이
-- ====================================

-- 3-1. 파티션 키 미포함 (모든 파티션 스캔)
EXPLAIN
SELECT c.*
FROM comment c
WHERE c.product_id = 12345;

-- 3-2. 파티션 키 포함 (특정 파티션만 스캔)
EXPLAIN
SELECT c.*
FROM comment c
WHERE c.product_id = 12345
  AND c.created_date BETWEEN '2024-01-01' AND '2024-12-31';

-- ====================================
-- 4. JOIN 시 파티션 프루닝
-- ====================================

-- 4-1. JOIN - 파티션 키 미포함
-- 댓글 테이블의 모든 파티션 스캔
EXPLAIN
SELECT p.id, p.name, c.content, c.rating
FROM product_partitioned p
LEFT JOIN comment c ON p.id = c.product_id
WHERE p.created_date BETWEEN '2024-01-01' AND '2024-12-31'
LIMIT 10;

-- 4-2. JOIN - 파티션 키 포함
-- 양쪽 테이블 모두 특정 파티션만 스캔
EXPLAIN
SELECT p.id, p.name, c.content, c.rating
FROM product_partitioned p
LEFT JOIN comment c ON p.id = c.product_id
    AND c.created_date BETWEEN '2024-01-01' AND '2024-12-31'
WHERE p.created_date BETWEEN '2024-01-01' AND '2024-12-31'
LIMIT 10;

-- ====================================
-- 5. 상세 EXPLAIN (JSON 형식)
-- ====================================

-- 5-1. 파티션 키 없음
EXPLAIN FORMAT=JSON
SELECT c.*
FROM comment c
WHERE c.product_id = 12345;

-- 5-2. 파티션 키 있음
EXPLAIN FORMAT=JSON
SELECT c.*
FROM comment c
WHERE c.product_id = 12345
  AND c.created_date BETWEEN '2024-01-01' AND '2024-12-31';

-- ====================================
-- 6. 실행 통계 비교
-- ====================================

-- 쿼리 실행 전 프로파일링 활성화
SET profiling = 1;

-- 6-1. 파티션 키 미포함
SELECT COUNT(*) FROM comment WHERE product_id IN (1, 2, 3, 4, 5);

-- 6-2. 파티션 키 포함
SELECT COUNT(*) FROM comment
WHERE product_id IN (1, 2, 3, 4, 5)
  AND created_date BETWEEN '2024-01-01' AND '2024-12-31';

-- 프로파일 결과 확인
SHOW PROFILES;

-- 상세 프로파일
SHOW PROFILE FOR QUERY 1;  -- 파티션 키 미포함
SHOW PROFILE FOR QUERY 2;  -- 파티션 키 포함

SET profiling = 0;

-- ====================================
-- 7. 파티션별 통계
-- ====================================

-- 파티션별 데이터 건수
SELECT
    PARTITION_NAME,
    TABLE_ROWS,
    AVG_ROW_LENGTH,
    DATA_LENGTH / 1024 / 1024 AS DATA_MB,
    INDEX_LENGTH / 1024 / 1024 AS INDEX_MB
FROM INFORMATION_SCHEMA.PARTITIONS
WHERE TABLE_SCHEMA = 'partition_test'
  AND TABLE_NAME = 'product_partitioned'
ORDER BY PARTITION_ORDINAL_POSITION;

-- 댓글 파티션별 통계
SELECT
    PARTITION_NAME,
    TABLE_ROWS,
    DATA_LENGTH / 1024 / 1024 AS DATA_MB
FROM INFORMATION_SCHEMA.PARTITIONS
WHERE TABLE_SCHEMA = 'partition_test'
  AND TABLE_NAME = 'comment'
ORDER BY PARTITION_ORDINAL_POSITION;
