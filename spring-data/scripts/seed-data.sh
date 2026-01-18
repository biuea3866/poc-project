#!/bin/bash

# MySQL 데이터 시딩 스크립트
# 사용법: ./scripts/seed-data.sh [small|full]

set -e

MODE=${1:-small}

MYSQL_HOST="localhost"
MYSQL_PORT="3306"
MYSQL_USER="root"
MYSQL_PASSWORD="root"
MYSQL_DATABASE="partition_test"

echo "======================================"
echo "MySQL 데이터 시딩 시작"
echo "모드: $MODE"
echo "======================================"

# Docker 컨테이너가 실행 중인지 확인
if ! docker ps | grep -q mysql-partition-test; then
    echo "❌ MySQL 컨테이너가 실행되지 않았습니다."
    echo "다음 명령어로 컨테이너를 시작하세요: docker-compose up -d"
    exit 1
fi

echo "✓ MySQL 컨테이너 실행 중"

# MySQL 연결 대기
echo "MySQL 연결 대기 중..."
for i in {1..30}; do
    if docker exec mysql-partition-test mysqladmin ping -h localhost -u root -proot --silent; then
        echo "✓ MySQL 연결 성공"
        break
    fi
    echo "대기 중... ($i/30)"
    sleep 2
done

if [ "$MODE" = "small" ]; then
    echo ""
    echo "======================================"
    echo "소규모 데이터 시딩 시작 (테스트용)"
    echo "- product: 100,000건"
    echo "- product_partitioned: 100,000건"
    echo "- comment: 200,000건"
    echo "예상 소요 시간: 2-5분"
    echo "======================================"
    echo ""

    docker exec -i mysql-partition-test mysql -u root -proot $MYSQL_DATABASE <<EOF
CALL seed_product_small();
CALL seed_product_partitioned_small();
CALL seed_comment_small();
EOF

elif [ "$MODE" = "full" ]; then
    echo ""
    echo "======================================"
    echo "전체 데이터 시딩 시작"
    echo "- product: 10,000,000건"
    echo "- product_partitioned: 10,000,000건"
    echo "- comment: 20,000,000건"
    echo "예상 소요 시간: 30-60분"
    echo "======================================"
    echo ""

    echo "⚠️  이 작업은 매우 오래 걸립니다. 계속하시겠습니까? (y/n)"
    read -r answer
    if [ "$answer" != "y" ]; then
        echo "취소되었습니다."
        exit 0
    fi

    docker exec -i mysql-partition-test mysql -u root -proot $MYSQL_DATABASE <<EOF
CALL seed_product();
CALL seed_product_partitioned();
CALL seed_comment();
EOF

else
    echo "❌ 잘못된 모드입니다. 'small' 또는 'full'을 사용하세요."
    exit 1
fi

echo ""
echo "======================================"
echo "✓ 데이터 시딩 완료!"
echo "======================================"

# 데이터 확인
echo ""
echo "데이터 건수 확인:"
docker exec mysql-partition-test mysql -u root -proot $MYSQL_DATABASE -e "
SELECT 'product' AS table_name, COUNT(*) AS row_count FROM product
UNION ALL
SELECT 'product_partitioned', COUNT(*) FROM product_partitioned
UNION ALL
SELECT 'comment', COUNT(*) FROM comment;
"

echo ""
echo "파티션별 데이터 분포:"
docker exec mysql-partition-test mysql -u root -proot $MYSQL_DATABASE -e "
SELECT
    TABLE_NAME,
    PARTITION_NAME,
    TABLE_ROWS
FROM INFORMATION_SCHEMA.PARTITIONS
WHERE TABLE_SCHEMA = '$MYSQL_DATABASE'
  AND TABLE_NAME IN ('product_partitioned', 'comment')
ORDER BY TABLE_NAME, PARTITION_ORDINAL_POSITION;
"
