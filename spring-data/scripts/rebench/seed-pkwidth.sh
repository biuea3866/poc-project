#!/bin/bash
# 청크 단위 재개 가능 시드. 중간에 컨테이너가 사라져도 이미 넣은 청크는 건너뛴다.
set -uo pipefail
source "$(cd "$(dirname "$0")" && pwd)/pkwidth-lib.sh"
ROWS=${1:-10000000}
CHUNK=1000000
SCHEMA="$(cd "$(dirname "$0")" && pwd)/pkwidth-schema.sql"
log() { echo "[$(date '+%H:%M:%S')] $*"; }

ensure_container || { log "컨테이너 기동 실패"; exit 1; }
docker exec -i "$CT" mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS $DB" 2>/dev/null

# 스키마: 없을 때만 만든다 (재개 시 기존 데이터 보존)
have=$(mq "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB' AND table_name IN ('product_noidx','product_composite_noidx','product_partitioned_noidx')")
if [ "${have:-0}" != "3" ]; then
  log "스키마 생성"
  docker exec -i "$CT" mysql -uroot -proot "$DB" < "$SCHEMA" 2>/dev/null
fi

# 순번 테이블
seqmax=$(mq "SELECT IFNULL(MAX(n),0) FROM seq")
if [ "${seqmax:-0}" -lt "$ROWS" ]; then
  log "순번 테이블 생성 ($ROWS)"
  ensure_container
  docker exec -i "$CT" mysql -uroot -proot "$DB" -e "DROP TABLE IF EXISTS seq; CREATE TABLE seq (n BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB; INSERT INTO seq VALUES (1);" 2>/dev/null
  p=1
  while [ $p -lt $ROWS ]; do
    ensure_container
    docker exec -i "$CT" mysql -uroot -proot "$DB" -e "INSERT INTO seq (n) SELECT n + $p FROM seq WHERE n + $p <= $ROWS;" 2>/dev/null || { log "순번 청크 실패, 재시도"; sleep 5; ensure_container; docker exec -i "$CT" mysql -uroot -proot "$DB" -e "INSERT IGNORE INTO seq (n) SELECT n + $p FROM seq WHERE n + $p <= $ROWS;" 2>/dev/null; }
    p=$((p * 2))
  done
  log "   seq = $(mq 'SELECT COUNT(*) FROM seq')"
fi

# 연도 함수
ensure_container
docker exec -i "$CT" mysql -uroot -proot "$DB" -e "
DROP FUNCTION IF EXISTS f_year;
CREATE FUNCTION f_year(x BIGINT) RETURNS INT DETERMINISTIC
RETURN 2020 + LEAST(5, FLOOR((x - 1) / $((ROWS / 6 + 1))));" 2>/dev/null

# product 청크 시드 — 이미 들어간 구간은 건너뛴다
start=1
while [ $start -le $ROWS ]; do
  end=$((start + CHUNK - 1)); [ $end -gt $ROWS ] && end=$ROWS
  cnt=$(mq "SELECT COUNT(*) FROM product_noidx WHERE id BETWEEN $start AND $end")
  want=$((end - start + 1))
  if [ "${cnt:-0}" -eq "$want" ]; then start=$((end + 1)); continue; fi
  ensure_container
  docker exec -i "$CT" mysql -uroot -proot "$DB" -e "
    INSERT IGNORE INTO product_noidx (id, name, price, category, description, stock_quantity, created_date)
    SELECT n, CONCAT('Product-', n), ROUND(10 + (n % 990), 2),
           ELT(1 + (n % 10), 'Electronics','Books','Clothing','Food','Toys','Furniture','Sports','Beauty','Home','Garden'),
           CONCAT('Description for product ', n), n % 1000,
           MAKEDATE(f_year(n), ((n - 1) % 365) + 1)
    FROM seq WHERE n BETWEEN $start AND $end;" 2>/dev/null
  now=$(mq "SELECT COUNT(*) FROM product_noidx WHERE id BETWEEN $start AND $end")
  if [ "${now:-0}" -ne "$want" ]; then log "   product $start~$end 미완($now/$want) 재시도"; sleep 5; continue; fi
  log "   product_noidx $end/$ROWS"
  start=$((end + 1))
done

# 복제본 두 벌
for t in product_composite_noidx product_partitioned_noidx; do
  start=1
  while [ $start -le $ROWS ]; do
    end=$((start + CHUNK - 1)); [ $end -gt $ROWS ] && end=$ROWS
    cnt=$(mq "SELECT COUNT(*) FROM $t WHERE id BETWEEN $start AND $end")
    want=$((end - start + 1))
    if [ "${cnt:-0}" -eq "$want" ]; then start=$((end + 1)); continue; fi
    ensure_container
    docker exec -i "$CT" mysql -uroot -proot "$DB" -e "INSERT IGNORE INTO $t SELECT * FROM product_noidx WHERE id BETWEEN $start AND $end;" 2>/dev/null
    now=$(mq "SELECT COUNT(*) FROM $t WHERE id BETWEEN $start AND $end")
    if [ "${now:-0}" -ne "$want" ]; then log "   $t $start~$end 미완 재시도"; sleep 5; continue; fi
    start=$((end + 1))
  done
  log "   $t 완료 = $(mq "SELECT COUNT(*) FROM $t")"
done

ensure_container
docker exec -i "$CT" mysql -uroot -proot "$DB" -e "DROP TABLE IF EXISTS seq; DROP FUNCTION IF EXISTS f_year; ANALYZE TABLE product_noidx, product_composite_noidx, product_partitioned_noidx;" >/dev/null 2>&1
log "시드 완료"
