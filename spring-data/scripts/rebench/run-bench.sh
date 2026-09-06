#!/bin/bash
# 파티셔닝 재검증 벤치마크 실행기. 사용법: run-bench.sh <uniform|skewed> [케이스 정규식]
# 두 번째 인수를 주면 이름이 일치하는 케이스만 돌린다 (예: '^(A|C)-' → 실험 A·C 만).
#
# 측정 프로토콜 (엔드포인트 1개 단위)
#   1. MySQL 재기동 → 버퍼 풀 초기화. 직전 엔드포인트가 데운 페이지가 다음 측정에 섞이지 않게 한다.
#   2. 워밍업 40초. 커넥션 재수립과 해당 워크로드의 작업 집합 적재를 여기서 끝낸다.
#   3. 본 측정 3회. 중앙값과 최소~최대를 함께 기록한다.
# 앱(JVM)은 스위트 내내 띄워 둔다. 재려는 것은 DB 동작이지 JIT 워밍업이 아니다.
set -uo pipefail

DATASET=$1
FILTER=${2:-.}
DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/results/$DATASET"
CONTAINER=mysql-partition-rebench
APP=http://localhost:8080
REPS=3

mkdir -p "$OUT"

mysql_q() { docker exec -i $CONTAINER mysql -uroot -proot -N -B "partition_$DATASET" -e "$1" 2>/dev/null; }

restart_mysql() {
  docker restart $CONTAINER >/dev/null
  until docker exec $CONTAINER mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; do sleep 2; done
  until mysql_q "SELECT 1" >/dev/null 2>&1; do sleep 2; done
}

# 2024년 상품의 id 구간 — 실험 C 의 조인 대상 윈도우를 여기서 고른다.
Y2024_MIN=$(mysql_q "SELECT MIN(id) FROM product WHERE created_date BETWEEN '2024-01-01' AND '2024-12-31'")
Y2024_MAX=$(mysql_q "SELECT MAX(id) FROM product WHERE created_date BETWEEN '2024-01-01' AND '2024-12-31'")
ID_MAX=$(mysql_q "SELECT MAX(id) FROM product")
echo "dataset=$DATASET  2024 id range=[$Y2024_MIN, $Y2024_MAX]  max id=$ID_MAX"

RANGE="startDate=2024-01-01&endDate=2024-12-31"

# name|url|mode|id_min|id_max
CASES=(
  "A-date-plain|$APP/api/rebench/a/plain?$RANGE&orderBy=date|static|1|1"
  "A-date-composite|$APP/api/rebench/a/composite?$RANGE&orderBy=date|static|1|1"
  "A-date-partitioned|$APP/api/rebench/a/partitioned?$RANGE&orderBy=date|static|1|1"
  "A-id-plain|$APP/api/rebench/a/plain?$RANGE&orderBy=id|static|1|1"
  "A-id-composite|$APP/api/rebench/a/composite?$RANGE&orderBy=id|static|1|1"
  "A-id-partitioned|$APP/api/rebench/a/partitioned?$RANGE&orderBy=id|static|1|1"
  "AGG-plain|$APP/api/rebench/agg/plain?$RANGE|static|1|1"
  "AGG-composite|$APP/api/rebench/agg/composite?$RANGE|static|1|1"
  "AGG-partitioned|$APP/api/rebench/agg/partitioned?$RANGE|static|1|1"
  "B-plain|$APP/api/rebench/b/plain?x=1|randid|1|$ID_MAX"
  "B-composite|$APP/api/rebench/b/composite?x=1|randid|1|$ID_MAX"
  "B-partitioned|$APP/api/rebench/b/partitioned?x=1|randid|1|$ID_MAX"
  "C-nokey|$APP/api/rebench/c?$RANGE&withKey=false|window|$Y2024_MIN|$Y2024_MAX"
  "C-withkey|$APP/api/rebench/c?$RANGE&withKey=true|window|$Y2024_MIN|$Y2024_MAX"
)

for case in "${CASES[@]}"; do
  IFS='|' read -r name url mode idmin idmax <<< "$case"
  if ! echo "$name" | grep -Eq "$FILTER"; then continue; fi
  echo ""
  echo "=============================================="
  echo " $name"
  echo "=============================================="

  restart_mysql

  ENVS="-e TARGET_URL=$url -e MODE=$mode -e ID_MIN=$idmin -e ID_MAX=$idmax -e ID_WINDOW=1000"

  echo "[warmup 40s]"
  k6 run $ENVS -e RAMP=10s -e STEADY=30s --no-summary --quiet "$DIR/bench.js" >/dev/null 2>&1

  for rep in $(seq 1 $REPS); do
    echo "[rep $rep/$REPS]"
    k6 run $ENVS --summary-export="$OUT/${name}-rep${rep}.json" --quiet "$DIR/bench.js" \
      > "$OUT/${name}-rep${rep}.log" 2>&1
    rc=$?
    if [ $rc -ne 0 ]; then
      echo "  k6 exit=$rc — 로그: $OUT/${name}-rep${rep}.log"
    fi
    python3 - "$OUT/${name}-rep${rep}.json" <<'PY'
import json, sys
d = json.load(open(sys.argv[1]))
m = d["metrics"]
reqs = m["http_reqs"]
dur = m["http_req_duration"]
failed = m.get("http_req_failed", {}).get("value", 0)
print(f"  TPS={reqs['rate']:.1f}  avg={dur['avg']:.0f}ms  p95={dur['p(95)']:.0f}ms  fail={failed*100:.2f}%")
PY
  done
done

echo ""
echo "완료. 결과: $OUT"
