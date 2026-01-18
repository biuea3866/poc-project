#!/bin/bash

# MySQL 리소스 모니터링 스크립트
# CPU, Memory, 커넥션 수 등을 주기적으로 모니터링

set -e

INTERVAL=${1:-5}  # 모니터링 간격 (초), 기본값 5초
DURATION=${2:-60}  # 모니터링 시간 (초), 기본값 60초
OUTPUT_FILE="mysql-monitor-$(date +%Y%m%d-%H%M%S).log"

echo "======================================"
echo "MySQL 리소스 모니터링"
echo "======================================"
echo "간격: ${INTERVAL}초"
echo "시간: ${DURATION}초"
echo "출력 파일: ${OUTPUT_FILE}"
echo "======================================"
echo ""

# Docker 컨테이너 확인
if ! docker ps | grep -q mysql-partition-test; then
    echo "❌ MySQL 컨테이너가 실행되지 않았습니다."
    exit 1
fi

echo "✓ MySQL 컨테이너 실행 중"
echo ""

# CSV 헤더 작성
echo "timestamp,cpu_percent,memory_usage_mb,memory_limit_mb,memory_percent,net_input_mb,net_output_mb,connections,threads_running,queries_per_sec" > "$OUTPUT_FILE"

# 모니터링 시작
echo "모니터링 시작..."
END_TIME=$((SECONDS + DURATION))

while [ $SECONDS -lt $END_TIME ]; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

    # Docker stats (CPU, Memory)
    STATS=$(docker stats mysql-partition-test --no-stream --format "{{.CPUPerc}},{{.MemUsage}}" | head -1)
    CPU_PERCENT=$(echo "$STATS" | cut -d',' -f1 | sed 's/%//')
    MEM_USAGE=$(echo "$STATS" | cut -d',' -f2)
    MEM_USED=$(echo "$MEM_USAGE" | awk '{print $1}' | sed 's/MiB//')
    MEM_LIMIT=$(echo "$MEM_USAGE" | awk '{print $3}' | sed 's/GiB//' | awk '{print $1*1024}')

    # Memory percent 계산
    if [ ! -z "$MEM_USED" ] && [ ! -z "$MEM_LIMIT" ]; then
        MEM_PERCENT=$(echo "scale=2; $MEM_USED / $MEM_LIMIT * 100" | bc)
    else
        MEM_PERCENT="0"
    fi

    # Network I/O
    NET_STATS=$(docker exec mysql-partition-test cat /sys/class/net/eth0/statistics/rx_bytes /sys/class/net/eth0/statistics/tx_bytes 2>/dev/null || echo "0 0")
    NET_INPUT=$(echo "$NET_STATS" | head -1 | awk '{printf "%.2f", $1/1024/1024}')
    NET_OUTPUT=$(echo "$NET_STATS" | tail -1 | awk '{printf "%.2f", $1/1024/1024}')

    # MySQL 통계
    MYSQL_STATS=$(docker exec mysql-partition-test mysql -u root -proot -e "
        SELECT
            (SELECT COUNT(*) FROM information_schema.PROCESSLIST) as connections,
            (SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE Command != 'Sleep') as threads_running,
            (SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME='Queries') as total_queries
        " --skip-column-names 2>/dev/null || echo "0 0 0")

    CONNECTIONS=$(echo "$MYSQL_STATS" | awk '{print $1}')
    THREADS_RUNNING=$(echo "$MYSQL_STATS" | awk '{print $2}')
    TOTAL_QUERIES=$(echo "$MYSQL_STATS" | awk '{print $3}')

    # QPS 계산 (이전 값과 비교)
    if [ ! -z "$PREV_QUERIES" ]; then
        QPS=$(echo "scale=2; ($TOTAL_QUERIES - $PREV_QUERIES) / $INTERVAL" | bc)
    else
        QPS="0"
    fi
    PREV_QUERIES=$TOTAL_QUERIES

    # CSV에 기록
    echo "$TIMESTAMP,$CPU_PERCENT,$MEM_USED,$MEM_LIMIT,$MEM_PERCENT,$NET_INPUT,$NET_OUTPUT,$CONNECTIONS,$THREADS_RUNNING,$QPS" >> "$OUTPUT_FILE"

    # 콘솔 출력
    printf "[%s] CPU: %s%% | MEM: %sMiB/%sMiB (%s%%) | Conn: %s | Running: %s | QPS: %s\n" \
        "$TIMESTAMP" "$CPU_PERCENT" "$MEM_USED" "$MEM_LIMIT" "$MEM_PERCENT" "$CONNECTIONS" "$THREADS_RUNNING" "$QPS"

    sleep $INTERVAL
done

echo ""
echo "======================================"
echo "✓ 모니터링 완료!"
echo "======================================"
echo "결과 파일: $OUTPUT_FILE"
echo ""

# 통계 요약
echo "통계 요약:"
echo "---"
awk -F',' 'NR>1 {
    cpu_sum+=$2; cpu_count++;
    mem_sum+=$5; mem_count++;
    conn_sum+=$8; conn_count++;
    qps_sum+=$10; qps_count++;
    if(NR==2 || $2>cpu_max) cpu_max=$2;
    if(NR==2 || $5>mem_max) mem_max=$5;
    if(NR==2 || $8>conn_max) conn_max=$8;
    if(NR==2 || $10>qps_max) qps_max=$10;
} END {
    printf "CPU - 평균: %.2f%%, 최대: %.2f%%\n", cpu_sum/cpu_count, cpu_max;
    printf "Memory - 평균: %.2f%%, 최대: %.2f%%\n", mem_sum/mem_count, mem_max;
    printf "Connections - 평균: %.0f, 최대: %.0f\n", conn_sum/conn_count, conn_max;
    printf "QPS - 평균: %.2f, 최대: %.2f\n", qps_sum/qps_count, qps_max;
}' "$OUTPUT_FILE"
