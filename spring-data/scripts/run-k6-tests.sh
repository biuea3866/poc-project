#!/bin/bash

# k6 성능 테스트 실행 스크립트

set -e

echo "======================================"
echo "k6 성능 테스트 시작"
echo "======================================"

# k6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo "❌ k6가 설치되지 않았습니다."
    echo ""
    echo "설치 방법:"
    echo "  macOS: brew install k6"
    echo "  Linux: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

echo "✓ k6 설치 확인됨"
echo ""

# 애플리케이션 실행 확인
echo "애플리케이션 health check..."
if ! curl -s http://localhost:8080/api/partition-test/health > /dev/null; then
    echo "❌ 애플리케이션이 실행되지 않았습니다."
    echo "다음 명령어로 애플리케이션을 시작하세요:"
    echo "  ./gradlew bootRun"
    echo "또는"
    echo "  java -jar build/libs/spring-data-0.0.1-SNAPSHOT.jar --spring.profiles.active=performance"
    exit 1
fi

echo "✓ 애플리케이션 실행 중"
echo ""

# 결과 디렉토리 생성
mkdir -p k6-results

# 테스트 실행
echo "======================================"
echo "API 1: 파티셔닝 미적용 상품 조회"
echo "======================================"
k6 run k6-tests/test-api1-non-partitioned.js --out json=k6-results/api1-non-partitioned.json

echo ""
echo "======================================"
echo "API 2: 파티셔닝 적용 상품 조회"
echo "======================================"
k6 run k6-tests/test-api2-partitioned.js --out json=k6-results/api2-partitioned.json

echo ""
echo "======================================"
echo "API 3: JOIN 파티션 키 미포함"
echo "======================================"
k6 run k6-tests/test-api3-join-without-partition-key.js --out json=k6-results/api3-join-without-key.json

echo ""
echo "======================================"
echo "API 4: JOIN 파티션 키 포함"
echo "======================================"
k6 run k6-tests/test-api4-join-with-partition-key.js --out json=k6-results/api4-join-with-key.json

echo ""
echo "======================================"
echo "✓ 모든 테스트 완료!"
echo "======================================"
echo ""
echo "결과 파일:"
ls -lh k6-results/

echo ""
echo "JSON 결과 요약:"
for file in k6-results/*.json; do
    echo "---"
    echo "파일: $file"
    if [ -f "$file" ]; then
        echo "크기: $(wc -c < "$file") bytes"
    fi
done
