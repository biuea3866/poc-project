#!/bin/bash
# 네 테이블의 실행 계획을 기록한다. 인덱스 유무가 접근 방식을 어떻게 바꾸는지가 핵심이다.
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/results/ablation/explain.md"
mkdir -p "$(dirname "$OUT")"
q() { docker exec -i mysql-abl3 mysql -uroot -proot -t partition_ablation -e "$1" 2>/dev/null; }
R="created_date BETWEEN '2024-01-01' AND '2024-12-31'"

{
  echo "# 날짜 인덱스 유무 대조 — 실행 계획"
  echo; echo "생성: $(date '+%Y-%m-%d %H:%M:%S')"; echo
  echo "## 데이터 분포"; echo; echo '```'
  q "SELECT YEAR(created_date) y, COUNT(*) c FROM product GROUP BY y ORDER BY y;"
  echo '```'; echo
  echo "## 세 테이블의 행 동일성 (차집합 모두 0 이어야 함)"; echo; echo '```'
  q "SELECT
       (SELECT COUNT(*) FROM (SELECT * FROM product EXCEPT SELECT * FROM product_noidx) a) AS vs_noidx,
       (SELECT COUNT(*) FROM (SELECT * FROM product EXCEPT SELECT * FROM product_partitioned_noidx) c) AS vs_part_noidx;"
  echo '```'; echo
  echo "## 테이블 크기"; echo; echo '```'
  q "SELECT table_name, ROUND((data_length+index_length)/1024/1024) total_mb, ROUND(index_length/1024/1024) idx_mb
     FROM information_schema.tables WHERE table_schema='partition_ablation' ORDER BY table_name;"
  echo '```'; echo
  echo "## 쿼리 1 — 범위 조회 + LIMIT 100"
  for t in product product_noidx product_partitioned_noidx; do
    echo; echo "### $t"; echo '```'
    q "EXPLAIN SELECT id, name, created_date FROM $t WHERE $R ORDER BY created_date, id LIMIT 100;"
    echo '```'
  done
  echo
  echo "## 쿼리 3 — 한 해 전체 집계"
  for t in product product_noidx product_partitioned_noidx; do
    echo; echo "### $t"; echo '```'
    q "EXPLAIN SELECT COUNT(*), SUM(price), AVG(stock_quantity) FROM $t WHERE $R;"
    echo '```'
  done
} > "$OUT"
echo "기록: $OUT"
