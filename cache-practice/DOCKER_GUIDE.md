# Docker Compose 가이드

MySQL과 Redis를 Docker Compose로 실행하는 가이드입니다.

## 사전 준비

Docker와 Docker Compose가 설치되어 있어야 합니다.

```bash
# Docker 버전 확인
docker --version
docker-compose --version
```

## 디렉토리 구조

```
cache-practice/
├── docker-compose.yml
├── .env.example
├── docker/
│   ├── mysql/
│   │   └── init/
│   │       └── 01-schema.sql
│   └── redis/
│       └── redis.conf
```

## 실행 방법

### 1. 컨테이너 시작

```bash
# 포그라운드 실행 (로그 확인)
docker-compose up

# 백그라운드 실행
docker-compose up -d
```

### 2. 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker-compose ps

# 로그 확인
docker-compose logs

# 특정 서비스 로그 확인
docker-compose logs mysql
docker-compose logs redis

# 실시간 로그 확인
docker-compose logs -f
```

### 3. 컨테이너 중지

```bash
# 컨테이너 중지 (데이터는 유지)
docker-compose stop

# 컨테이너 중지 및 삭제 (데이터는 유지)
docker-compose down

# 컨테이너 및 볼륨 모두 삭제 (데이터도 삭제)
docker-compose down -v
```

### 4. 컨테이너 재시작

```bash
# 모든 서비스 재시작
docker-compose restart

# 특정 서비스만 재시작
docker-compose restart mysql
docker-compose restart redis
```

## 서비스 정보

### MySQL

- **포트**: 3306
- **데이터베이스**: cache_practice
- **Root 비밀번호**: root
- **사용자**: cache_user
- **비밀번호**: cache_password

#### 접속 방법

```bash
# MySQL CLI 접속
docker-compose exec mysql mysql -uroot -proot cache_practice

# 또는 외부에서 접속
mysql -h 127.0.0.1 -P 3306 -ucache_user -pcache_password cache_practice
```

#### 유용한 MySQL 명령어

```sql
-- 테이블 목록 확인
SHOW TABLES;

-- Product 테이블 조회
SELECT * FROM product;

-- User 테이블 조회
SELECT * FROM user;

-- 데이터베이스 상태 확인
SHOW STATUS;
```

### Redis

- **포트**: 6379
- **데이터 영속성**: AOF + RDB
- **최대 메모리**: 256MB
- **메모리 정책**: allkeys-lru

#### 접속 방법

```bash
# Redis CLI 접속
docker-compose exec redis redis-cli

# 또는 외부에서 접속
redis-cli -h 127.0.0.1 -p 6379
```

#### 유용한 Redis 명령어

```bash
# 연결 테스트
PING

# 모든 키 조회
KEYS *

# 키 개수 확인
DBSIZE

# 특정 키 조회
GET user:1

# 특정 키 삭제
DEL user:1

# 모든 데이터 삭제
FLUSHALL

# Redis 정보 확인
INFO

# 메모리 사용량 확인
INFO memory
```

## 데이터 초기화

### MySQL 데이터 초기화

컨테이너 시작 시 `docker/mysql/init/01-schema.sql` 스크립트가 자동 실행됩니다.

데이터를 재초기화하려면:

```bash
# 1. 볼륨 포함 삭제
docker-compose down -v

# 2. 다시 시작 (초기화 스크립트 재실행)
docker-compose up -d
```

### Redis 데이터 초기화

```bash
# Redis CLI에서
docker-compose exec redis redis-cli FLUSHALL

# 또는 컨테이너 재시작
docker-compose restart redis
```

## 볼륨 관리

### 볼륨 확인

```bash
# 볼륨 목록
docker volume ls | grep cache-practice

# 볼륨 상세 정보
docker volume inspect cache-practice_mysql-data
docker volume inspect cache-practice_redis-data
```

### 볼륨 백업

```bash
# MySQL 데이터 백업
docker-compose exec mysql mysqldump -uroot -proot cache_practice > backup.sql

# Redis 데이터 백업
docker-compose exec redis redis-cli SAVE
docker cp cache-practice-redis:/data/dump.rdb ./redis-backup.rdb
```

### 볼륨 복원

```bash
# MySQL 데이터 복원
docker-compose exec -T mysql mysql -uroot -proot cache_practice < backup.sql

# Redis 데이터 복원
docker cp ./redis-backup.rdb cache-practice-redis:/data/dump.rdb
docker-compose restart redis
```

## 애플리케이션 연동

### application.yml 설정 (Redis 프로파일)

```yaml
spring:
  profiles: redis
  datasource:
    url: jdbc:mysql://localhost:3306/cache_practice
    username: cache_user
    password: cache_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
```

### 애플리케이션 실행

```bash
# Redis 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=redis'
```

## 트러블슈팅

### 포트 충돌

**문제**: 3306 또는 6379 포트가 이미 사용 중

**해결**:
```bash
# 1. 기존 프로세스 확인
lsof -i :3306
lsof -i :6379

# 2. docker-compose.yml에서 포트 변경
# mysql:
#   ports:
#     - "3307:3306"  # 호스트 포트 변경
```

### 컨테이너 시작 실패

**문제**: 컨테이너가 시작되지 않음

**해결**:
```bash
# 1. 로그 확인
docker-compose logs mysql
docker-compose logs redis

# 2. 볼륨 삭제 후 재시작
docker-compose down -v
docker-compose up -d
```

### MySQL 연결 거부

**문제**: `Connection refused` 오류

**해결**:
```bash
# 1. 헬스체크 확인
docker-compose ps

# 2. MySQL이 완전히 시작될 때까지 대기 (30초 정도)
docker-compose logs -f mysql

# 3. 연결 테스트
docker-compose exec mysql mysqladmin -uroot -proot ping
```

### Redis 메모리 부족

**문제**: `OOM` 오류

**해결**:
```bash
# 1. redis.conf에서 maxmemory 증가
# maxmemory 512mb

# 2. 컨테이너 재시작
docker-compose restart redis
```

## 성능 모니터링

### MySQL 성능 확인

```bash
# 실행 중인 쿼리 확인
docker-compose exec mysql mysql -uroot -proot -e "SHOW PROCESSLIST;"

# Slow Query 확인
docker-compose exec mysql mysql -uroot -proot -e "SELECT * FROM information_schema.processlist WHERE time > 1;"
```

### Redis 성능 확인

```bash
# Redis 통계
docker-compose exec redis redis-cli INFO stats

# 느린 명령어 확인
docker-compose exec redis redis-cli SLOWLOG GET 10

# 실시간 모니터링
docker-compose exec redis redis-cli MONITOR
```

## 개발 팁

### 데이터 확인 도구

- **MySQL**: DBeaver, MySQL Workbench, DataGrip
- **Redis**: RedisInsight, Medis, Another Redis Desktop Manager

### 빠른 재시작

```bash
# 애플리케이션 개발 중 Redis만 재시작
docker-compose restart redis

# MySQL만 재시작
docker-compose restart mysql
```

### 로그 레벨 조정

MySQL의 경우 `docker-compose.yml`에서:
```yaml
environment:
  MYSQL_LOG_CONSOLE: 'true'
```

## 운영 환경 고려사항

**주의**: 이 설정은 개발 환경용입니다. 운영 환경에서는:

1. 강력한 비밀번호 사용
2. 네트워크 격리
3. 볼륨 백업 전략 수립
4. 리소스 제한 설정
5. TLS/SSL 적용
6. Redis 비밀번호 설정

---

## 요약

```bash
# 시작
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f

# 중지
docker-compose down

# 완전 삭제 (데이터 포함)
docker-compose down -v
```

이제 MySQL과 Redis가 Docker Compose로 실행됩니다! 🚀
