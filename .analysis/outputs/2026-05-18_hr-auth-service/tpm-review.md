# TPM 산출물 검수 — hr-platform/auth-service

**검수 대상**: `tpm-analysis.md` (17 티켓, 15 API, 11 DomainEvent)
**비교 기준**: `auth-prd-reconstructed.md`, `.claude/rules/be-code-convention.md`, employee-service main 머지본
**작성일**: 2026-05-18

---

## 판정: NEEDS_REVISION

P0 1건(Kafka publisher 토픽 라우팅 누락) + P1 3건이 구현 시작 전 보강 필요. 그 외 17 티켓 DAG 자체는 건전. P0만 분해 보강하면 PASS 전환 가능.

---

## P0 — 치명 이슈 (구현 시작 불가)

### P0-1. `DomainEventPublisher`의 토픽 라우팅이 employee 전용으로 하드코딩됨
- **근거**: `hr-platform/employee-service/src/main/kotlin/com/hrplatform/employee/infrastructure/kafka/KafkaDomainEventPublisher.kt:18` — `@Value("\${hrplatform.kafka.topics.employee:event.hr.employee.v1}")` 단일 토픽 주입.
- **영향**: AT-SVC-1/2/3에서 `DomainEventPublisher.publishAll()`을 호출하면 auth 이벤트 11종이 모두 `event.hr.employee.v1`로 발행됩니다. AT-WKR(auth가 구독)이 자기 발행 이벤트를 다시 수신하는 루프까지 발생합니다.
- **요구 조치**:
  1. AT-BS 또는 신규 티켓 `AT-EVT-PUB`에서 auth-service 전용 `AuthKafkaDomainEventPublisher`(infrastructure/kafka)를 신설하고 `aggregateType=="UserAccount"`이면 `event.hr.auth.v1`로 라우팅하도록 분리.
  2. 또는 core 모듈의 `DomainEventPublisher` 계약에 토픽 선택 책임을 명시(aggregateType→topic 매핑 빈)하여 employee/auth 양쪽이 공유 가능한 형태로 리팩토링. 단, 이는 employee 모듈 수정이 동반되므로 "변경 없음" 전제를 깸 — 영향도 명시 필수.
  3. TPM 분석의 §영향 서비스 표에 employee-service "변경 없음"을 유지하려면 **방안 1** 채택이 안전.

---

## P1 — 개선 권고 (구현 진행 가능하나 보완 필요)

### P1-1. employee.suspended의 `state.status="ON_LEAVE"` ↔ UserAccount `SUSPENDED` 매핑 누락
- **근거**: `EmployeeSuspendedEvent.kt:37` → `status="ON_LEAVE"`. PRD 재구성본에는 `EmployeeSuspended 수신 → UserAccount.status=SUSPENDED`만 명시.
- **요구 조치**: AT-WKR 작업 범위에 **이벤트 status 문자열 매핑 테이블**(`ON_LEAVE→SUSPENDED`, `RESIGNED→DEACTIVATED`, `ACTIVE→ACTIVE`)을 명시. 단순 enum.valueOf 시도하면 런타임 실패합니다.

### P1-2. Kafka Consumer 인프라(group-id·error-handler·DLQ publisher) 신설 범위 부족
- **근거**:
  - employee-service의 `KafkaConfig.kt`는 **Producer만** 정의. ConsumerFactory/ContainerFactory는 없음.
  - `application.yml:74-79` 에 consumer 기본키만 있고 `spring.kafka.consumer.group-id` 미정의 → `@KafkaListener(groupId = …)`로 직접 지정 필요.
  - AT-WKR가 처음으로 Consumer를 도입하는 모듈인데 티켓 작업 범위에 ContainerFactory(에러 핸들러·DLQ Recoverer·재시도) 명시가 빠짐.
- **요구 조치**: AT-WKR 작업 범위에 다음 항목 추가:
  - `KafkaConsumerConfig`(infrastructure/kafka) — ConcurrentKafkaListenerContainerFactory + DefaultErrorHandler + DeadLetterPublishingRecoverer
  - `application.yml`에 `hrplatform.kafka.consumer-groups.employee-sync: auth-service.employee-sync.v1` 키 추가
  - DLQ 토픽 publisher 분리(별도 Producer 또는 KafkaTemplate 재사용)

### P1-3. AuditorAware 빈이 employee 모듈에 묶여 있음 — auth-service에서 재사용 불가
- **근거**: `SpringAuditorAware.kt:13` — `@Component("auditorAware")` 클래스가 `com.hrplatform.employee.infrastructure.audit` 패키지에 위치. auth-service는 employee-service에 의존하지 않으므로 동일 빈을 재사용할 수 없음.
- **요구 조치**: 두 가지 중 하나 — TPM 결정 필요:
  - (A) auth-service 내부에 동일 stub `SpringAuditorAware` 복제 — AT-BS 작업 범위에 명시. SecurityContext 연동은 AT-SEC 완료 시점에 SecurityContextAuditorAware로 교체.
  - (B) core 모듈로 AuditorAware를 끌어올려 양쪽 공유 — employee 코드 일부 이동, 본 MVP 범위에 영향.
- **현재 분석에는 어느 쪽도 명시되지 않음** — AT-BS 또는 AT-DOM-1 작업 범위에 결정 필요.

---

## P2 — 정합성 검증 결과

### 보강 5개 API 포함 여부 — PASS
| API | TPM 표 포함 | 티켓 매핑 |
|---|:-:|---|
| POST /auth/password/change | ● | AT-UC-2 |
| POST /auth/users/{id}/unlock | ● | AT-UC-4 |
| POST /auth/users/{id}/sessions/logout-all | ● | AT-UC-4 |
| POST /auth/api-tokens | ● | AT-UC-3 |
| DELETE /auth/api-tokens/{id} | ● | AT-UC-3 |

### 11종 DomainEvent action+state 규약 — PASS (단, Q1 잔존)
- core의 `DomainEvent`/`DomainEventAction`/`DomainEventState` 계약과 11종 모두 정합.
- AT-EVT 완료 기준에 "envelope→JSON Schema 11종 통과" 검증 포함됨 — 적절.
- 단, `UserUnlocked.action=UNLOCK`이 자동(15분 백오프) vs 수동(관리자) 두 트리거를 가지므로 `action.details.trigger ∈ {AUTO, MANUAL}` 필드 필수 — AT-EVT 작업 범위에 명시 권장.

### UserAccount 4상태 머신 + 전이 — PASS
- ACTIVE/LOCKED/SUSPENDED/DEACTIVATED 4상태 명시. DEACTIVATED 종착 정상.
- AT-DOM-1의 `UserAccountStatus.canTransitTo` enum 캡슐화 정상.
- AC2(15분 자동 해제)에 대한 트리거: 스케줄러 vs 다음 로그인 시점 lazy 체크 — TPM 분석 미언급. **권고**: lazy 체크(`AuthDomainService.authenticate`가 `lockedUntil < now`이면 자동 unlock + UNLOCK(AUTO) 이벤트 발행)가 인프라 비용 적음.

### employee 이벤트 4종 구독 명시 — PASS (매핑 보강 P1-1 필요)
- AT-WKR 작업 범위에 Hired/Resigned/Suspended/Resumed 4종 명시.
- eventType 기반 라우팅 명시 — 정상.
- 미언급: **Idempotency 키 구현체**(Redis SETNX 또는 별도 테이블) — "또는"으로 OR로 두지 말고 단일 선택 권장.

### 17 티켓 DAG 정합성 + Single Writer per File — PASS (1건 주의)
- DAG 위상정렬 7 wave, ready 셋 분포 정상 (W1=2, W4=3, W5=5).
- 후행 카운트 ≥ 3 티켓(AT-BS, AT-EVT, AT-SVC-1) 모두 정당한 공통 산출물 — 분해 불필요.
- **주의**: W4(SVC-1/2/3)와 W5(UC-1~4, WKR) 사이 — AT-SVC-1과 AT-WKR가 모두 `application/sync/UserAccountSyncUseCase.kt`(또는 동급 파일)를 손댈 위험. TPM은 "UserAccountSyncUseCase는 AT-WKR가 application layer에 신설"로 명시했으나, 만약 AT-SVC-1의 `authenticate` 결과를 sync에서 호출하면 W5 내 단일 파일 충돌 가능. **요구 조치**: AT-WKR 작업 범위에 "UseCase 신설 위치와 파일명" 명시.

### be-code-convention 위반 가능성 — 잠재 위험 1건
- **Port 인터페이스**: 분석 본문에 `Port`/`Adapter` 단어 없음 — OK.
- **@Query**: 분석 본문에 `@Query` 단어 없음, CustomRepositoryImpl(QueryDSL) 명시됨 — OK.
- **LocalDateTime**: 분석 본문에 모두 ZonedDateTime/없음. PRD 재구성본도 `lockedUntil(ZonedDateTime)` 명시 — OK.
- **shadow Entity**: PRD 재구성본 `be-code-convention 적용` 섹션에 "7개 모두 @Entity 직접 부착, shadow 금지" 명시 — OK.
- **잠재 위험**: AT-SVC-1이 `RedisJtiBlacklist`를 DomainService 안에서 직접 호출하는 것으로 보이는데, Redis는 infrastructure 어댑터입니다. domain layer는 `JtiBlacklist` interface만 정의하고 Redis 구현체는 infrastructure로 분리해야 컨벤션의 "Domain은 어느 것도 import하지 않는다" 원칙을 지킬 수 있습니다. → AT-SVC-1 작업 범위에 **`JtiBlacklist interface(domain) + RedisJtiBlacklist(infrastructure)`** 분리 명시 필요.

### JPA Auditing AuditorAware 빈 — P1-3에서 다룸

---

## 확인된 누락 티켓

| 제목 | 레포 | 이유 |
|---|---|---|
| (선택) AT-EVT-PUB — Auth 전용 KafkaDomainEventPublisher 분리 | hr-platform | P0-1 해결. AT-EVT에 흡수 가능하나 토픽 라우팅 책임은 인프라성이므로 분리가 안전. |
| (선택) AT-CONS-CFG — Kafka Consumer 인프라(ContainerFactory + ErrorHandler + DLQ Recoverer + AuthDeadLetterPublisher) | hr-platform | P1-2 해결. AT-WKR에 흡수해도 무방하지만 인프라 코드 비중 크면 분리. AT-WKR 선행으로 배치 시 W5 fan-out 일부 손상 — AT-BS 또는 AT-KF 직후 wave에 단독 배치 권장. |

> 두 티켓 모두 "흡수 가능"이므로 강제 신설은 아닙니다. 다만 작업 범위 명시는 필수입니다.

---

## 승인 조건

다음 4개 항목이 TPM 분석 v2에 반영되면 PASS:

1. **P0-1 반영**: AT-BS 또는 신규 AT-EVT-PUB에 auth 전용 publisher 명시. 라우팅 키(aggregateType 또는 명시 토픽 파라미터) 결정.
2. **P1-1 반영**: AT-WKR 작업 범위에 employee 이벤트 `state.status` → UserAccount `status` 변환 테이블 명시.
3. **P1-2 반영**: AT-WKR 또는 신규 AT-CONS-CFG에 ConsumerFactory + DefaultErrorHandler + DLQ Recoverer + consumer group-id 키(`auth-service.employee-sync.v1`) 명시.
4. **P1-3 반영**: AT-BS 작업 범위에 auth-service 내부 `SpringAuditorAware` 복제(또는 core 이동) 결정 명시.

추가 권장(소요 작은 보강, PASS 후 적용 가능):
- AT-DOM-1: `UserAccountStatus.canTransitTo` 표 명시(전이 매트릭스).
- AT-EVT: `UserUnlocked.action.details.trigger ∈ {AUTO, MANUAL}` 필드 명시.
- AT-SVC-1: `JtiBlacklist` interface vs RedisJtiBlacklist 구현 분리 명시 + 15분 자동 해제 lazy 체크 위치 결정.
- AT-WKR: Idempotency 구현체 단일 선택(Redis SETNX 권장 — 기존 인프라 재사용).

---

## P3 — 관찰 사항 (미결 사항 Q1~Q6 검토)

| ID | TPM 권장 결정 | 검수 코멘트 |
|---|---|---|
| Q1 | Flyway 글로벌 시드(companyId=NULL) | 검수 OK. roles 테이블 companyId NOT NULL이면 스키마 변경 필요 — AT-DB의 roles 컬럼 정의 재확인. |
| Q2 | /auth/api-tokens HR_MANAGER+ 전용 | 검수 OK. AT-CTL의 `MyAuthController` 항목에서 api-tokens 빠지고 AdminAuthController로 이동 확정 필요. |
| Q3 | NotificationGateway interface + LogStub | 검수 OK. AC2의 "알림 메일 발송" 검증은 LogStub로 만족 — E2E에서 로그 캡처 검증. |
| Q4 | Redis 프로파일 분리 | 검수 OK. 본 분석 범위 외 정당. |
| Q5 | `auth-service.employee-sync.v1` 그룹 ID | 검수 OK. P1-2 조치 시 함께 반영. |
| Q6 | SSO Phase 1.5 — MVP 제외 | 검수 OK. PRD 재구성본과 정합. |

---

## P4 — 산출물 메타

- 검수 시간: 약 30분
- 검토 파일: tpm-analysis.md(393라인) + auth-prd-reconstructed.md(162라인) + employee-service 기존 코드 12개 파일 + core 4개 파일 + infrastructure/kafka 설정
- be-code-convention.md 준수 항목 16개 중 위반 0건, 잠재 위험 1건(JtiBlacklist 분리).
- TPM이 진단한 후행 카운트 ≥ 3 티켓 3건 모두 정당.
