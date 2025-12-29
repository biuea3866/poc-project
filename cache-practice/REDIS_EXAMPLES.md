# Redis 예제 가이드

REDIS.md의 학습 내용을 기반으로 실습할 수 있는 4가지 Redis 예제 모음입니다.

## 사전 준비

### 1. Redis 서버 실행

```bash
# Docker를 이용한 Redis 실행
docker run -d -p 6379:6379 --name redis redis:latest

# 또는 로컬 Redis 실행
redis-server
```

### 2. 애플리케이션 실행

```bash
# redis 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=redis'
```

## 패키지 구조

```
redis/
├── concurrency/          # 1. 레디스 동시성 문제
│   ├── RedisConcurrencyService.kt
│   └── RedisConcurrencyController.kt
│
├── caching/              # 2. 데이터 캐싱
│   ├── RedisCachingService.kt
│   └── RedisCachingController.kt
│
├── distributedlock/      # 3. 분산락
│   ├── RedisDistributedLockService.kt
│   └── RedisDistributedLockController.kt
│
└── messagequeue/         # 4. 메시지 큐
    ├── RedisMessagePublisher.kt
    ├── RedisMessageSubscriber.kt
    ├── RedisMessageQueueService.kt
    └── RedisMessageQueueController.kt
```

---

## 1. 레디스 동시성 문제

Redis는 하나의 요청을 원자적으로 수행하지만, 여러 요청에 걸친 작업은 데이터 정합성 문제가 발생할 수 있습니다.

### 예제 구현

#### MULTI/EXEC - 트랜잭션
여러 명령어를 하나의 트랜잭션으로 묶어 순차 실행합니다.

```bash
curl -X POST http://localhost:8080/api/redis/concurrency/multi-exec \
  -H "Content-Type: application/json" \
  -d '{
    "key1": "user:name",
    "key2": "user:email",
    "value1": "홍길동",
    "value2": "hong@example.com"
  }'
```

#### WATCH - 낙관적 락
키 변경을 감지하여 트랜잭션 충돌을 방지합니다.

```bash
curl -X POST http://localhost:8080/api/redis/concurrency/watch \
  -H "Content-Type: application/json" \
  -d '{
    "balanceKey": "account:balance",
    "initialBalance": 10000,
    "decrementAmount": 3000
  }'
```

#### Lua Script - 원자적 스크립트 실행
재고 차감을 원자적으로 실행합니다.

```bash
curl -X POST http://localhost:8080/api/redis/concurrency/lua-script \
  -H "Content-Type: application/json" \
  -d '{
    "stockKey": "product:stock:1",
    "initialStock": 100,
    "quantity": 5
  }'
```

#### Lua Script - 잔액 이체
여러 키를 원자적으로 업데이트합니다.

```bash
curl -X POST http://localhost:8080/api/redis/concurrency/lua-transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromKey": "account:A",
    "toKey": "account:B",
    "fromBalance": 10000,
    "toBalance": 5000,
    "amount": 3000
  }'
```

---

## 2. 데이터 캐싱

Redis의 in-memory 특성을 활용한 고성능 캐싱 전략 예제입니다.

### 캐싱 패턴

#### Look-Aside (Cache-Aside)
캐시 우선 조회, 미스 시 DB 조회 후 캐싱합니다.

```bash
# 첫 번째 요청 (Cache Miss - DB 조회)
curl http://localhost:8080/api/redis/caching/look-aside/1?ttl=60

# 두 번째 요청 (Cache Hit - Redis 조회)
curl http://localhost:8080/api/redis/caching/look-aside/1?ttl=60
```

#### Write-Through
캐시와 DB를 동시에 업데이트합니다.

```bash
curl -X POST http://localhost:8080/api/redis/caching/write-through \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com",
    "age": 30
  }'
```

#### Write-Behind
캐시에 먼저 쓰고, 나중에 비동기로 DB 업데이트합니다.

```bash
curl -X POST http://localhost:8080/api/redis/caching/write-behind \
  -H "Content-Type: application/json" \
  -d '{
    "id": 2,
    "name": "김철수",
    "email": "kim@example.com",
    "age": 25
  }'
```

### Redis 자료구조 활용

#### Hash - 여러 필드를 하나의 키에 저장

```bash
# Hash 저장
curl -X POST http://localhost:8080/api/redis/caching/hash \
  -H "Content-Type: application/json" \
  -d '{
    "id": 100,
    "name": "이영희",
    "email": "lee@example.com",
    "age": 28
  }'

# Hash 조회
curl http://localhost:8080/api/redis/caching/hash/100
```

#### List - 최근 조회 항목 관리

```bash
# 최근 조회 항목 추가
curl -X POST http://localhost:8080/api/redis/caching/recent-view \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 101,
    "maxSize": 10
  }'

# 최근 조회 항목 조회
curl http://localhost:8080/api/redis/caching/recent-view/1
```

#### Sorted Set - 랭킹 시스템

```bash
# 랭킹 업데이트
curl -X POST http://localhost:8080/api/redis/caching/ranking \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "score": 95.5
  }'

# 상위 10개 조회
curl http://localhost:8080/api/redis/caching/ranking/top/10
```

---

## 3. 분산락

여러 인스턴스 환경에서 동시성 제어를 위한 분산락 예제입니다.

### Redisson 분산락 종류

#### Basic Lock - 기본 분산락

```bash
curl -X POST http://localhost:8080/api/redis/distributed-lock/basic \
  -H "Content-Type: application/json" \
  -d '{
    "lockKey": "order:123",
    "waitTime": 5,
    "leaseTime": 10,
    "taskDuration": 2000
  }'
```

#### Fair Lock - 공정한 락 (FIFO)

```bash
curl -X POST http://localhost:8080/api/redis/distributed-lock/fair \
  -H "Content-Type: application/json" \
  -d '{
    "lockKey": "payment:456",
    "waitTime": 5,
    "leaseTime": 10,
    "taskDuration": 2000
  }'
```

#### Read Lock - 읽기 락

```bash
curl -X POST http://localhost:8080/api/redis/distributed-lock/read \
  -H "Content-Type: application/json" \
  -d '{
    "lockKey": "product:789",
    "taskDuration": 1000
  }'
```

#### Write Lock - 쓰기 락

```bash
curl -X POST http://localhost:8080/api/redis/distributed-lock/write \
  -H "Content-Type: application/json" \
  -d '{
    "lockKey": "product:789",
    "taskDuration": 2000
  }'
```

### 실전 예제: 재고 차감

```bash
# 1. 재고 초기화
curl -X POST http://localhost:8080/api/redis/distributed-lock/stock/init \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "stock": 100
  }'

# 2. 재고 차감 (동시 요청 시 분산락으로 순차 처리)
curl -X POST http://localhost:8080/api/redis/distributed-lock/stock/decrease \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 5
  }'

# 3. 현재 재고 조회
curl http://localhost:8080/api/redis/distributed-lock/stock/1
```

---

## 4. 메시지 큐 (Pub/Sub)

Redis Pub/Sub를 이용한 실시간 메시징 시스템 예제입니다.

### 기본 Pub/Sub

```bash
# 1. 채널 구독
curl -X POST http://localhost:8080/api/redis/message-queue/subscribe \
  -H "Content-Type: application/json" \
  -d '{"channel": "test-channel"}'

# 2. 메시지 발행
curl -X POST http://localhost:8080/api/redis/message-queue/publish \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "test-channel",
    "message": "Hello Redis Pub/Sub!"
  }'

# 3. 수신한 메시지 조회
curl http://localhost:8080/api/redis/message-queue/received/test-channel
```

### 채팅 시스템

```bash
# 1. 채팅방 구독
curl -X POST http://localhost:8080/api/redis/message-queue/chat/subscribe \
  -H "Content-Type: application/json" \
  -d '{"roomId": "room-1"}'

# 2. 채팅 메시지 전송
curl -X POST http://localhost:8080/api/redis/message-queue/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": "room-1",
    "senderId": 1,
    "senderName": "홍길동",
    "message": "안녕하세요!"
  }'

# 3. 채팅 메시지 조회
curl http://localhost:8080/api/redis/message-queue/chat/room-1
```

### 알림 시스템

```bash
# 1. 사용자 알림 구독
curl -X POST http://localhost:8080/api/redis/message-queue/notification/subscribe \
  -H "Content-Type: application/json" \
  -d '{"userId": 1}'

# 2. 알림 발행
curl -X POST http://localhost:8080/api/redis/message-queue/notification/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "type": "ORDER",
    "title": "주문 완료",
    "message": "주문이 성공적으로 완료되었습니다."
  }'

# 3. 알림 조회
curl http://localhost:8080/api/redis/message-queue/notification/1
```

### 이벤트 시스템

```bash
# 1. 이벤트 구독
curl -X POST http://localhost:8080/api/redis/message-queue/event/subscribe \
  -H "Content-Type: application/json" \
  -d '{"eventType": "USER_REGISTERED"}'

# 2. 이벤트 발행
curl -X POST http://localhost:8080/api/redis/message-queue/event/publish \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "USER_REGISTERED",
    "data": {
      "userId": 123,
      "email": "user@example.com",
      "timestamp": 1234567890
    }
  }'

# 3. 이벤트 조회
curl http://localhost:8080/api/redis/message-queue/event/USER_REGISTERED
```

---

## 주요 학습 포인트

### 1. 동시성 문제
- **MULTI/EXEC**: 순차 실행으로 원자성 보장
- **WATCH**: 낙관적 락으로 충돌 감지
- **Lua Script**: 서버 측 원자적 실행

### 2. 데이터 캐싱
- **Look-Aside**: 가장 일반적인 캐싱 패턴
- **Write-Through**: 데이터 일관성 중시
- **Write-Behind**: 쓰기 성능 최적화
- **자료구조 활용**: Hash, List, Sorted Set 등

### 3. 분산락
- **Basic Lock**: 간단한 배타적 락
- **Fair Lock**: 공정한 순서 보장
- **ReadWrite Lock**: 읽기/쓰기 분리
- **실전 응용**: 재고 차감, 결제 처리 등

### 4. 메시지 큐
- **Pub/Sub 패턴**: 비동기 메시징
- **실시간 통신**: 채팅, 알림, 이벤트
- **제약사항**: 메시지 영구 보관 X, ACK X

---

## Redis Pub/Sub의 특징과 한계

### 장점
- 간단한 구조로 빠른 구현
- 실시간 메시징에 적합
- 채팅, 알림 등에 활용

### 한계
- **메시지 유실 가능**: 구독자가 없으면 메시지 소실
- **ACK 없음**: 메시지 수신 확인 불가
- **재발행 불가**: 메시지 리플레이 불가능
- **영속성 없음**: 메시지를 영구 저장하지 않음

### 대안
메시지 신뢰성이 중요한 경우 Kafka, RabbitMQ 등 사용 권장

---

## 테스트 팁

1. **동시성 테스트**: 여러 터미널에서 동시에 API 호출
2. **TTL 확인**: Redis CLI로 키 만료 시간 확인
   ```bash
   redis-cli
   TTL user:1
   ```
3. **메시지 수신 확인**: 구독 먼저, 발행은 나중에
4. **분산락 테스트**: 긴 작업 시간 설정 후 동시 요청
