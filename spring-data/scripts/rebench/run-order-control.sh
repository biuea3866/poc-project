#!/bin/bash
# 순서 효과 대조군. 사용법: run-order-control.sh <uniform|skewed>
#
# docker restart 는 InnoDB 버퍼 풀만 비우고 호스트 OS 페이지 캐시는 남긴다.
# 그래서 뒤에 도는 케이스일수록 유리해지고, 이 순서 효과가 테이블 속성 차이와 섞인다.
# 본 측정과 정반대 순서로 같은 세 케이스를 다시 돌려, 순위가 뒤집히는지 확인한다.
#   순위 유지 → 테이블 속성 차이가 실재한다
#   순위 반전 → 관측된 차이는 순서 효과다
set -uo pipefail

DATASET=$1
SET=${2:-a}   # a = 실험 A, aggc = 실험 A''(집계) + C(JOIN)
DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/results/$DATASET"
CONTAINER=mysql-partition-rebench
APP=http://localhost:8080
RANGE="startDate=2024-01-01&endDate=2024-12-31"

mkdir -p "$OUT"

restart_mysql() {
  docker restart $CONTAINER >/dev/null
  until docker exec $CONTAINER mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; do sleep 2; done
  until docker exec -i $CONTAINER mysql -uroot -proot -N -B "partition_$DATASET" -e "SELECT 1" >/dev/null 2>&1; do sleep 2; done
}

# 본 측정은 plain → composite → partitioned, nokey → withkey 순이었다. 여기서는 반대로 간다.
if [ "$SET" = "a" ]; then
  CASES=(
    "REV-A-date-partitioned|$APP/api/rebench/a/partitioned?$RANGE&orderBy=date|static"
    "REV-A-date-composite|$APP/api/rebench/a/composite?$RANGE&orderBy=date|static"
    "REV-A-date-plain|$APP/api/rebench/a/plain?$RANGE&orderBy=date|static"
  )
else
  Y_MIN=$(docker exec -i $CONTAINER mysql -uroot -proot -N -B "partition_$DATASET" \
    -e "SELECT MIN(id) FROM product WHERE created_date BETWEEN '2024-01-01' AND '2024-12-31'" 2>/dev/null)
  Y_MAX=$(docker exec -i $CONTAINER mysql -uroot -proot -N -B "partition_$DATASET" \
    -e "SELECT MAX(id) FROM product WHERE created_date BETWEEN '2024-01-01' AND '2024-12-31'" 2>/dev/null)
  CASES=(
    "REV-C-withkey|$APP/api/rebench/c?$RANGE&withKey=true|window|$Y_MIN|$Y_MAX"
    "REV-C-nokey|$APP/api/rebench/c?$RANGE&withKey=false|window|$Y_MIN|$Y_MAX"
    "REV-AGG-partitioned|$APP/api/rebench/agg/partitioned?$RANGE|static|1|1"
    "REV-AGG-composite|$APP/api/rebench/agg/composite?$RANGE|static|1|1"
    "REV-AGG-plain|$APP/api/rebench/agg/plain?$RANGE|static|1|1"
  )
fi

for case in "${CASES[@]}"; do
  IFS='|' read -r name url mode idmin idmax <<< "$case"
  mode=${mode:-static}; idmin=${idmin:-1}; idmax=${idmax:-1}
  echo ""
  echo "===== $name ====="
  restart_mysql
  ENVS="-e TARGET_URL=$url -e MODE=$mode -e ID_MIN=$idmin -e ID_MAX=$idmax -e ID_WINDOW=1000"
  k6 run $ENVS -e RAMP=10s -e STEADY=30s --no-summary --quiet "$DIR/bench.js" >/dev/null 2>&1
  for rep in 1 2 3; do
    k6 run $ENVS --summary-export="$OUT/${name}-rep${rep}.json" --quiet "$DIR/bench.js" \
      > "$OUT/${name}-rep${rep}.log" 2>&1
    python3 - "$OUT/${name}-rep${rep}.json" <<'PY'
import json, sys
m = json.load(open(sys.argv[1]))["metrics"]
print(f"  rep TPS={m['http_reqs']['rate']:.1f}  avg={m['http_req_duration']['avg']:.0f}ms")
PY
  done
done
echo ""
echo "대조군 완료: $OUT"
