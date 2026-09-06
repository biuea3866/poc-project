#!/bin/bash
# 각 실험이 실제로 어떤 플랜을 탔는지 기록한다. 사용법: capture-explain.sh <uniform|skewed>
#
# 측정값만으로는 "왜" 를 말할 수 없다. partitions 컬럼과 접근 방식이 비교군 사이에서
# 의도한 것만 다른지 여기서 확인한다.
set -uo pipefail

DATASET=$1
DIR="$(cd "$(dirname "$0")" && pwd)"
DB="partition_$DATASET"
OUT="$DIR/results/$DATASET/explain.md"
mkdir -p "$(dirname "$OUT")"

q() { docker exec -i mysql-partition-rebench mysql -uroot -proot -t "$DB" -e "$1" 2>/dev/null; }
qn() { docker exec -i mysql-partition-rebench mysql -uroot -proot -N -B "$DB" -e "$1" 2>/dev/null; }

Y_MIN=$(qn "SELECT MIN(id) FROM product WHERE created_date BETWEEN '2024-01-01' AND '2024-12-31'")
RANGE="created_date BETWEEN '2024-01-01' AND '2024-12-31'"
JOIN_BASE="SELECT p.id, p.name, c.id AS comment_id, c.rating FROM product_partitioned p JOIN comment c ON c.product_id = p.id WHERE p.$RANGE AND p.id BETWEEN $Y_MIN AND $((Y_MIN + 1000))"
JOIN_TAIL="ORDER BY p.id, c.id LIMIT 100"

{
  echo "# 쿼리 플랜 — $DATASET"
  echo
  echo "생성: $(date '+%Y-%m-%d %H:%M:%S')"
  echo

  echo "## 데이터 분포"
  echo
  echo '```'
  q "SELECT YEAR(created_date) AS y, COUNT(*) AS rows_cnt FROM product GROUP BY y ORDER BY y;"
  echo '```'
  echo
  echo "세 상품 테이블의 행이 동일한지, 댓글 연도가 상품 연도와 일치하는지 확인한다."
  echo
  echo '```'
  q "SELECT
       (SELECT COUNT(*) FROM (SELECT * FROM product EXCEPT SELECT * FROM product_partitioned) a) AS plain_minus_partitioned,
       (SELECT COUNT(*) FROM (SELECT * FROM product EXCEPT SELECT * FROM product_composite_pk) b) AS plain_minus_composite;"
  q "SELECT COUNT(*) AS comment_year_mismatch FROM comment c JOIN product p ON p.id = c.product_id WHERE YEAR(c.created_date) <> YEAR(p.created_date);"
  echo '```'
  echo
  echo "## 테이블 크기"
  echo
  echo '```'
  q "SELECT table_name, ROUND((data_length+index_length)/1024/1024) AS total_mb FROM information_schema.tables WHERE table_schema='$DB' ORDER BY total_mb DESC;"
  echo '```'
  echo

  echo "## 실험 A — 범위 조회, ORDER BY created_date, id"
  for t in product product_composite_pk product_partitioned; do
    echo; echo "### $t"; echo '```'
    q "EXPLAIN SELECT id, name, created_date FROM $t WHERE $RANGE ORDER BY created_date, id LIMIT 100;"
    echo '```'
  done
  echo

  echo "## 실험 A' — 같은 조회, ORDER BY id"
  for t in product product_composite_pk product_partitioned; do
    echo; echo "### $t"; echo '```'
    q "EXPLAIN SELECT id, name, created_date FROM $t WHERE $RANGE ORDER BY id LIMIT 100;"
    echo '```'
  done
  echo

  echo "## 실험 A'' — 한 해 전체 집계"
  for t in product product_composite_pk product_partitioned; do
    echo; echo "### $t"; echo '```'
    q "EXPLAIN SELECT COUNT(*), SUM(price), AVG(stock_quantity) FROM $t WHERE $RANGE;"
    echo '```'
  done
  echo

  echo "## 실험 B — PK 점 조회 (파티션 키 미포함)"
  for t in product product_composite_pk product_partitioned; do
    echo; echo "### $t"; echo '```'
    q "EXPLAIN SELECT id, name, created_date FROM $t WHERE id = 5000000;"
    echo '```'
  done
  echo

  echo "## 실험 C — JOIN"
  echo; echo "### 댓글 날짜 조건 없음"; echo '```'
  q "EXPLAIN $JOIN_BASE $JOIN_TAIL;"
  echo '```'
  echo; echo "### 댓글 날짜 조건 포함"; echo '```'
  q "EXPLAIN $JOIN_BASE AND c.$RANGE $JOIN_TAIL;"
  echo '```'
  echo
  echo "### 두 쿼리의 결과 집합 동일성"
  echo
  echo '```'
  q "WITH c1 AS ($JOIN_BASE $JOIN_TAIL), c2 AS ($JOIN_BASE AND c.$RANGE $JOIN_TAIL)
     SELECT (SELECT COUNT(*) FROM (SELECT * FROM c1 EXCEPT SELECT * FROM c2) a) AS c1_minus_c2,
            (SELECT COUNT(*) FROM (SELECT * FROM c2 EXCEPT SELECT * FROM c1) b) AS c2_minus_c1,
            (SELECT COUNT(*) FROM c1) AS c1_rows,
            (SELECT COUNT(*) FROM c2) AS c2_rows;"
  echo '```'
} > "$OUT"

echo "기록 완료: $OUT"
