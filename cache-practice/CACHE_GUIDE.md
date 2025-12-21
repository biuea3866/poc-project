# Spring 로컬 캐시 가이드

## 개요

이 프로젝트는 Spring Boot에서 로컬 캐시를 효과적으로 사용하는 방법을 시연합니다.
Caffeine과 EHCache 두 가지 캐시 구현을 제공하며, 일반적인 캐싱 문제와 해결 방법을 보여줍니다.

## 주요 기능

### 1. 캐싱 전략

#### Look Aside (읽기 전략)
- `@Cacheable` 어노테이션 사용
- 캐시 미스 시 DB 조회 후 캐시에 저장
- 가장 일반적인 읽기 패턴

#### Write Through (쓰기 전략)
- `@CachePut` 어노테이션 사용
- DB 업데이트 후 캐시도 갱신
- 캐시-DB 일관성 유지

#### Invalidate (무효화 전략)
- `@CacheEvict` 어노테이션 사용
- DB에서 삭제 후 캐시에서도 제거
- 불필요한 캐시 항목 제거

### 2. 캐싱 문제 해결

#### 캐시 스템피드 (Cache Stampede)
**문제**: 인기 있는 캐시 항목이 만료될 때 대량의 동시 요청이 DB로 몰리는 현상

**해결 방법**:
- Caffeine의 `get(key, mappingFunction)` 사용
- 동일 키에 대한 동시 요청 시 하나의 스레드만 DB 접근
- 나머지 스레드는 결과를 공유

**코드 예제**:
```kotlin
// CaffeineDirectService.kt
val product = cache.get(id) { key ->
    // 여러 스레드가 동시 호출해도 이 블록은 한 번만 실행됨
    productRepository.findById(key) ?: Product.NOT_FOUND
}
```

#### 캐시 관통 (Cache Penetration)
**문제**: 존재하지 않는 데이터를 반복 조회하여 매번 DB에 접근하는 현상

**해결 방법**:
- 센티널 객체 (`Product.NOT_FOUND`) 캐싱
- 존재하지 않는 ID에 대해서도 캐시 저장
- 반복적인 DB 조회 방지

**코드 예제**:
```kotlin
// ProductService.kt
@Cacheable(value = ["products"], key = "#id")
fun getProductById(id: Long): Product {
    val product = productRepository.findById(id)

    return if (product != null) {
        product
    } else {
        Product.NOT_FOUND // 센티널 객체 캐싱
    }
}
```

## 캐시 구현 비교

### Caffeine vs EHCache

| 특징 | Caffeine | EHCache |
|------|----------|---------|
| 제거 정책 | Window TinyLfu (고급) | LRU, LFU 등 |
| 적중률 | 매우 높음 | 높음 |
| 성능 | 매우 빠름 | 빠름 |
| 메모리 계층 | 힙 메모리만 | 힙, 오프힙, 디스크 |
| 분산 캐시 | 미지원 | 지원 (Terracotta) |
| 표준 준수 | 자체 API | JSR-107 (JCache) |
| 통계 | 내장 지원 | 설정 필요 |

### 언제 무엇을 사용할까?

**Caffeine을 선택하는 경우**:
- 단일 서버 환경
- 최고의 적중률과 성능이 필요
- 간단한 설정을 원함
- 메모리 사용량이 적당함

**EHCache를 선택하는 경우**:
- 오프힙 메모리나 디스크 캐시가 필요
- 분산 캐시 환경 (Terracotta 서버 사용)
- JSR-107 표준 준수가 중요
- 기존 EHCache 인프라가 있음

## 사용 방법

### 1. 프로파일 설정

#### Caffeine 사용
```bash
# application.yml에서 기본 설정
spring.profiles.active=caffeine

# 또는 실행 시 지정
./gradlew bootRun --args='--spring.profiles.active=caffeine'
```

#### EHCache 사용
```bash
./gradlew bootRun --args='--spring.profiles.active=ehcache'
```

### 2. API 엔드포인트

#### 기본 CRUD
```bash
# 제품 조회 (Look Aside)
curl http://localhost:8080/api/products/1

# 모든 제품 조회
curl http://localhost:8080/api/products

# 제품 생성 (Write Through)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "New Product", "price": 99.99}'

# 제품 수정 (Write Through)
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Product", "price": 199.99}'

# 제품 삭제 (Invalidate)
curl -X DELETE http://localhost:8080/api/products/1
```

#### 캐시 스템피드 테스트
```bash
# Caffeine 직접 사용 (스템피드 방지)
curl http://localhost:8080/api/products/stampede-test/1
```

#### 캐시 통계 및 관리
```bash
# 캐시 통계 조회 (Caffeine만)
curl http://localhost:8080/api/products/cache/stats

# 캐시 초기화
curl -X DELETE http://localhost:8080/api/products/cache
```

### 3. 모니터링

#### Actuator 엔드포인트
```bash
# 애플리케이션 상태
curl http://localhost:8080/actuator/health

# 캐시 정보
curl http://localhost:8080/actuator/caches

# 메트릭
curl http://localhost:8080/actuator/metrics

# Prometheus 메트릭
curl http://localhost:8080/actuator/prometheus
```

#### 캐시 메트릭 확인
```bash
# 캐시 적중률
curl http://localhost:8080/actuator/metrics/cache.gets

# 캐시 크기
curl http://localhost:8080/actuator/metrics/cache.size
```

## 테스트 실행

### 전체 테스트
```bash
./gradlew test
```

### 특정 테스트만 실행
```bash
# 기본 캐시 동작 테스트
./gradlew test --tests ProductServiceTest

# 캐시 스템피드 테스트
./gradlew test --tests CacheStampedeTest
```

## 성능 측정 예시

### 캐시 적중 vs 미스
```
첫 번째 조회 (캐시 미스): 105ms (DB 조회 시간 포함)
두 번째 조회 (캐시 적중): 1ms (캐시에서 반환)

성능 향상: 약 100배
```

### 캐시 스템피드 방지
```
시나리오: 10개 스레드가 동시에 같은 키 요청

캐시 스템피드 방지 없음:
- 10번의 DB 조회 발생
- 총 소요 시간: ~1000ms

Caffeine 스템피드 방지:
- 1번의 DB 조회만 발생
- 총 소요 시간: ~120ms
- 성능 향상: 약 8배
```

## 아키텍처

```
┌─────────────────┐
│   Controller    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Service     │ ◄─── @Cacheable, @CachePut, @CacheEvict
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Repository    │
└─────────────────┘

Cache Layer:
┌──────────────────────────────────┐
│  Caffeine / EHCache              │
│  - Look Aside (Read)             │
│  - Write Through / Invalidate    │
│  - Stampede Protection           │
│  - Penetration Prevention        │
└──────────────────────────────────┘
```

## 프로젝트 구조

```
src/main/kotlin/com/example/cachepractice/
├── config/
│   ├── CaffeineCacheConfig.kt    # Caffeine 설정
│   └── EHCacheConfig.kt          # EHCache 설정
├── controller/
│   └── ProductController.kt      # REST API
├── domain/
│   └── Product.kt                # 도메인 모델
├── repository/
│   └── ProductRepository.kt      # 데이터 저장소
└── service/
    ├── ProductService.kt         # Spring 캐싱 어노테이션 사용
    └── CaffeineDirectService.kt  # Caffeine API 직접 사용

src/test/kotlin/com/example/cachepractice/
└── service/
    ├── ProductServiceTest.kt     # 기본 캐시 테스트
    └── CacheStampedeTest.kt      # 스템피드 테스트
```

## 학습 포인트

1. **Look Aside 패턴**: 가장 일반적인 캐싱 전략
2. **Write Through vs Invalidate**: 데이터 일관성 유지 방법
3. **캐시 스템피드**: 동시성 문제와 해결 방법
4. **캐시 관통**: 존재하지 않는 데이터의 반복 조회 방지
5. **Caffeine vs EHCache**: 각 구현의 특징과 선택 기준
6. **캐시 모니터링**: Actuator를 통한 성능 추적

## 참고 자료

- [Caffeine GitHub](https://github.com/ben-manes/caffeine)
- [EHCache Documentation](https://www.ehcache.org/)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
