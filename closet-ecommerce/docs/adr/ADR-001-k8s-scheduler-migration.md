# ADR-001: K8s 전환 시 @Scheduled → ShedLock + K8s CronJob 마이그레이션

**상태**: Accepted
**날짜**: 2026-04-06
**작성자**: Architect Team

---

## 컨텍스트

Closet 이커머스 플랫폼이 Docker Compose → Kubernetes로 전환 예정이다. K8s에서 각 서비스가 멀티 Pod(최소 2개)로 배포되면, Spring `@Scheduled` 어노테이션을 사용한 스케줄 작업이 **모든 Pod에서 중복 실행**되는 문제가 발생한다.

### 현재 @Scheduled 사용 현황 (5곳)

| 위치 | 스케줄 | 주기 | 특성 |
|------|--------|------|------|
| `OutboxPoller` (closet-common) | fixedDelay=5s | 5초 | **고빈도 폴링**, 멱등성 있음 (PENDING→PUBLISHED) |
| `AutoConfirmBatchJob` (closet-order) | cron 0,12시 | 12시간 | **배치 작업**, 중복 실행 시 데이터 정합성 위험 |
| `AutoConfirmScheduler` (closet-fulfillment) | cron 매일 0시 | 24시간 | **배치 작업**, 이벤트 발행 포함 |
| `TrackingPollScheduler` (closet-fulfillment) | fixedDelay=30m | 30분 | **외부 API 폴링**, 중복 시 택배사 API 과호출 |
| `PopularKeywordService.refreshSnapshot` (closet-search) | fixedRate=1h | 1시간 | **캐시 갱신**, 중복 실행 시 성능 낭비 |

### 중복 실행 시 영향도 분석

| 스케줄러 | 중복 실행 시 | 심각도 |
|----------|------------|--------|
| OutboxPoller | 동일 이벤트 2회 발행 → **Kafka 메시지 중복** | HIGH |
| AutoConfirmBatchJob | 동일 주문 2회 확정 시도 → **DB 정합성 오류** | CRITICAL |
| AutoConfirmScheduler | 동일 건 2회 구매확정 → **이중 이벤트 발행** | CRITICAL |
| TrackingPollScheduler | 택배사 API 2배 호출 → **Rate Limit 초과** | MEDIUM |
| PopularKeywordService | Redis 2회 쓰기 → **성능 낭비** (정합성 OK) | LOW |

---

## 검토한 대안

### 1. ShedLock (분산 락 기반)

```
장점: @Scheduled 그대로 유지, 코드 변경 최소, DB/Redis 락
단점: 장애 시 락 해제 문제, 스케줄 관리가 코드에 종속
적합: 고빈도 폴링 (OutboxPoller, TrackingPoll, PopularKeyword)
```

### 2. K8s CronJob

```
장점: K8s 네이티브, 단일 Pod 실행 보장, 독립 배포/롤백
단점: 코드 변경 필요 (CLI runner), 네트워크 분리 시 DB 접근 설정 추가
적합: 저빈도 배치 (AutoConfirmBatchJob, AutoConfirmScheduler)
```

### 3. Quartz Cluster Mode

```
장점: 풍부한 기능, DB 기반 클러스터링
단점: 무겁고 복잡, 테이블 11개 추가, 우리 규모에 오버엔지니어링
판정: 기각
```

### 4. 자체 Redis 분산 락

```
장점: Redisson 이미 사용 중, 추가 의존성 없음
단점: ShedLock과 본질적으로 동일하지만 직접 구현 부담
판정: ShedLock이 이미 표준화된 구현 → 바퀴 재발명 불필요
```

---

## 결정

**하이브리드 전략**: 스케줄러 특성에 따라 2가지 방식을 병행한다.

### Tier 1: ShedLock (고빈도 인프라 폴링)

**대상**: OutboxPoller, TrackingPollScheduler, PopularKeywordService

```kotlin
// Before
@Scheduled(fixedDelay = 5000)
fun poll() { ... }

// After
@Scheduled(fixedDelay = 5000)
@SchedulerLock(name = "outbox-poller", lockAtMostFor = "4s", lockAtLeastFor = "3s")
fun poll() { ... }
```

**이유**: 
- 5초~1시간 주기로 빈번하게 실행 → K8s CronJob은 최소 1분 단위라 부적합
- 코드 변경 최소화 (어노테이션 1줄 추가)
- 멱등성이 이미 보장된 작업 (OutboxPoller: PENDING 상태 체크, 인기검색어: Redis 덮어쓰기)

**락 저장소**: MySQL (`shedlock` 테이블) — 이미 각 서비스가 DB 사용 중

### Tier 2: K8s CronJob (저빈도 비즈니스 배치)

**대상**: AutoConfirmBatchJob, AutoConfirmScheduler

```yaml
# k8s/cronjobs/auto-confirm.yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: auto-confirm-scheduler
spec:
  schedule: "0 0 * * *"
  concurrencyPolicy: Forbid
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: auto-confirm
            image: closet-fulfillment:latest
            command: ["java", "-jar", "app.jar", "--spring.profiles.active=batch"]
            args: ["--job=auto-confirm"]
          restartPolicy: OnFailure
```

**이유**:
- 1일 1~2회 실행 → CronJob이 적합
- `concurrencyPolicy: Forbid`로 단일 실행 보장
- 배치 실패 시 K8s 자체 재시도 (`backoffLimit`)
- 배치 전용 Spring Profile로 스케줄러 비활성화

---

## 무중단 마이그레이션 전략

Feature Flag(`FeatureToggleService`)를 활용한 **3단계 점진적 전환**.

### Phase 1: ShedLock 도입 (Zero-downtime)

```
1. ShedLock 의존성 추가 + shedlock 테이블 마이그레이션
2. 기존 @Scheduled에 @SchedulerLock 추가 (코드 변경만, 동작 동일)
3. 배포 → 단일 인스턴스에서도 ShedLock 정상 동작 확인
4. K8s 전환 후 멀티 Pod에서 락 경쟁 → 1 Pod만 실행 확인
```

### Phase 2: 배치 Job 분리 (Feature Flag)

```
1. AutoConfirmBatchJob/AutoConfirmScheduler → CommandLineRunner 기반 Batch Job 구현
2. Feature Flag: feature.auto-confirm-scheduler-enabled=false (기존 스케줄러 OFF)
3. K8s CronJob 배포 (batch Profile)
4. 모니터링: CronJob 실행 로그 + 구매확정 건수 확인
5. 안정화 후 기존 @Scheduled 코드 제거
```

### Phase 3: @EnableScheduling 제거

```
1. 모든 @Scheduled가 ShedLock 또는 CronJob으로 전환 완료 확인
2. @EnableScheduling 제거 (또는 ShedLock 전용으로 유지)
3. 기존 Feature Flag property 정리
```

### 롤백 전략

```
Phase 1 롤백: ShedLock 어노테이션 제거 → 기존 동작 복원
Phase 2 롤백: Feature Flag ON → 기존 @Scheduled 즉시 복원, CronJob suspend
Phase 3 롤백: @EnableScheduling 재추가
```

---

## 결과

### 최종 스케줄러 배치 (K8s 전환 후)

| 스케줄러 | 방식 | 주기 | 락/보장 |
|----------|------|------|---------|
| OutboxPoller | **ShedLock** | 5s fixedDelay | MySQL 락, lockAtMost=4s |
| TrackingPollScheduler | **ShedLock** | 30m fixedDelay | MySQL 락, lockAtMost=25m |
| PopularKeywordService | **ShedLock** | 1h fixedRate | MySQL 락, lockAtMost=50m |
| AutoConfirmBatchJob | **K8s CronJob** | 0시, 12시 | concurrencyPolicy: Forbid |
| AutoConfirmScheduler | **K8s CronJob** | 매일 0시 | concurrencyPolicy: Forbid |

### 추가 인프라

```sql
-- Flyway: V__add_shedlock.sql
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL COMMENT '락 이름',
    lock_until DATETIME(6)  NOT NULL COMMENT '락 유지 시각',
    locked_at  DATETIME(6)  NOT NULL COMMENT '락 획득 시각',
    locked_by  VARCHAR(255) NOT NULL COMMENT '락 획득 인스턴스',
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ShedLock 분산 스케줄 락';
```

### 의존성

```kotlin
// build.gradle.kts (closet-common)
implementation("net.javacrumbs.shedlock:shedlock-spring:5.10.0")
implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.10.0")
```

---

## 참고

- [ShedLock GitHub](https://github.com/lukas-krecan/ShedLock)
- [K8s CronJob 문서](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/)
- 쿠팡: 수백 개 마이크로서비스에서 배치 작업은 별도 Job 서비스로 분리
