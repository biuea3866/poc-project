# Cache Strategy — AI Wiki Backend

## 1. 트래픽 패턴 분석

### Read Heavy vs Write Heavy

| 도메인 | Read 비율 | Write 비율 | 판단 |
|--------|----------|-----------|------|
| Document | 높음 (목록 조회, 상세 조회, 자식 문서 조회) | 낮음 (생성/수정은 상대적으로 드뭄) | **Read Heavy** |
| User | 매우 높음 (매 요청마다 JWT → UserDetails 조회) | 매우 낮음 (가입/탈퇴만) | **Read Heavy** |
| Tag | 높음 (문서 조회 시 태그도 함께 로드) | 낮음 (문서 생성 시에만 태그 저장) | **Read Heavy** |
| Auth Token | 중간 (블랙리스트 체크는 매 요청) | 중간 (로그인/로그아웃 시) | **Read Heavy** |

**결론:** 전체적으로 **Read Heavy** 워크로드. 위키 특성상 조회가 쓰기보다 압도적으로 많음.

### 핫 데이터 식별

1. **UserDetails (매 요청):** JWT 인증 필터에서 `UserService.findById()` 호출 → 모든 인증된 요청마다 DB 조회 발생
2. **문서 목록 (빈번 조회):** `findByCreatedByAndDeletedAtIsNull`, `findByParentIsNullAndDeletedAtIsNull` — 메인 페이지/대시보드 진입 시마다 호출
3. **문서 상세 (빈번 조회):** `findByIdAndDeletedAtIsNull` — 문서 열람 시마다 호출
4. **태그 (빈번 조회, 변경 드뭄):** 문서와 함께 조회되며 변경 빈도 매우 낮음

### 캐시 적합성 평가

| 대상 | 적합성 | 이유 |
|------|-------|------|
| UserDetails | ⭐⭐⭐⭐⭐ | 매 요청 조회, 변경 거의 없음, 캐시 효과 극대화 |
| 문서 목록 | ⭐⭐⭐⭐ | 빈번 조회, 쓰기 시 invalidation 필요하나 관리 가능 |
| 문서 상세 | ⭐⭐⭐⭐ | 자주 조회, 수정 시 eviction만 하면 됨 |
| 태그 목록 | ⭐⭐⭐⭐⭐ | 변경 빈도 극히 낮아 긴 TTL 적용 가능 |
| JWT 블랙리스트 | ⭐⭐⭐ | 이미 Redis 사용 중, 추가 로컬 캐시는 보안 트레이드오프 |

---

## 2. 로컬 캐시 전략 (Caffeine)

### 적용 대상

| 캐시 이름 | 대상 | TTL | 최대 크기 |
|----------|------|-----|----------|
| `userDetails` | `UserService.findById()` 결과 | 5분 | 1,000 entries |
| `documents` | 문서 상세 조회 결과 | 5분 | 500 entries |
| `documentList` | 문서 목록 조회 결과 | 3분 | 200 entries |
| `tags` | 태그 목록 | 30분 | 100 entries |

### 장점
- **밀리초 단위 응답:** 네트워크 왕복 없이 JVM 힙에서 직접 조회
- **DB 부하 감소:** 매 요청마다 발생하는 UserDetails 쿼리 제거
- **설정 간단:** Spring Cache + Caffeine으로 어노테이션 기반 적용

### 단점
- **멀티 인스턴스 불일치:** 서버 간 캐시 동기화 안 됨 (TTL 만료까지 stale 가능)
- **메모리 사용:** JVM 힙 메모리 소비 (1,000 entries 기준 수 MB 수준으로 미미)
- **Invalidation 한계:** 단일 인스턴스에서만 eviction 발생

### 적용 코드 예시

```kotlin
// UserService
@Cacheable(value = ["userDetails"], key = "#userId")
@Transactional(readOnly = true)
fun findById(userId: Long): User {
    return userRepository.findByIdAndDeletedAtIsNull(userId)
        ?: throw UserNotFoundException(userId)
}
```

---

## 3. Redis 분산 캐시 전략

### 캐시 키 설계

| 캐시 키 패턴 | 대상 | TTL | 비고 |
|-------------|------|-----|------|
| `documents:detail:{id}` | 문서 상세 | 10분 | 수정/삭제 시 evict |
| `documents:list:{userId}:{page}:{size}` | 사용자별 문서 목록 | 5분 | 문서 CUD 시 userId 기반 evict |
| `documents:root:{page}:{size}` | 루트 문서 목록 | 5분 | 문서 CUD 시 evict |
| `tags:all` | 전체 태그 목록 | 30분 | 변경 빈도 낮음 |
| `users:{id}` | 유저 정보 | 10분 | 프로필 수정/삭제 시 evict |

### Cache Invalidation 전략

```
문서 생성(save) → evict: documents:list:*, documents:root:*
문서 수정(update) → evict: documents:detail:{id}, documents:list:*, documents:root:*
문서 삭제(delete) → evict: documents:detail:{id}, documents:list:*, documents:root:*
유저 삭제(delete) → evict: users:{id}
태그 생성(save) → evict: tags:all
```

### Spring `@Cacheable` / `@CacheEvict` 적용 방안

```kotlin
// DocumentService - 조회
@Cacheable(value = ["documents"], key = "#id")
fun getDocument(id: Long): Document { ... }

@Cacheable(value = ["documentList"], key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
fun listDocuments(userId: Long, pageable: Pageable): Page<Document> { ... }

// DocumentService - 변경
@CacheEvict(value = ["documents", "documentList"], allEntries = true)
fun updateDocument(...): Document { ... }

@CacheEvict(value = ["documents", "documentList"], allEntries = true)
fun deleteDocument(...) { ... }
```

---

## 4. 2-Tier 캐시 구조

### 아키텍처

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Request    │────▶│  L1 Cache   │────▶│  L2 Cache   │────▶  DB (MySQL)
│              │     │  (Caffeine) │     │   (Redis)   │
│              │     │  ~0.1ms     │     │   ~1-2ms    │     │  ~5-20ms
└─────────────┘     └─────────────┘     └─────────────┘     └───────────┘
```

### 조회 흐름

1. **L1 Hit (Caffeine):** 로컬 힙 메모리에서 즉시 반환 → ~0.1ms
2. **L1 Miss → L2 Hit (Redis):** Redis에서 조회 후 L1에 캐싱 → ~1-2ms
3. **L1 Miss → L2 Miss → DB:** MySQL 쿼리 후 L1 + L2에 캐싱 → ~5-20ms

### 현재 단계 적용 방안

현재 싱글 인스턴스 운영이므로 **L1(Caffeine) 단독 적용**이 적절:
- L2(Redis) 캐시는 멀티 인스턴스 스케일아웃 시 추가
- Redis는 현재 Auth Token 저장에만 사용하고, 캐시 용도 확장은 스케일아웃 시점에 진행
- 2-Tier 구조는 `CacheManager` 체이닝으로 추후 확장 가능

---

## 5. 캐시 적용 전후 예상 성능

### DB 쿼리 감소율 예측

| 대상 | 캐시 전 (요청당 쿼리) | 캐시 후 (요청당 쿼리) | 감소율 |
|------|---------------------|---------------------|-------|
| UserDetails 조회 | 1 query/request | 0 (캐시 히트 시) | **~95% 감소** (TTL 5분 내 동일 유저) |
| 문서 목록 | 1 query/request | 0 (캐시 히트 시) | **~80% 감소** |
| 문서 상세 | 1 query/request | 0 (캐시 히트 시) | **~85% 감소** |
| 태그 조회 | 1 query/request | 0 (캐시 히트 시) | **~95% 감소** (TTL 30분) |

### 응답 시간 개선 예측

| 시나리오 | 캐시 전 | 캐시 후 (L1 Hit) | 개선율 |
|---------|--------|-----------------|-------|
| 인증된 API 요청 (UserDetails) | +5-10ms | +0.1ms | **~98% 개선** |
| 문서 목록 조회 | 10-30ms | 0.1-0.5ms | **~95% 개선** |
| 문서 상세 조회 | 5-15ms | 0.1ms | **~98% 개선** |
| 태그 목록 조회 | 3-8ms | 0.1ms | **~97% 개선** |

### 주의사항
- 캐시 히트율이 낮은 초기 단계(cold start)에서는 효과 미미
- TTL 만료 직후 thundering herd 가능 → 향후 필요 시 cache stampede 방지 로직 추가
- 문서 수정 빈도가 높아지면 invalidation 오버헤드 증가 가능
