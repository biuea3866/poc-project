-- 소규모 테스트용 데이터 시딩 스크립트
-- 실행 시간: 약 2-5분 소요
-- 용도: 빠른 테스트 및 검증용

USE partition_test;

SET autocommit = 0;

DELIMITER $$

-- 1. 파티셔닝 미적용 상품 테이블 (10만건)
DROP PROCEDURE IF EXISTS seed_product_small$$
CREATE PROCEDURE seed_product_small()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 5000;
    DECLARE total_records INT DEFAULT 100000;
    DECLARE year_val INT;
    DECLARE month_val INT;
    DECLARE day_val INT;

    SET @categories = 'Electronics,Books,Clothing,Food,Toys,Furniture,Sports,Beauty,Home,Garden';

    WHILE i <= total_records DO
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

        IF i % batch_size = 0 THEN
            COMMIT;
        END IF;
    END WHILE;

    COMMIT;
    SELECT '소규모 product 테이블 시딩 완료! (100,000건)' AS status;
END$$

-- 2. 파티셔닝 적용 상품 테이블 (10만건)
DROP PROCEDURE IF EXISTS seed_product_partitioned_small$$
CREATE PROCEDURE seed_product_partitioned_small()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 5000;
    DECLARE total_records INT DEFAULT 100000;
    DECLARE year_val INT;
    DECLARE month_val INT;
    DECLARE day_val INT;

    SET @categories = 'Electronics,Books,Clothing,Food,Toys,Furniture,Sports,Beauty,Home,Garden';

    WHILE i <= total_records DO
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

        IF i % batch_size = 0 THEN
            COMMIT;
        END IF;
    END WHILE;

    COMMIT;
    SELECT '소규모 product_partitioned 테이블 시딩 완료! (100,000건)' AS status;
END$$

-- 3. 댓글 테이블 (20만건)
DROP PROCEDURE IF EXISTS seed_comment_small$$
CREATE PROCEDURE seed_comment_small()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 5000;
    DECLARE total_records INT DEFAULT 200000;
    DECLARE year_val INT;
    DECLARE month_val INT;
    DECLARE day_val INT;
    DECLARE product_id_val BIGINT;

    WHILE i <= total_records DO
        SET year_val = 2020 + FLOOR(RAND() * 6);
        SET month_val = 1 + FLOOR(RAND() * 12);
        SET day_val = 1 + FLOOR(RAND() * 28);
        SET product_id_val = 1 + FLOOR(RAND() * 100000);

        INSERT INTO comment (product_id, user_name, content, rating, created_date)
        VALUES (
            product_id_val,
            CONCAT('User-', FLOOR(RAND() * 10000)),
            CONCAT('Comment content ', i, ' - This is a review for the product'),
            1 + FLOOR(RAND() * 5),
            DATE(CONCAT(year_val, '-', LPAD(month_val, 2, '0'), '-', LPAD(day_val, 2, '0')))
        );

        SET i = i + 1;

        IF i % batch_size = 0 THEN
            COMMIT;
        END IF;
    END WHILE;

    COMMIT;
    SELECT '소규모 comment 테이블 시딩 완료! (200,000건)' AS status;
END$$

DELIMITER ;

-- 소규모 데이터 자동 실행 (테스트용)
-- 주석 해제하여 사용
-- CALL seed_product_small();
-- CALL seed_product_partitioned_small();
-- CALL seed_comment_small();
