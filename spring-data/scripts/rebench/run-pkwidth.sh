#!/bin/bash
# 날짜 인덱스 유무 대조 측정. 사용법: run-ablation.sh [forward|reverse]
#
# 본 재검증은 네 테이블 전부에 idx_created_date 를 걸고 "파티셔닝이냐 인덱스냐" 를 물었다.
# 여기서는 인덱스를 뺀 짝을 함께 재서 "인덱스 없이 파티셔닝만 있으면" 을 답한다.
# 순서 효과가 결과를 뒤집은 전례가 있으므로 정순과 역순을 모두 돌린다.
set -uo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ORDER=${1:-forward}
OUT="$DIR/results/pkwidth"
CONTAINER=mysql-pkw
APP=http://localhost:8080
RANGE="startDate=2024-01-01&endDate=2024-12-31"
mkdir -p "$OUT"

# MySQL 이 준비된 뒤, 앱이 실제로 200 을 돌려줄 때까지 기다린다.
# 컨테이너만 확인하면 커넥션 풀이 회복되기 전에 측정이 시작돼 전 구간이 타임아웃된다.
wait_app() {
  local url="$1" i=0
  until [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$url")" = "200" ]; do
    sleep 3; i=$((i+1))
    [ $((i % 5)) -eq 0 ] && { docker ps --format '{{.Names}}' | grep -qx "$CONTAINER" || ensure_container; }
    [ $i -gt 40 ] && return 1
  done
  return 0
}

restart_mysql() {
  docker restart $CONTAINER >/dev/null
  until docker exec $CONTAINER mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; do sleep 2; done
  until docker exec -i $CONTAINER mysql -uroot -proot -N -B partition_pkwidth -e "SELECT 1" >/dev/null 2>&1; do sleep 2; done
}

CASES=(
  "range-plain|$APP/api/rebench/a/plain-noidx?$RANGE&orderBy=date"
  "range-composite|$APP/api/rebench/a/composite-noidx?$RANGE&orderBy=date"
  "range-part|$APP/api/rebench/a/partitioned-noidx?$RANGE&orderBy=date"
  "agg-plain|$APP/api/rebench/agg/plain-noidx?$RANGE"
  "agg-composite|$APP/api/rebench/agg/composite-noidx?$RANGE"
  "agg-part|$APP/api/rebench/agg/partitioned-noidx?$RANGE"
)

if [ "$ORDER" = "reverse" ]; then
  for ((i=${#CASES[@]}-1; i>=0; i--)); do REV+=("${CASES[$i]}"); done
  CASES=("${REV[@]}")
  PREFIX="REV-"
else
  PREFIX=""
fi

for case in "${CASES[@]}"; do
  IFS='|' read -r name url <<< "$case"
  name="${PREFIX}${name}"
  # 이미 3회 모두 정상(실패율 5% 이하)으로 받은 케이스는 건너뛴다.
  if python3 "$DIR/check-case.py" "$OUT" "$name"; then
    echo ""; echo "===== $name ===== (완료됨, 건너뜀)"
    continue
  fi
  echo ""
  echo "===== $name ====="
  restart_mysql
  ENVS="-e TARGET_URL=$url -e MODE=static -e ID_MIN=1 -e ID_MAX=1"
  wait_app "$url" || { echo "  앱 응답 없음 — 이 케이스 건너뜀"; continue; }
  k6 run $ENVS -e RAMP=10s -e STEADY=30s --no-summary --quiet "$DIR/bench.js" >/dev/null 2>&1
  for rep in 1 2 3; do
    attempt=0
    while [ $attempt -lt 5 ]; do
      attempt=$((attempt + 1))
      docker ps --format '{{.Names}}' | grep -qx "$CONTAINER" || { echo "  컨테이너 소실 — 복구"; ensure_container; }
      wait_app "$url" || { echo "  앱 응답 없음 — 대기"; sleep 10; continue; }
      k6 run $ENVS --summary-export="$OUT/${name}-rep${rep}.json" --quiet "$DIR/bench.js" \
        > "$OUT/${name}-rep${rep}.log" 2>&1
      # 실패율 5% 초과는 DB 가 사라진 구간이라 측정값이 아니다. 되살리고 다시 잰다.
      python3 "$DIR/check-rep.py" "$OUT/${name}-rep${rep}.json"
      rc=$?
      [ $rc -eq 0 ] && break
      echo "  실패율 초과 — 복구 후 rep $rep 재측정 (시도 $attempt/5)"
      ensure_container
      sleep 5
    done
  done
done
echo ""; echo "완료: $OUT"
