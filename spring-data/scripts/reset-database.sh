#!/bin/bash

# 데이터베이스 초기화 스크립트
# 모든 테이블 데이터를 삭제하고 초기 상태로 되돌립니다.

set -e

MYSQL_DATABASE="partition_test"

echo "======================================"
echo "데이터베이스 초기화"
echo "======================================"

# Docker 컨테이너 확인
if ! docker ps | grep -q mysql-partition-test; then
    echo "❌ MySQL 컨테이너가 실행되지 않았습니다."
    exit 1
fi

echo "⚠️  모든 데이터가 삭제됩니다. 계속하시겠습니까? (y/n)"
read -r answer
if [ "$answer" != "y" ]; then
    echo "취소되었습니다."
    exit 0
fi

echo ""
echo "테이블 데이터 삭제 중..."

docker exec mysql-partition-test mysql -u root -proot $MYSQL_DATABASE -e "
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE product;
TRUNCATE TABLE product_partitioned;
TRUNCATE TABLE comment;
SET FOREIGN_KEY_CHECKS = 1;
"

echo "✓ 데이터베이스가 초기화되었습니다."

# 현재 상태 확인
echo ""
echo "현재 데이터 건수:"
docker exec mysql-partition-test mysql -u root -proot $MYSQL_DATABASE -e "
SELECT 'product' AS table_name, COUNT(*) AS row_count FROM product
UNION ALL
SELECT 'product_partitioned', COUNT(*) FROM product_partitioned
UNION ALL
SELECT 'comment', COUNT(*) FROM comment;
"
