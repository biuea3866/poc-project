# 캐시 전략 및 문제 해결 가이드

이 프로젝트는 다양한 캐시 전략과 운영 문제 해결 방법을 실제 코드로 구현한 예제입니다.

## 구현된 캐시 전략

### 1. Look Aside + Write Around

**읽기 전략 - Look Aside**:
- 애플리케이션이 캐시에서 먼저 조회
- 캐시 미스 시 DB에서 조회 후 캐시 업데이트
- 애플리케이션이 모든 접근을 담당

**쓰기 전략 - Write Around**:
- DB에만 저장, 캐시는 무효화
- 다음 읽기 시 캐시 미스 발생하여 새 데이터 로드

**사용 예시**:
```bash
# 제품 조회
curl http://localhost:8080/api/strategy/look-aside-write-around/1

# 제품 생성/수정
curl -X POST http://localhost:8080/api/strategy/look-aside-write-around \
  -H "Content-Type: application/json" \
  -d '{"id": 10, "name": "New Product", "price": 99.99}'

# 제품 삭제
curl -X DELETE http://localhost:8080/api/strategy/look-aside-write-around/1

# 캐시 초기화
curl -X DELETE http://localhost:8080/api/strategy/look-aside-write-around/cache
```

**장점**:
- 쓰기 비용 낮음
- 읽기 중심 워크로드에 적합

**단점**:
- 쓰기 후 즉시 읽을 때 캐시 미스
- 캐시 스템피드 가능성

---

### 2. Read Through + Write Around

**읽기 전략 - Read Through**:
- 애플리케이션은 캐시만 접근
- 캐시가 DB 접근을 담당 (CacheLoader)
- 캐시가 단일 진입점

**쓰기 전략 - Write Around**:
- DB에만 저장, 캐시 무효화
- 다음 읽기 시 캐시가 자동으로 DB에서 로드

**사용 예시**:
```bash
# 제품 조회
curl http://localhost:8080/api/strategy/read-through-write-around/1

# 제품 생성/수정
curl -X POST http://localhost:8080/api/strategy/read-through-write-around \
  -H "Content-Type: application/json" \
  -d '{"id": 11, "name": "Product", "price": 199.99}'

# 통계 조회
curl http://localhost:8080/api/strategy/read-through-write-around/stats
```

**장점**:
- DB 접근 로직이 캐시에 캡슐화
- 애플리케이션 로직 단순화

**단점**:
- 캐시가 SPOF (Single Point of Failure)

---

### 3. Read Through + Write Through

**읽기 전략 - Read Through**:
- 캐시가 DB 접근 담당

**쓰기 전략 - Write Through**:
- 캐시와 DB에 동시 저장
- 실시간 일관성 유지

**사용 예시**:
```bash
# 제품 조회
curl http://localhost:8080/api/strategy/read-through-write-through/1

# 제품 생성/수정 (캐시와 DB 동시 저장)
curl -X POST http://localhost:8080/api/strategy/read-through-write-through \
  -H "Content-Type: application/json" \
  -d '{"id": 12, "name": "Instant Update", "price": 299.99}'

# 통계 조회
curl http://localhost:8080/api/strategy/read-through-write-through/stats
```

**장점**:
- 최고의 실시간성
- 강력한 데이터 일관성

**단점**:
- 쓰기 성능 저하 (2곳에 저장)
- 쓰기 호출량이 많으면 부하 증가

---

## 캐시 운영 문제 해결

### 1. 캐시 스템피드 해결 - 조기 갱신 (Early Refresh)

**문제**:
- TTL 만료 시점에 대량의 요청이 DB로 몰림
- 특히 정각마다 만료되는 경우 심각

**해결 방법**:
- TTL 만료 전에 스케줄러로 미리 갱신
- 핫키만 선택적으로 갱신

**사용 예시**:
```bash
# 제품 조회
curl http://localhost:8080/api/problem/early-refresh/1

# 핫키 추가
curl -X POST http://localhost:8080/api/problem/early-refresh/hotkey/1

# 핫키 제거
curl -X DELETE http://localhost:8080/api/problem/early-refresh/hotkey/1

# 전체 캐시 강제 갱신
curl -X POST http://localhost:8080/api/problem/early-refresh/refresh

# 통계 조회
curl http://localhost:8080/api/problem/early-refresh/stats
```

**특징**:
- TTL: 30분
- 갱신 주기: 29분마다 자동 실행
- 핫키만 선택적으로 갱신하여 효율성 향상

**장점**:
- 안정적인 캐시 적중률 유지
- 스템피드 완전 방지

**단점**:
- 주기적인 갱신 비용
- 사용하지 않는 데이터도 갱신

---

### 2. 캐시 스템피드 해결 - PER (Probabilistic Early Recomputation)

**문제**:
- 조기 갱신은 모든 캐시를 갱신하여 비용이 큼

**해결 방법**:
- 확률적으로 만료 전에 갱신
- 공식: `(currentTime() - delta * beta * log(rand(0,1))) >= expiry`
- 만료 시점에 가까울수록, 재계산 시간이 길수록 갱신 확률 증가

**사용 예시**:
```bash
# 제품 조회 (beta 기본값: 1.0)
curl http://localhost:8080/api/problem/per/1

# Beta 값 지정 (높을수록 조기 갱신 빈도 증가)
curl http://localhost:8080/api/problem/per/1?beta=2.0

# Beta 테스트 (0.5, 1.0, 2.0, 5.0)
curl -X POST http://localhost:8080/api/problem/per/test-beta/1

# 통계 조회
curl http://localhost:8080/api/problem/per/stats
```

**Beta 파라미터**:
- `beta = 0.5`: 낮은 갱신 빈도
- `beta = 1.0`: 기본 (권장)
- `beta = 2.0`: 높은 갱신 빈도
- `beta = 5.0`: 매우 높은 갱신 빈도

**장점**:
- 조기 갱신보다 효율적
- Lock 방식보다 대기 시간 없음

**단점**:
- 구현 복잡
- Beta 파라미터 튜닝 필요

---

### 3. 캐시 관통 해결 - 블룸 필터 (Bloom Filter)

**문제**:
- 존재하지 않는 데이터 반복 조회 시 매번 DB 접근
- 빈 값 캐싱도 메모리 사용

**해결 방법**:
- 블룸 필터로 존재 여부를 먼저 체크
- 존재하지 않으면 DB 접근 없이 즉시 반환

**사용 예시**:
```bash
# 제품 조회
curl http://localhost:8080/api/problem/bloom-filter/1

# 존재하지 않는 제품 조회 (DB 접근 차단)
curl http://localhost:8080/api/problem/bloom-filter/9999

# 제품 생성 (블룸 필터에 자동 추가)
curl -X POST http://localhost:8080/api/problem/bloom-filter \
  -H "Content-Type: application/json" \
  -d '{"id": 100, "name": "Bloom Product", "price": 99.99}'

# 통계 조회 (차단률 확인)
curl http://localhost:8080/api/problem/bloom-filter/stats

# 캐시 및 블룸 필터 초기화
curl -X DELETE http://localhost:8080/api/problem/bloom-filter/cache
```

**블룸 필터 동작**:
1. **삽입**: 여러 해시 함수로 비트 배열 설정
2. **조회**: 모든 해시 위치가 true면 "존재 가능"
3. **결과**:
   - 모두 true → 존재할 수도 있음 (False Positive 가능)
   - 하나라도 false → 확실히 존재하지 않음

**특징**:
- False Positive 가능 (없는데 있다고 판단)
- False Negative 불가능 (있는데 없다고 판단 X)
- 메모리 효율적

**장점**:
- 존재하지 않는 데이터의 DB 접근 완전 차단
- 빈 값 캐싱 대비 메모리 효율적

**단점**:
- False Positive로 인한 불필요한 조회 가능
- 삭제 연산 미지원

---

## 전략 비교표

| 전략 | 읽기 지연 | 쓰기 지연 | 일관성 | 복잡도 | 사용 사례 |
|------|----------|----------|--------|--------|----------|
| Look Aside + Write Around | 낮음 | 매우 낮음 | 보통 | 낮음 | 읽기 중심 |
| Read Through + Write Around | 낮음 | 낮음 | 보통 | 보통 | 캐시 중심 설계 |
| Read Through + Write Through | 낮음 | 높음 | 높음 | 높음 | 강한 일관성 필요 |

## 문제 해결 방법 비교표

| 방법 | 해결 문제 | 리소스 비용 | 복잡도 | 효율성 |
|------|----------|-------------|--------|--------|
| Lock (기존) | 스템피드 | 낮음 | 낮음 | 보통 |
| 조기 갱신 | 스템피드 | 높음 | 낮음 | 높음 (안정적) |
| PER | 스템피드 | 낮음 | 높음 | 높음 (효율적) |
| 빈 값 캐싱 (기존) | 관통 | 보통 | 낮음 | 보통 |
| 블룸 필터 | 관통 | 낮음 | 보통 | 높음 (메모리 효율) |

## 실행 방법

```bash
# 애플리케이션 실행
./gradlew bootRun

# 특정 전략 테스트
# 1. Look Aside + Write Around
curl http://localhost:8080/api/strategy/look-aside-write-around/1

# 2. Read Through + Write Around
curl http://localhost:8080/api/strategy/read-through-write-around/1

# 3. Read Through + Write Through
curl http://localhost:8080/api/strategy/read-through-write-through/1

# 4. Early Refresh
curl http://localhost:8080/api/problem/early-refresh/1

# 5. PER
curl http://localhost:8080/api/problem/per/1?beta=1.0

# 6. Bloom Filter
curl http://localhost:8080/api/problem/bloom-filter/1
```

## 로그 확인

각 전략과 문제 해결 방법은 상세한 로깅을 제공합니다:

```bash
# 애플리케이션 실행 후 로그 확인
tail -f logs/spring.log

# 주요 로그 패턴
[Look Aside] ...
[Write Around] ...
[Read Through] ...
[Write Through] ...
[Early Refresh] ...
[PER] ...
[Bloom Filter] ...
```

## 참고 자료

- [spring_local_cache.md](spring_local_cache.md) - 캐싱 기본 개념
- [spring_local_cache_combination.md](spring_local_cache_combination.md) - 캐시 전략 조합
- [CACHE_GUIDE.md](CACHE_GUIDE.md) - 기본 캐시 사용 가이드
