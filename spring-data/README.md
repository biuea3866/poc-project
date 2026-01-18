# MySQL Partitioning Performance Test

MySQL 파티셔닝 성능 테스트 프로젝트입니다. 파티셔닝 적용 전/후 성능 비교 및 JOIN 시 파티션 키 포함 여부에 따른 성능 차이를 분석합니다.

## 프로젝트 구조

```
spring-data/
├── docker/                      # Docker 설정
│   ├── mysql/
│   │   ├── init/               # 초기화 SQL
│   │   │   ├── 01-schema.sql  # 테이블 스키마
│   │   │   ├── 02-seed-data.sql       # 대용량 시딩
│   │   │   └── 03-seed-data-small.sql # 소규모 시딩
│   │   └── conf.d/             # MySQL 설정
│   └── README.md
├── scripts/                     # 실행 스크립트
│   ├── seed-data.sh            # 데이터 시딩
│   ├── reset-database.sh       # DB 초기화
│   ├── run-k6-tests.sh         # k6 테스트 실행
│   ├── monitor-mysql.sh        # MySQL 모니터링
│   ├── check-partition-pruning.sql
│   └── analyze-partition-pruning.sh
├── k6-tests/                    # k6 성능 테스트
│   ├── test-api1-non-partitioned.js
│   ├── test-api2-partitioned.js
│   ├── test-api3-join-without-partition-key.js
│   └── test-api4-join-with-partition-key.js
├── src/main/kotlin/com/biuea/springdata/
│   ├── domain/                  # Entity
│   ├── repository/              # JPA Repository
│   ├── service/                 # Service Layer
│   ├── controller/              # REST Controller
│   └── dto/                     # DTO
├── docker-compose.yml
├── API.md                       # API 문서
└── README.md
```

## 테스트 데이터

- **product** (파티셔닝 미적용): 10,000,000건
- **product_partitioned** (파티셔닝 적용): 10,000,000건
- **comment** (파티셔닝 적용): 20,000,000건

파티셔닝: RANGE 파티셔닝 (연도별)

## 빠른 시작

### 1. MySQL 시작 및 데이터 시딩

```bash
# MySQL 컨테이너 시작
docker-compose up -d

# 소규모 테스트 데이터 (권장 - 첫 테스트용)
./scripts/seed-data.sh small

# 전체 데이터 (30-60분 소요)
./scripts/seed-data.sh full
```

### 2. 애플리케이션 실행

```bash
# 개발 모드
./gradlew bootRun

# 성능 테스트 모드 (로깅 최소화)
./gradlew bootJar
java -jar build/libs/spring-data-0.0.1-SNAPSHOT.jar --spring.profiles.active=performance
```

### 3. API 테스트

```bash
# Health Check
curl http://localhost:8080/api/partition-test/health

# API 1: 파티셔닝 미적용
curl "http://localhost:8080/api/partition-test/products/non-partitioned?page=0&size=100&startDate=2024-01-01&endDate=2024-12-31"

# API 2: 파티셔닝 적용
curl "http://localhost:8080/api/partition-test/products/partitioned?page=0&size=100&startDate=2024-01-01&endDate=2024-12-31"

# API 3: JOIN 파티션 키 미포함
curl "http://localhost:8080/api/partition-test/products-with-comments/without-partition-key?page=0&size=10&productStartDate=2024-01-01&productEndDate=2024-12-31"

# API 4: JOIN 파티션 키 포함
curl "http://localhost:8080/api/partition-test/products-with-comments/with-partition-key?page=0&size=10&startDate=2024-01-01&endDate=2024-12-31"
```

## 성능 테스트

### k6 부하 테스트

```bash
# k6 설치 (macOS)
brew install k6

# 전체 테스트 실행
./scripts/run-k6-tests.sh
```

### MySQL 리소스 모니터링

```bash
# 60초 동안 5초 간격으로 모니터링
./scripts/monitor-mysql.sh 5 60
```

### 파티셔닝 프루닝 분석

```bash
# EXPLAIN 분석
./scripts/analyze-partition-pruning.sh
```

## 테스트 시나리오

### API 1 vs API 2: 파티셔닝 적용 효과
- **목적**: 파티셔닝이 조회 성능에 미치는 영향 측정
- **비교**: product (파티셔닝 X) vs product_partitioned (파티셔닝 O)
- **예상 결과**: 파티션 프루닝 적용 시 더 빠른 조회

### API 3 vs API 4: 파티션 키 포함 여부
- **목적**: JOIN 시 파티션 키 포함 여부가 성능에 미치는 영향
- **API 3**: 댓글 조회 시 파티션 키 미포함 (전체 파티션 스캔)
- **API 4**: 댓글 조회 시 파티션 키 포함 (특정 파티션만 스캔)
- **예상 결과**: API 4가 API 3보다 훨씬 빠름

## 측정 항목

### 1. 성능 지표 (k6)
- TPS (Transactions Per Second)
- p50, p95, p99 Latency
- Error Rate

### 2. 리소스 사용률
- CPU 사용률 (%)
- Memory 사용률 (%)
- Network I/O
- Active Connections
- QPS (Queries Per Second)

### 3. 파티션 프루닝
- EXPLAIN의 partitions 컬럼
- Scanned rows 수
- Execution time

## 결과 문서화

결과는 `mysql-partition.md` 파일의 **# 결과** 섹션에 테이블 형태로 기록합니다.

```markdown
## 결과

### 성능 테스트 결과

| API | TPS | p95 Latency | CPU (%) | Memory (%) | Partitions Scanned |
|-----|-----|-------------|---------|------------|--------------------|
| API 1 (파티셔닝 X) | ... | ... | ... | ... | N/A |
| API 2 (파티셔닝 O) | ... | ... | ... | ... | p2024 |
| API 3 (키 X) | ... | ... | ... | ... | p2020,p2021,... |
| API 4 (키 O) | ... | ... | ... | ... | p2024 |
```

## 유용한 명령어

```bash
# Docker
docker-compose up -d          # 시작
docker-compose down          # 중지
docker-compose down -v       # 완전 삭제 (볼륨 포함)
docker-compose logs -f mysql # 로그 확인

# MySQL 접속
docker-compose exec mysql mysql -u root -proot partition_test

# 데이터 확인
docker-compose exec mysql mysql -u root -proot partition_test -e "
SELECT 'product' AS table_name, COUNT(*) AS row_count FROM product
UNION ALL
SELECT 'product_partitioned', COUNT(*) FROM product_partitioned
UNION ALL
SELECT 'comment', COUNT(*) FROM comment;"

# 파티션 정보
docker-compose exec mysql mysql -u root -proot partition_test -e "
SELECT TABLE_NAME, PARTITION_NAME, TABLE_ROWS
FROM INFORMATION_SCHEMA.PARTITIONS
WHERE TABLE_SCHEMA = 'partition_test'
  AND TABLE_NAME IN ('product_partitioned', 'comment')
ORDER BY TABLE_NAME, PARTITION_ORDINAL_POSITION;"
```

## 기술 스택

- **언어**: Kotlin 2.2.21
- **프레임워크**: Spring Boot 4.0.1
- **데이터베이스**: MySQL 8.0
- **ORM**: Spring Data JPA
- **부하 테스트**: k6
- **컨테이너**: Docker & Docker Compose

## 참고 자료

- [API.md](./API.md) - REST API 상세 문서
- [docker/README.md](./docker/README.md) - Docker 가이드
- [mysql-partition.md](./mysql-partition.md) - 프로젝트 목표 및 결과

## 라이센스

MIT License
