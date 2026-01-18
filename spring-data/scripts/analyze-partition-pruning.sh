#!/bin/bash

# 파티셔닝 프루닝 분석 스크립트
# EXPLAIN 결과를 분석하여 파티션 프루닝이 제대로 동작하는지 확인

set -e

OUTPUT_FILE="partition-pruning-analysis-$(date +%Y%m%d-%H%M%S).txt"

echo "======================================"
echo "파티셔닝 프루닝 분석"
echo "======================================"
echo "출력 파일: ${OUTPUT_FILE}"
echo "======================================"
echo ""

# Docker 컨테이너 확인
if ! docker ps | grep -q mysql-partition-test; then
    echo "❌ MySQL 컨테이너가 실행되지 않았습니다."
    exit 1
fi

echo "✓ MySQL 컨테이너 실행 중" | tee "$OUTPUT_FILE"
echo "" | tee -a "$OUTPUT_FILE"

# SQL 파일 실행
echo "=====================================" | tee -a "$OUTPUT_FILE"
echo "EXPLAIN 분석 실행 중..." | tee -a "$OUTPUT_FILE"
echo "=====================================" | tee -a "$OUTPUT_FILE"
echo "" | tee -a "$OUTPUT_FILE"

docker exec -i mysql-partition-test mysql -u root -proot partition_test < scripts/check-partition-pruning.sql >> "$OUTPUT_FILE" 2>&1

echo "" | tee -a "$OUTPUT_FILE"
echo "=====================================" | tee -a "$OUTPUT_FILE"
echo "✓ 분석 완료!" | tee -a "$OUTPUT_FILE"
echo "=====================================" | tee -a "$OUTPUT_FILE"
echo "" | tee -a "$OUTPUT_FILE"
echo "결과 파일: $OUTPUT_FILE"

# 주요 포인트 추출
echo ""
echo "======================================"
echo "주요 분석 포인트:"
echo "======================================"
echo ""

echo "1. 파티션 정보 확인"
echo "   - 각 파티션의 데이터 건수와 크기 확인"
echo ""

echo "2. EXPLAIN의 'partitions' 컬럼 확인"
echo "   - 파티션 키 포함: 특정 파티션명 표시 (예: p2024)"
echo "   - 파티션 키 미포함: 모든 파티션 표시 (예: p2020,p2021,p2022,...)"
echo ""

echo "3. 'rows' 컬럼 비교"
echo "   - 파티션 프루닝 적용 시: 적은 rows 수"
echo "   - 파티션 프루닝 미적용 시: 많은 rows 수"
echo ""

echo "4. 'Extra' 컬럼 확인"
echo "   - 'Using where' 표시 확인"
echo ""

echo "상세 결과는 ${OUTPUT_FILE} 파일을 확인하세요."
