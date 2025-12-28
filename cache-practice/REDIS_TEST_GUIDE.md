# Redis E2E 테스트 가이드

Testcontainers를 이용한 Redis 예제 통합 테스트입니다.

## 테스트 개요

모든 테스트는 실제 Redis 컨테이너를 사용하여 실행되므로, 로컬 환경에 Redis 설치 없이도 테스트가 가능합니다.

### 테스트 구조

```
redis/
├── RedisTestBase.kt                        # 공통 테스트 베이스 클래스
├── concurrency/
│   └── RedisConcurrencyE2ETest.kt          # 동시성 문제 테스트
├── caching/
│   └── RedisCachingE2ETest.kt              # 데이터 캐싱 테스트
├── distributedlock/
│   └── RedisDistributedLockE2ETest.kt      # 분산락 테스트
└── messagequeue/
    └── RedisMessageQueueE2ETest.kt         # 메시지 큐 테스트
```

## 사전 요구사항

1. **Docker 실행**: Testcontainers가 Docker를 사용하여 Redis 컨테이너를 실행합니다.
   ```bash
   # Docker가 실행 중인지 확인
   docker ps
   ```

2. **Gradle 빌드 도구**: 테스트 실행을 위한 Gradle이 필요합니다.

## 테스트 실행 방법

### 1. 전체 Redis 테스트 실행

```bash
./gradlew test --tests "com.example.cachepractice.redis.*"
```

### 2. 특정 테스트 클래스 실행

#### 동시성 테스트
```bash
./gradlew test --tests "com.example.cachepractice.redis.concurrency.RedisConcurrencyE2ETest"
```

#### 캐싱 테스트
```bash
./gradlew test --tests "com.example.cachepractice.redis.caching.RedisCachingE2ETest"
```

#### 분산락 테스트
```bash
./gradlew test --tests "com.example.cachepractice.redis.distributedlock.RedisDistributedLockE2ETest"
```

#### 메시지 큐 테스트
```bash
./gradlew test --tests "com.example.cachepractice.redis.messagequeue.RedisMessageQueueE2ETest"
```

### 3. 특정 테스트 메서드 실행

```bash
./gradlew test --tests "com.example.cachepractice.redis.concurrency.RedisConcurrencyE2ETest.testLuaScriptConcurrency"
```

## 테스트 상세 내용

### 1. RedisConcurrencyE2ETest - 동시성 문제 테스트

**테스트 케이스:**
- ✅ MULTI/EXEC - 여러 명령어를 하나의 트랜잭션으로 실행
- ✅ WATCH - 낙관적 락을 이용한 트랜잭션
- ✅ WATCH - 잔액 부족 시 실패
- ✅ Lua Script - 재고 차감 성공
- ✅ Lua Script - 재고 부족 시 실패
- ✅ Lua Script - 잔액 이체 성공
- ✅ Lua Script - 동시성 테스트: 재고 차감 (10 스레드, 재고 정합성 검증)
- ✅ WATCH - 동시성 테스트: 여러 스레드가 동시에 잔액 차감 (20 스레드)
- ✅ Lua Script - 이체 동시성 테스트 (10 스레드, 전체 잔액 보존 검증)

**핵심 검증 사항:**
- 원자성 보장
- 동시 요청 시 데이터 정합성
- 재고/잔액 부족 시 적절한 실패 처리

### 2. RedisCachingE2ETest - 데이터 캐싱 테스트

**테스트 케이스:**
- ✅ Look-Aside - 첫 요청은 Cache Miss, 두 번째 요청은 Cache Hit
- ✅ Write-Through - 캐시와 DB를 동시에 업데이트
- ✅ Write-Behind - 캐시에 먼저 저장
- ✅ Cache Invalidation - 캐시 무효화
- ✅ Hash - 여러 필드를 하나의 키에 저장
- ✅ Hash - 존재하지 않는 사용자 조회
- ✅ List - 최근 조회 항목 관리
- ✅ List - 최대 크기 유지
- ✅ Sorted Set - 랭킹 시스템
- ✅ Sorted Set - 랭킹 업데이트
- ✅ TTL - 캐시 만료 확인
- ✅ Look-Aside - 성능 비교 (Cache Hit vs Cache Miss)

**핵심 검증 사항:**
- Cache Hit/Miss 동작
- 캐싱 패턴별 동작 방식
- Redis 자료구조 활용
- TTL을 통한 자동 만료

### 3. RedisDistributedLockE2ETest - 분산락 테스트

**테스트 케이스:**
- ✅ Basic Lock - 락 획득 및 작업 실행
- ✅ Basic Lock - 락 대기 시간 초과
- ✅ Fair Lock - 요청 순서대로 락 획득
- ✅ Read Lock - 여러 스레드가 동시에 읽기 가능
- ✅ Write Lock - 배타적 접근만 가능
- ✅ 재고 차감 - 단일 요청
- ✅ 재고 차감 - 재고 부족
- ✅ 재고 차감 - 동시성 테스트 (20 스레드, 데이터 정합성 검증)
- ✅ 재고 차감 - 높은 동시성 테스트 (100 스레드)
- ✅ 재고 차감 - 경쟁 조건에서 데이터 정합성 검증 (50 스레드)
- ✅ 락 재진입 테스트

**핵심 검증 사항:**
- 분산 환경에서 동시성 제어
- 락 타입별 특징 (Basic, Fair, ReadWrite)
- 재고 차감 시나리오에서 데이터 정합성
- 높은 동시성 환경에서 안정성

### 4. RedisMessageQueueE2ETest - 메시지 큐 테스트

**테스트 케이스:**
- ✅ 기본 Pub/Sub - 메시지 발행 및 수신
- ✅ 채팅 - 채팅방 구독 및 메시지 전송
- ✅ 채팅 - 여러 메시지 전송
- ✅ 알림 - 사용자 알림 발행 및 수신
- ✅ 알림 - 여러 사용자에게 알림 발송
- ✅ 이벤트 - 이벤트 발행 및 구독
- ✅ 이벤트 - 다양한 이벤트 타입
- ✅ Pub/Sub - 구독 전 발행된 메시지는 수신 불가 (메시지 유실 특성 검증)
- ✅ 채팅 - 동일 채팅방에 여러 사용자
- ✅ 알림 - 동일 사용자에게 여러 타입의 알림
- ✅ 메시지 초기화 테스트
- ✅ 이벤트 - 이벤트 기반 아키텍처 시뮬레이션
- ✅ 성능 테스트 - 다수의 메시지 발행 (100개)

**핵심 검증 사항:**
- Pub/Sub 패턴 동작
- 채팅, 알림, 이벤트 시스템 구현
- 메시지 유실 특성 이해
- 다수 메시지 처리 성능

## Testcontainers 동작 방식

### RedisTestBase 클래스

모든 테스트의 베이스 클래스로, Redis 컨테이너를 자동으로 관리합니다.

```kotlin
@Container
val redisContainer: RedisContainer = RedisContainer(
    DockerImageName.parse("redis:7.2-alpine")
)
```

### 특징

1. **자동 컨테이너 관리**: 테스트 실행 시 Redis 컨테이너 자동 시작/종료
2. **격리된 환경**: 각 테스트는 독립적인 Redis 인스턴스 사용
3. **동적 포트 할당**: 포트 충돌 방지
4. **실제 환경과 동일**: 실제 Redis를 사용하므로 신뢰성 높은 테스트

## 테스트 실행 결과 예시

```
RedisConcurrencyE2ETest
✓ MULTI/EXEC - 여러 명령어를 하나의 트랜잭션으로 실행
✓ WATCH - 낙관적 락을 이용한 트랜잭션
✓ Lua Script - 동시성 테스트: 재고 차감
  - Success: 20, Fail: 0
  - Final Stock: 0

RedisDistributedLockE2ETest
✓ 재고 차감 - 동시성 테스트
  - Success: 20, Fail: 0, Final Stock: 0
✓ 재고 차감 - 높은 동시성 테스트
  - High Concurrency Test - Duration: 523ms
  - Success: 100, Final Stock: 900

RedisCachingE2ETest
✓ Look-Aside - 성능 비교
  - Cache Miss Duration: 105ms
  - Avg Cache Hit Duration: 2.4ms

RedisMessageQueueE2ETest
✓ 성능 테스트 - 다수의 메시지 발행
  - Published 100 messages in 45ms
  - Received 100 messages
```

## 주의사항

1. **Docker 필수**: Testcontainers는 Docker를 사용하므로 Docker가 실행 중이어야 합니다.
2. **테스트 시간**: 컨테이너 시작으로 인해 첫 테스트는 시간이 더 걸릴 수 있습니다.
3. **리소스 정리**: 테스트 후 Redis 데이터는 자동으로 정리됩니다 (`@AfterEach cleanup()`).
4. **메시지 큐 테스트**: Pub/Sub 특성상 비동기 처리를 위해 `Thread.sleep()` 사용.

## 트러블슈팅

### Docker 연결 오류
```
Could not find a valid Docker environment
```
**해결 방법**: Docker Desktop이 실행 중인지 확인하세요.

### 포트 충돌
```
Port 6379 is already in use
```
**해결 방법**: Testcontainers는 자동으로 다른 포트를 할당하므로, 로컬 Redis가 실행 중이어도 문제없습니다.

### 테스트 타임아웃
```
Test timed out after 60000 milliseconds
```
**해결 방법**: Docker 리소스를 늘리거나, 동시 실행 스레드 수를 줄이세요.

## 성능 벤치마크

테스트를 통해 다음과 같은 성능 특성을 확인할 수 있습니다:

- **Cache Hit vs Miss**: Cache Hit가 약 40~50배 빠름
- **분산락 처리량**: 100개 동시 요청을 ~500ms 내 처리
- **메시지 발행**: 100개 메시지를 ~50ms 내 발행
- **동시성 제어**: Lua Script/분산락으로 완벽한 데이터 정합성 보장

## 추가 테스트 작성 가이드

새로운 테스트를 작성할 때는 다음 패턴을 따르세요:

```kotlin
@DisplayName("테스트 설명")
class MyRedisE2ETest : RedisTestBase() {

    @Autowired
    private lateinit var myService: MyService

    @AfterEach
    fun cleanup() {
        // Redis 데이터 정리
    }

    @Test
    @DisplayName("테스트 케이스 설명")
    fun testMyFeature() {
        // given
        // when
        // then
    }
}
```

---

## 참고 자료

- [Testcontainers 공식 문서](https://testcontainers.com/)
- [Redis Testcontainers](https://github.com/testcontainers/testcontainers-java/tree/main/modules/redis)
- [Kotest Assertions](https://kotest.io/docs/assertions/assertions.html)
