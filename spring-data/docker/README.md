# Docker Compose 가이드

## 사전 요구사항
- Docker Desktop 설치 및 실행

## MySQL 컨테이너 실행

### 1. 컨테이너 시작
```bash
docker-compose up -d
```

### 2. 컨테이너 상태 확인
```bash
docker-compose ps
```

### 3. MySQL 로그 확인
```bash
docker-compose logs -f mysql
```

### 4. MySQL 접속
```bash
# Root 계정으로 접속
docker-compose exec mysql mysql -u root -proot partition_test

# 일반 사용자 계정으로 접속
docker-compose exec mysql mysql -u testuser -ptestpass partition_test
```

### 5. 컨테이너 중지
```bash
docker-compose down
```

### 6. 볼륨 포함 완전 삭제
```bash
docker-compose down -v
```

## 파티션 정보 확인

MySQL 접속 후 다음 쿼리 실행:

```sql
-- 파티션 정보 조회
SELECT
    TABLE_NAME,
    PARTITION_NAME,
    PARTITION_METHOD,
    PARTITION_EXPRESSION,
    TABLE_ROWS
FROM INFORMATION_SCHEMA.PARTITIONS
WHERE TABLE_SCHEMA = 'partition_test'
  AND TABLE_NAME IN ('product_partitioned', 'comment')
ORDER BY TABLE_NAME, PARTITION_ORDINAL_POSITION;

-- 특정 파티션 데이터 조회
SELECT COUNT(*) FROM product_partitioned PARTITION (p2024);
```

## 성능 모니터링

```sql
-- 현재 실행 중인 쿼리 확인
SHOW FULL PROCESSLIST;

-- InnoDB 상태 확인
SHOW ENGINE INNODB STATUS;

-- 테이블 크기 확인
SELECT
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.TABLES
WHERE table_schema = 'partition_test'
ORDER BY size_mb DESC;
```

## 데이터 시딩

### 소규모 데이터 (테스트용)
```bash
# 100,000건 상품 + 200,000건 댓글 (2-5분 소요)
./scripts/seed-data.sh small
```

### 전체 데이터 (프로덕션 규모)
```bash
# 10,000,000건 상품 + 20,000,000건 댓글 (30-60분 소요)
./scripts/seed-data.sh full
```

### 데이터베이스 초기화
```bash
# 모든 데이터 삭제
./scripts/reset-database.sh
```

### 수동 실행 (MySQL 콘솔에서)
```bash
# MySQL 접속
docker-compose exec mysql mysql -u root -proot partition_test

# 소규모 데이터
CALL seed_product_small();
CALL seed_product_partitioned_small();
CALL seed_comment_small();

# 전체 데이터
CALL seed_product();
CALL seed_product_partitioned();
CALL seed_comment();
```

## 트러블슈팅

### Docker가 실행되지 않는 경우
1. Docker Desktop 애플리케이션을 실행하세요
2. 시스템 트레이에서 Docker 아이콘이 활성화될 때까지 기다리세요
3. `docker ps` 명령어로 Docker가 정상 작동하는지 확인하세요

### 포트 충돌 (3306)
다른 MySQL 인스턴스가 3306 포트를 사용 중인 경우:
```yaml
# docker-compose.yml 수정
ports:
  - "3307:3306"  # 호스트의 3307 포트로 변경
```
