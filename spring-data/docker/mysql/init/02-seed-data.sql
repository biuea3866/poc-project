-- 대용량 데이터 시딩 스크립트
-- 실행 시간: 약 30-60분 소요 예상

USE partition_test;

-- 자동 커밋 비활성화 (성능 향상)
SET autocommit = 0;

-- 로그 레벨 조정
SET GLOBAL innodb_flush_log_at_trx_commit = 2;

DELIMITER $$

-- 1. 파티셔닝 미적용 상품 테이블 시딩 프로시저 (1,000만건)
DROP PROCEDURE IF EXISTS seed_product$$
CREATE PROCEDURE seed_product()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_records INT DEFAULT 10000000;
    DECLARE year_val INT;
    DECLARE month_val INT;
    DECLARE day_val INT;

    SET @categories = 'Electronics,Books,Clothing,Food,Toys,Furniture,Sports,Beauty,Home,Garden';

    WHILE i <= total_records DO
        -- 2020-2025년 사이 랜덤 날짜 생성
        SET year_val = 2020 + FLOOR(RAND() * 6);
        SET month_val = 1 + FLOOR(RAND() * 12);
        SET day_val = 1 + FLOOR(RAND() * 28);

        INSERT INTO product (name, price, category, description, stock_quantity, created_date)
        VALUES (
            CONCAT('Product-', i),
            ROUND(10 + (RAND() * 990), 2),
            SUBSTRING_INDEX(SUBSTRING_INDEX(@categories, ',', 1 + FLOOR(RAND() * 10)), ',', -1),
            CONCAT('Description for product ', i),
            FLOOR(RAND() * 1000),
            DATE(CONCAT(year_val, '-', LPAD(month_val, 2, '0'), '-', LPAD(day_val, 2, '0')))
        );

        SET i = i + 1;

        -- 배치마다 커밋
        IF i % batch_size = 0 THEN
            COMMIT;
            SELECT CONCAT('product: ', i, ' / ', total_records, ' (', ROUND(i/total_records*100, 2), '%)') AS progress;
        END IF;
    END WHILE;

    COMMIT;
    SELECT 'product 테이블 시딩 완료!' AS status;
END$$

-- 2. 파티셔닝 적용 상품 테이블 시딩 프로시저 (1,000만건)
DROP PROCEDURE IF EXISTS seed_product_partitioned$$
CREATE PROCEDURE seed_product_partitioned()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_records INT DEFAULT 10000000;
    DECLARE year_val INT;
    DECLARE month_val INT;
    DECLARE day_val INT;

    SET @categories = 'Electronics,Books,Clothing,Food,Toys,Furniture,Sports,Beauty,Home,Garden';

    WHILE i <= total_records DO
        -- 2020-2025년 사이 랜덤 날짜 생성 (파티션에 골고루 분산)
        SET year_val = 2020 + FLOOR(RAND() * 6);
        SET month_val = 1 + FLOOR(RAND() * 12);
        SET day_val = 1 + FLOOR(RAND() * 28);

        INSERT INTO product_partitioned (name, price, category, description, stock_quantity, created_date)
        VALUES (
            CONCAT('Product-P-', i),
            ROUND(10 + (RAND() * 990), 2),
            SUBSTRING_INDEX(SUBSTRING_INDEX(@categories, ',', 1 + FLOOR(RAND() * 10)), ',', -1),
            CONCAT('Description for partitioned product ', i),
            FLOOR(RAND() * 1000),
            DATE(CONCAT(year_val, '-', LPAD(month_val, 2, '0'), '-', LPAD(day_val, 2, '0')))
        );

        SET i = i + 1;

        -- 배치마다 커밋
        IF i % batch_size = 0 THEN
            COMMIT;
            SELECT CONCAT('product_partitioned: ', i, ' / ', total_records, ' (', ROUND(i/total_records*100, 2), '%)') AS progress;
        END IF;
    END WHILE;

    COMMIT;
    SELECT 'product_partitioned 테이블 시딩 완료!' AS status;
END$$

-- 3. 댓글 테이블 시딩 프로시저 (2,000만건)
DROP PROCEDURE IF EXISTS seed_comment$$
CREATE PROCEDURE seed_comment()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_records INT DEFAULT 20000000;
    DECLARE year_val INT;
    DECLARE month_val INT;
    DECLARE day_val INT;
    DECLARE product_id_val BIGINT;

    WHILE i <= total_records DO
        -- 2020-2025년 사이 랜덤 날짜 생성
        SET year_val = 2020 + FLOOR(RAND() * 6);
        SET month_val = 1 + FLOOR(RAND() * 12);
        SET day_val = 1 + FLOOR(RAND() * 28);

        -- 상품 ID는 1~1000만 사이 랜덤
        SET product_id_val = 1 + FLOOR(RAND() * 10000000);

        INSERT INTO comment (product_id, user_name, content, rating, created_date)
        VALUES (
            product_id_val,
            CONCAT('User-', FLOOR(RAND() * 100000)),
            CONCAT('Comment content ', i, ' - This is a review for the product'),
            1 + FLOOR(RAND() * 5),
            DATE(CONCAT(year_val, '-', LPAD(month_val, 2, '0'), '-', LPAD(day_val, 2, '0')))
        );

        SET i = i + 1;

        -- 배치마다 커밋
        IF i % batch_size = 0 THEN
            COMMIT;
            SELECT CONCAT('comment: ', i, ' / ', total_records, ' (', ROUND(i/total_records*100, 2), '%)') AS progress;
        END IF;
    END WHILE;

    COMMIT;
    SELECT 'comment 테이블 시딩 완료!' AS status;
END$$

DELIMITER ;

-- 프로시저 실행은 수동으로 진행
-- 자동 실행하지 않음 (너무 오래 걸리므로)
--
-- 실행 방법:
-- CALL seed_product();
-- CALL seed_product_partitioned();
-- CALL seed_comment();
