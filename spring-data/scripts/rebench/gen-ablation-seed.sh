#!/bin/bash
# 날짜 인덱스 대조 실험용 시드. 사용법: gen-ablation-seed.sh <상품 행수>
# product 를 채운 뒤 나머지 두 테이블에 그대로 복제한다. 세 테이블의 행이 같아야
# 인덱스와 파티셔닝 외의 변수가 남지 않는다.
set -euo pipefail
ROWS=$1
CHUNK=1000000

cat <<EOF
SET autocommit = 1;
SET SESSION unique_checks = 0;

DROP FUNCTION IF EXISTS f_year;
DELIMITER \$\$
CREATE FUNCTION f_year(x BIGINT) RETURNS INT DETERMINISTIC
BEGIN
  RETURN 2020 + LEAST(5, FLOOR((x - 1) / $((ROWS / 6 + 1))));
END\$\$
DELIMITER ;

DROP TABLE IF EXISTS seq;
CREATE TABLE seq (n BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO seq (n) VALUES (1);
EOF

p=1
while [ $p -lt $ROWS ]; do
  echo "INSERT INTO seq (n) SELECT n + $p FROM seq WHERE n + $p <= $ROWS;"
  p=$((p * 2))
done

start=1
while [ $start -le $ROWS ]; do
  end=$((start + CHUNK - 1)); [ $end -gt $ROWS ] && end=$ROWS
  cat <<EOF
INSERT INTO product (id, name, price, category, description, stock_quantity, created_date)
SELECT n, CONCAT('Product-', n), ROUND(10 + (n % 990), 2),
       ELT(1 + (n % 10), 'Electronics','Books','Clothing','Food','Toys','Furniture','Sports','Beauty','Home','Garden'),
       CONCAT('Description for product ', n), n % 1000,
       MAKEDATE(f_year(n), ((n - 1) % 365) + 1)
FROM seq WHERE n BETWEEN $start AND $end;
EOF
  start=$((end + 1))
done

start=1
while [ $start -le $ROWS ]; do
  end=$((start + CHUNK - 1)); [ $end -gt $ROWS ] && end=$ROWS
  for t in product_noidx product_partitioned_noidx; do
    echo "INSERT INTO $t SELECT * FROM product WHERE id BETWEEN $start AND $end;"
  done
  start=$((end + 1))
done

cat <<EOF
DROP TABLE seq;
DROP FUNCTION f_year;
ANALYZE TABLE product, product_noidx, product_partitioned_noidx;
EOF
