#!/bin/bash

# Cache Practice 자동 테스트 실행 스크립트
set -e

echo "========================================="
echo "Cache Practice 자동 테스트 시작"
echo "========================================="
echo ""

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. 데이터 시드 완료 확인
echo -e "${YELLOW}1. 데이터 시드 완료 대기 중...${NC}"
while true; do
    COUNT=$(docker exec cache-practice-mysql mysql -uroot -ppassword -e "SELECT COUNT(*) as count FROM cache_practice.orders;" 2>/dev/null | tail -1)
    if [ "$COUNT" -ge "1000000" ]; then
        echo -e "${GREEN}✓ 데이터 시드 완료: $COUNT 건${NC}"
        break
    fi
    echo "현재: $COUNT / 1,000,000 건"
    sleep 30
done

# 2. 애플리케이션 재시작 (일반 모드)
echo ""
echo -e "${YELLOW}2. 애플리케이션 재시작 (일반 모드)${NC}"
pkill -f "cache-practice" || true
sleep 5

echo "애플리케이션 시작 중..."
./gradlew bootRun > app-log.txt 2>&1 &
APP_PID=$!
echo "PID: $APP_PID"

# 헬스 체크 대기
echo "헬스 체크 대기 중..."
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
        echo -e "${GREEN}✓ 애플리케이션 준비 완료${NC}"
        break
    fi
    sleep 2
done

# Eager loading 캐시 로드 대기 (로그 확인)
echo "Eager loading 캐시 로드 대기 중..."
sleep 120

# 3. k6 테스트 실행
echo ""
echo -e "${YELLOW}3. k6 부하 테스트 실행${NC}"

# k6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}k6가 설치되어 있지 않습니다. 설치 방법:${NC}"
    echo "macOS: brew install k6"
    echo "https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# 3-1. 캐시 없음 테스트
echo ""
echo -e "${YELLOW}3-1. 캐시 없음 (Baseline) 테스트${NC}"
k6 run k6/test-no-cache.js --out json=results-no-cache.json

# 3-2. Lazy Loading 테스트
echo ""
echo -e "${YELLOW}3-2. Lazy Loading 테스트${NC}"
k6 run k6/test-lazy-cache.js --out json=results-lazy.json

# 3-3. Eager Loading 테스트 (2분)
echo ""
echo -e "${YELLOW}3-3. Eager Loading 테스트 (TTL 관찰)${NC}"
k6 run k6/test-eager-cache.js --out json=results-eager.json

# 4. 메트릭 수집
echo ""
echo -e "${YELLOW}4. Prometheus 메트릭 수집${NC}"
curl -s http://localhost:8080/actuator/metrics > metrics-all.json
curl -s http://localhost:8080/actuator/prometheus > metrics-prometheus.txt

echo -e "${GREEN}✓ 메트릭 수집 완료${NC}"

# 5. 완료 메시지
echo ""
echo "========================================="
echo -e "${GREEN}모든 테스트 완료!${NC}"
echo "========================================="
echo ""
echo "생성된 파일:"
echo "  - results-no-cache.json"
echo "  - results-lazy.json"
echo "  - results-eager.json"
echo "  - metrics-all.json"
echo "  - metrics-prometheus.txt"
echo "  - app-log.txt"
echo ""
echo "다음 단계:"
echo "  1. Grafana 확인: http://localhost:3000"
echo "  2. PERFORMANCE_REPORT.md 업데이트"
echo ""
