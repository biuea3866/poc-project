#!/bin/bash
# 데이터셋별 시드 SQL 을 생성한다. 사용법: gen-seed.sh <uniform|skewed> <상품 행수> <댓글 행수>
set -euo pipefail

DIST=$1
ROWS=$2
COMMENTS=$3
CHUNK=1000000

# 연도 산출식. id 와 함께 연도가 증가하도록 구간을 나눈다 (append-only 시계열 모양).
if [ "$DIST" = "uniform" ]; then
  # 2020~2025 여섯 해에 균등 분배
  YEAR_BODY="RETURN 2020 + LEAST(5, FLOOR((x - 1) / $((ROWS / 6 + 1))));"
else
  # 2024 에 90% 집중, 나머지 10% 를 다섯 해에 분배
  MINOR=$((ROWS / 50))          # 한 해당 2%
  Y2024_END=$((ROWS - MINOR))   # 2025 로 넘어가기 직전
  YEAR_BODY="RETURN CASE
      WHEN x <= $((MINOR * 1)) THEN 2020
      WHEN x <= $((MINOR * 2)) THEN 2021
      WHEN x <= $((MINOR * 3)) THEN 2022
      WHEN x <= $((MINOR * 4)) THEN 2023
      WHEN x <= $Y2024_END     THEN 2024
      ELSE 2025 END;"
fi

cat <<EOF
SET autocommit = 1;
SET SESSION unique_checks = 0;
SET SESSION foreign_key_checks = 0;

-- 연도 산출 함수. 상품과 댓글이 같은 식을 쓰게 해 댓글 날짜를 상품과 같은 해로 맞춘다.
DROP FUNCTION IF EXISTS f_year;
DELIMITER \$\$
CREATE FUNCTION f_year(x BIGINT) RETURNS INT DETERMINISTIC
BEGIN
  $YEAR_BODY
END\$\$
DELIMITER ;

-- 1) 순번 테이블
DROP TABLE IF EXISTS seq;
CREATE TABLE seq (n BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO seq (n) VALUES (1);
EOF

# 배가로 순번 채우기
p=1
while [ $p -lt $COMMENTS ]; do
  echo "INSERT INTO seq (n) SELECT n + $p FROM seq WHERE n + $p <= $COMMENTS;"
  p=$((p * 2))
done

echo "SELECT CONCAT('seq rows: ', COUNT(*)) FROM seq;"

# 2) 상품 시드 (청크 분할 — 단일 거대 트랜잭션의 undo 폭증을 피한다)
start=1
while [ $start -le $ROWS ]; do
  end=$((start + CHUNK - 1))
  [ $end -gt $ROWS ] && end=$ROWS
  cat <<EOF
INSERT INTO product (id, name, price, category, description, stock_quantity, created_date)
SELECT n,
       CONCAT('Product-', n),
       ROUND(10 + (n % 990), 2),
       ELT(1 + (n % 10), 'Electronics','Books','Clothing','Food','Toys','Furniture','Sports','Beauty','Home','Garden'),
       CONCAT('Description for product ', n),
       n % 1000,
       MAKEDATE(f_year(n), ((n - 1) % 365) + 1)
FROM seq WHERE n BETWEEN $start AND $end;
EOF
  start=$((end + 1))
done

# 3) 나머지 두 상품 테이블은 product 를 그대로 복제한다 (행 값 완전 동일)
start=1
while [ $start -le $ROWS ]; do
  end=$((start + CHUNK - 1))
  [ $end -gt $ROWS ] && end=$ROWS
  echo "INSERT INTO product_composite_pk SELECT * FROM product WHERE id BETWEEN $start AND $end;"
  echo "INSERT INTO product_partitioned SELECT * FROM product WHERE id BETWEEN $start AND $end;"
  start=$((end + 1))
done

# 4) 댓글 시드. product_id 는 결정적 의사난수로 흩고, created_date 는 그 상품과 같은 해로 맞춘다.
start=1
while [ $start -le $COMMENTS ]; do
  end=$((start + CHUNK - 1))
  [ $end -gt $COMMENTS ] && end=$COMMENTS
  cat <<EOF
INSERT INTO comment (id, product_id, user_name, content, rating, created_date)
SELECT n,
       1 + ((n * 7919) % $ROWS),
       CONCAT('User-', n % 100000),
       CONCAT('Comment content ', n, ' - This is a review for the product'),
       1 + (n % 5),
       MAKEDATE(f_year(1 + ((n * 7919) % $ROWS)), ((n - 1) % 365) + 1)
FROM seq WHERE n BETWEEN $start AND $end;
EOF
  start=$((end + 1))
done

cat <<EOF
DROP TABLE seq;
DROP FUNCTION f_year;
ANALYZE TABLE product, product_composite_pk, product_partitioned, comment;
EOF
