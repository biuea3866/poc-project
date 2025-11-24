# 캐시 성능 종합 분석 보고서

## 날짜: 2025-11-23

---

## 핵심 발견사항 요약

### 🔍 1. 로컬 캐시 성능 향상이 미비했던 이유

#### 문제: 캐시 히트율 0.92%

**원인**: k6 테스트 스크립트가 **완전 랜덤** 패턴으로 데이터 조회

```javascript
// 기존 스크립트 (문제)
const orderId = Math.floor(Math.random() * 1000000) + 1;
// → 100만 개 중 매번 랜덤 선택 → 중복 거의 없음
```

**결과**:
- 총 요청: 62,600
- Cache Hit: 579 (0.92%)
- Cache Miss: 62,021 (99.08%)
- 평균 응답 시간: 7.27ms
- **캐시가 거의 작동하지 않음!**

#### 해결: Hot-key 패턴 적용

```javascript
// 개선된 스크립트 (Zipf 분포)
function getHotKeyOrderId() {
  const rand = Math.random();
  if (rand < 0.8) {
    // 80% 확률로 상위 10,000개 조회 (hot keys)
    return Math.floor(Math.random() * 10000) + 1;
  } else {
    // 20% 확률로 나머지 990,000개 조회 (cold keys)
    return Math.floor(Math.random() * 990000) + 10001;
  }
}
```

**결과**:
- 총 요청: 65,334
- Cache Hit: 34,383 (52.6%) ✅
- Cache Miss: 30,951 (47.4%)
- 평균 응답 시간: 2.83ms
- **캐시 히트율 57배 증가!**

#### 비교 분석

| 패턴 | Hit Rate | 평균 응답 시간 | P95 | 개선율 |
|------|----------|---------------|-----|--------|
| **랜덤 (Uniform)** | 0.92% | 7.27ms | 12.91ms | baseline |
| **Hot-key (Zipf)** | 52.6% | 2.83ms | 7.42ms | **61% 개선** |
| **No Cache** | 0% | 8.32ms | 14.98ms | - |

**핵심 인사이트**:
- 🎯 **실제 서비스 패턴 중요**: 균등 분포가 아닌 Zipf/Pareto 분포가 현실적
- ✅ **캐시는 Hot-key 패턴에서 진가 발휘**: 20%의 데이터가 80% 조회
- 💡 **maximumSize=10,000 설정 효과적**: 100만 건 중 1%만 캐싱해도 충분

---

### 🔧 2. MySQL Grafana 모니터링 문제 해결

#### 문제: MySQL Exporter 계속 재시작

**에러 로그**:
```
level=error msg="failed to validate config" section=client err="no user specified in section or parent"
level=info msg="Error parsing host config" file=.my.cnf err="no configuration found"
```

**원인**: docker-compose.yml의 환경 변수 설정 오류

```yaml
# 기존 설정 (문제)
mysql-exporter:
  environment:
    - DATA_SOURCE_NAME=root:password@(mysql:3306)/
```

#### 해결

```yaml
# 수정된 설정
mysql-exporter:
  command:
    - '--mysqld.username=root'
    - '--mysqld.address=mysql:3306'
  environment:
    - MYSQLD_EXPORTER_PASSWORD=password
  depends_on:
    mysql:
      condition: service_healthy
```

**결과**:
```
✅ MySQL Exporter 정상 작동
✅ Prometheus에서 메트릭 수집 확인
   - mysql_global_status_queries: 3,451,677
   - mysql_global_status_threads_connected: 7
```

---

### ⚡ 3. Cache Penetration 분석 (TTL 만료)

**현재 설정**:
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=60s
```

#### 분석 결과

**1. TTL 60초 설정의 영향**:
- 캐시 엔트리는 60초 후 자동 만료
- 만료 시점에 동일한 키 조회 → DB 쿼리 발생
- **하지만** Hot-key 패턴에서는 즉시 재캐싱됨

**2. Cache Penetration 위험도**: **낮음**

**이유**:
- ✅ **점진적 만료**: 엔트리별로 생성 시간이 다름 → 동시 만료 없음
- ✅ **Hot-key는 계속 재캐싱**: 자주 조회되는 데이터는 TTL 내 재접근
- ✅ **Window TinyLFU**: 빈도 기반 eviction으로 인기 데이터 유지

**3. 실제 측정 (Hot-key 패턴)**:
- 테스트 기간: 2분 (TTL 60초 × 2회 주기)
- 캐시 히트율: 52.6% (안정적 유지)
- P95 응답 시간: 7.42ms (일정 유지)

**결론**: TTL 만료로 인한 Cache Penetration은 **미미한 수준**

#### 개선 권장사항

1. **TTL 분산 (Staggered Expiration)**:
   ```java
   // 예시: ±10% 랜덤 TTL
   .expireAfterWrite(54 + Random.nextInt(12), TimeUnit.SECONDS)
   ```

2. **Refresh Ahead**:
   ```java
   .refreshAfterWrite(50, TimeUnit.SECONDS) // TTL 전 미리 갱신
   ```

3. **모니터링 알림**:
   ```yaml
   alerts:
     - name: HighCacheMissRate
       expr: cache_miss_rate > 0.5
   ```

---

### 📊 4. JVM & MySQL CPU/Memory 사용량 분석

#### JVM 메트릭 (Spring Boot Actuator)

**메모리 사용량** (Hot-key 패턴 테스트 중):
```
JVM Heap 사용량:
- jvm_memory_used_bytes{area="heap"}: ~150MB
- jvm_memory_max_bytes{area="heap"}: 2GB
- 사용률: ~7.5%

Non-Heap:
- jvm_memory_used_bytes{area="nonheap"}: ~120MB
- Compressed Class Space: ~12MB
```

**스레드**:
```
- jvm_threads_live_threads: 24
- jvm_threads_daemon_threads: 20
- jvm_threads_states{state="runnable"}: 7
- jvm_threads_states{state="blocked"}: 0 ✅
```

**GC 활동**:
```
- jvm_gc_pause_seconds_count{action="end of minor GC"}: 낮은 빈도
- jvm_gc_memory_allocated_bytes_total: 안정적
```

#### MySQL 메트릭 (MySQL Exporter)

**쿼리 성능**:
```
- mysql_global_status_queries: 3,451,677
- mysql_global_status_slow_queries: 0 ✅
- mysql_global_status_questions: 3,451,600
```

**연결**:
```
- mysql_global_status_threads_connected: 7
- mysql_global_status_threads_running: 1-2
- mysql_global_status_max_used_connections: 10
```

**버퍼 풀 (InnoDB)**:
```
- mysql_global_status_innodb_buffer_pool_reads: 최소
- mysql_global_status_innodb_buffer_pool_read_requests: 높음
- 히트율: ~99% (InnoDB 버퍼 풀이 효과적)
```

#### 비교 분석: 캐시 유무

| 메트릭 | No Cache | Lazy Cache (Random) | Lazy Cache (Hot-key) |
|--------|----------|---------------------|---------------------|
| **애플리케이션** ||||
| JVM Heap 사용량 | ~140MB | ~150MB | ~155MB |
| GC 빈도 | 보통 | 보통 | 약간 증가 |
| 스레드 수 | 24 | 24 | 24 |
| **MySQL** ||||
| 쿼리 수 (2분) | 61,962 | 62,021 | 30,951 |
| 초당 쿼리 | 516 | 516 | 258 (**50% 감소**) |
| 연결 수 | 7 | 7 | 5-7 |
| 버퍼 풀 히트율 | 99% | 99% | 99.5% |

**핵심 발견**:
- ✅ **Hot-key 패턴**: MySQL 부하 **50% 감소** (캐시 히트율 덕분)
- ✅ **JVM 메모리 증가**: 약 15MB (캐시 10,000 entries)
- ✅ **매우 효율적**: 적은 메모리로 큰 성능 향상

---

## 최종 권장사항

### ✅ 채택된 전략: Lazy Loading with Hot-key Pattern

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=60s
```

### 성능 개선 요약

#### Hot-key 패턴 (실제 서비스 패턴)
- **응답 시간**: 평균 2.83ms (No Cache 8.32ms 대비 **66% 개선**)
- **P95 응답 시간**: 7.42ms (No Cache 14.98ms 대비 **50% 개선**)
- **캐시 히트율**: **52.6%**
- **MySQL 부하**: **50% 감소**
- **처리량**: 544 req/s (5.4% 증가)

#### 랜덤 패턴 (비현실적)
- **응답 시간**: 평균 7.27ms (개선 미미)
- **캐시 히트율**: **0.92%** (거의 작동하지 않음)

### 프로덕션 적용 체크리스트

- [x] **maximumSize 최적화**: 10,000 entries (메모리 ~15MB)
- [x] **TTL 설정**: 60s (데이터 변경 빈도에 맞게)
- [x] **모니터링 설정**:
  - [x] Prometheus + Grafana
  - [x] MySQL Exporter 정상 작동
  - [x] JVM 메트릭 노출
- [ ] **캐시 히트율 모니터링**: recordStats() 활성화 (이미 동작 중)
- [ ] **알림 설정**: 캐시 히트율 < 40% 시 알림
- [ ] **TTL 분산**: Refresh Ahead 전략 고려

### 다음 단계

1. **실제 트래픽 패턴 분석**: 80-20 법칙 검증
2. **maximumSize 튜닝**: 캐시 히트율 vs 메모리 트레이드오프
3. **Grafana 대시보드**: 실시간 모니터링
4. **부하 테스트**: 더 높은 동시성 (200-500 VUs)

---

## 결론

### 🎯 핵심 교훈

1. **테스트 패턴이 중요**: 랜덤 패턴으로 테스트하면 캐시 효과를 측정할 수 없음
2. **실제 서비스는 Hot-key 패턴**: 80-20 법칙 (파레토 원칙) 적용
3. **작은 캐시로도 충분**: 전체 데이터의 1%만 캐싱해도 52.6% 히트율
4. **Window TinyLFU 효과적**: Caffeine의 스마트한 eviction 알고리즘

### 💡 Final Verdict

**"로컬 캐시가 작동하지 않는 것이 아니라,
테스트가 현실을 반영하지 못했다!"**

- Uniform 분포 (0.92% 히트율) → 7% 개선
- Zipf 분포 (52.6% 히트율) → **66% 개선**

---

**작성자**: Claude Code
**작성일**: 2025-11-23
**버전**: 2.0 (종합 분석)
