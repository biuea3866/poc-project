# Spring 로컬 캐시 실습 프로젝트

Spring Boot에서 로컬 캐시를 효과적으로 사용하는 방법을 시연하는 프로젝트입니다.

## 주요 특징

### 1. 두 가지 캐시 구현 비교
- **Caffeine**: Window TinyLfu 알고리즘, 고성능
- **EHCache**: JSR-107 표준, 다양한 메모리 계층 지원

### 2. 다양한 캐싱 전략 구현
- **Look Aside + Write Around**: 읽기 중심 워크로드에 적합
- **Read Through + Write Around**: 캐시 중심 설계
- **Read Through + Write Through**: 강한 일관성 필요 시

### 3. 캐싱 문제 해결 (5가지 방법)
- **캐시 스템피드 해결**:
  - Lock (Caffeine 자동 잠금)
  - 조기 갱신 (Scheduled Early Refresh)
  - PER (Probabilistic Early Recomputation)
- **캐시 관통 해결**:
  - 빈 값 캐싱 (센티널 객체)
  - 블룸 필터 (Bloom Filter)

### 4. 모니터링
- Spring Actuator를 통한 캐시 메트릭
- Prometheus 엔드포인트 제공
- 각 전략별 통계 API

## 빠른 시작

### 1. 빌드 및 테스트
```bash
./gradlew clean build
```

### 2. 애플리케이션 실행

#### Caffeine 사용 (기본)
```bash
./gradlew bootRun
```

#### EHCache 사용
```bash
./gradlew bootRun --args='--spring.profiles.active=ehcache'
```

## API 엔드포인트

### 기본 CRUD (기존)

```bash
# 제품 조회
curl http://localhost:8080/api/products/1

# 캐시 스템피드 방지 테스트
curl http://localhost:8080/api/products/stampede-test/1

# 캐시 통계 조회
curl http://localhost:8080/api/products/cache/stats
```

### 캐시 전략별 API

```bash
# 1. Look Aside + Write Around
curl http://localhost:8080/api/strategy/look-aside-write-around/1
curl -X POST http://localhost:8080/api/strategy/look-aside-write-around \
  -H "Content-Type: application/json" \
  -d '{"id": 10, "name": "Product", "price": 99.99}'

# 2. Read Through + Write Around
curl http://localhost:8080/api/strategy/read-through-write-around/1
curl http://localhost:8080/api/strategy/read-through-write-around/stats

# 3. Read Through + Write Through
curl http://localhost:8080/api/strategy/read-through-write-through/1
curl http://localhost:8080/api/strategy/read-through-write-through/stats
```

### 캐시 문제 해결 API

```bash
# 조기 갱신 (Early Refresh)
curl http://localhost:8080/api/problem/early-refresh/1
curl -X POST http://localhost:8080/api/problem/early-refresh/hotkey/1
curl http://localhost:8080/api/problem/early-refresh/stats

# PER (Probabilistic Early Recomputation)
curl http://localhost:8080/api/problem/per/1?beta=1.0
curl -X POST http://localhost:8080/api/problem/per/test-beta/1
curl http://localhost:8080/api/problem/per/stats

# 블룸 필터 (Bloom Filter)
curl http://localhost:8080/api/problem/bloom-filter/1
curl http://localhost:8080/api/problem/bloom-filter/9999  # 없는 ID (차단됨)
curl http://localhost:8080/api/problem/bloom-filter/stats
```

### 모니터링

```bash
# 애플리케이션 상태
curl http://localhost:8080/actuator/health

# 캐시 정보
curl http://localhost:8080/actuator/caches

# Prometheus 메트릭
curl http://localhost:8080/actuator/prometheus
```

## 프로젝트 구조

```
src/main/kotlin/com/example/cachepractice/
├── config/
│   ├── CaffeineCacheConfig.kt              # Caffeine 설정
│   └── EHCacheConfig.kt                    # EHCache 설정
├── controller/
│   ├── ProductController.kt                # 기본 REST API
│   └── CacheStrategyController.kt          # 전략/문제해결 API
├── domain/
│   └── Product.kt                          # 도메인 모델
├── repository/
│   └── ProductRepository.kt                # 인메모리 저장소
├── service/
│   ├── ProductService.kt                   # 기본 캐시 서비스
│   ├── CaffeineDirectService.kt            # Caffeine 직접 사용
│   ├── strategy/                           # 캐시 전략
│   │   ├── LookAsideWriteAroundService.kt
│   │   ├── ReadThroughWriteAroundService.kt
│   │   └── ReadThroughWriteThroughService.kt
│   └── problem/                            # 문제 해결
│       ├── EarlyRefreshService.kt          # 조기 갱신
│       ├── ProbabilisticEarlyRecomputationService.kt  # PER
│       └── BloomFilterService.kt           # 블룸 필터

src/test/kotlin/com/example/cachepractice/
└── service/
    ├── ProductServiceTest.kt               # 캐시 동작 테스트
    └── CacheStampedeTest.kt                # 스템피드 테스트
```

## 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests ProductServiceTest
./gradlew test --tests CacheStampedeTest
```

## 학습 포인트

1. **캐싱 전략**: Look Aside, Write Through, Invalidate
2. **캐시 스템피드**: 동시성 문제와 해결
3. **캐시 관통**: 존재하지 않는 데이터 처리
4. **Caffeine vs EHCache**: 각 구현의 장단점
5. **모니터링**: Actuator를 통한 성능 추적

## 성능 예시

### 캐시 적중 vs 미스
- 첫 번째 조회 (캐시 미스): ~105ms
- 두 번째 조회 (캐시 적중): ~1ms
- **성능 향상: 약 100배**

### 캐시 스템피드 방지
- 스템피드 방지 없음: 10번 DB 조회 (~1000ms)
- Caffeine 방지: 1번 DB 조회 (~120ms)
- **성능 향상: 약 8배**

## 참고 문서

자세한 내용은 [CACHE_GUIDE.md](CACHE_GUIDE.md)를 참고하세요.

## 기술 스택

- Kotlin 1.9.21
- Spring Boot 3.2.0
- Caffeine 3.1.8
- EHCache 3.10.8
- Spring Actuator + Prometheus

## 요구사항

- JDK 17 이상
- Gradle 8.x

## 라이선스

MIT License
