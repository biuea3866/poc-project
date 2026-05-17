# HR-M1 employee-service 티켓 분해

**일자**: 2026-05-16
**관련 문서**: [TDD-001](../tdd/TDD-001-employee-service.md) · [ADR-001](../adr/ADR-001-hexagonal-multimodule.md) · [ADR-002](../adr/ADR-002-employee-ssot-model.md)
**TPM 분석**: `.analysis/outputs/2026-05-16_hr-employee-service/tpm-analysis.md`
**TPM 검수**: `.analysis/outputs/2026-05-16_hr-employee-service/tpm-review.md` (PASS_WITH_NOTES)

## 분해 원칙 적용

본 분해는 [`.claude/rules/ticket-guide.md`](../../../.claude/rules/ticket-guide.md)의 다음 원칙을 따랐습니다:

- 1명 / 1일 / 1PR 단위, AC와 파일 목록은 작성하지 않음
- 후행 의존 카운트 ≥ 3 티켓은 분해 재검토 → 병목 4종(BS-01 · DB-01 · KF-01 · CM-01)만 의도된 병목으로 유지
- TPM 검수 권고 반영: L 사이즈 3건(BE-02 / BE-08 / BE-11) 분할 → 총 21개 티켓 (PR #117 p2 보강에서 BE-08d/BE-08e 추가)
- Single Writer per File — 같은 wave 안 동일 파일 수정 금지
- 평균 wave 너비 2.625 · 4인 팀 가동률 평균 약 66% — wave 6 피크 7(175%)로 보장. 70% 목표 미달은 본 도메인이 SSOT라 통합 게이트(BE-12)·공통 Controller(BE-11a) 단독 wave가 본질적 병목

## 티켓 사이즈 정의

- **S**: 약 200줄 (구현 코드 기준, 테스트 제외 — test 카테고리 티켓은 테스트 코드 줄수 기준으로 별도 측정)
- **M**: 약 400줄
- **L**: 약 800줄 — 본 분해에서 L 티켓은 0개 (모두 S/M으로 분할 완료)

---

## 티켓 목록

### BS-01 — 인프라 부트스트랩 (Gradle 멀티모듈 골격)

- 카테고리: infra · 사이즈: M · 담당: BE
- 작업 내용 (설계 의도):
  - 루트 `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` 작성
  - Kotlin 2.0 · JDK 21 · Spring Boot 3.4 · Spring Cloud 2024.x 버전 카탈로그 등록
  - 공통 plugin 블록(detekt · ktlint · kover · jacoco 통합)
  - `employee-service` 모듈을 Spring Boot Application으로 부트 가능한 상태로 만들기 (기본 `@SpringBootApplication` 1개)
  - `application.yml` 기본 골격 (profile 분리: local/dev/staging/prod)
- 선행: 없음 (Wave 1 단독 병목)
- 후행 의존: 모든 후속 티켓
- 테스트 케이스: `./gradlew :employee-service:bootRun` 으로 빈 Application이 8080 포트에서 기동된다

---

### CM-01 — 공통 모듈 골격 (core + common-kafka)

- 카테고리: common · 사이즈: M · 담당: BE
- 작업 내용:
  - `core` 모듈: `BaseEntity`(id · createdAt · updatedAt) · `DomainEvent` 인터페이스 · `AggregateRoot`(domainEvents 적재·pull) · 공통 예외 5종(BusinessException · NotFoundException · UnauthorizedException · ForbiddenException · ConflictException)
  - `common-kafka` 모듈: `DomainEventPublisher` interface (domain layer 정의) · `BaseEventWorker` (도메인별 EventWorker 상속용)
  - `core` 모듈에 ZonedDateTime UTC 유틸 · ISO 3166/4217 코드 검증 유틸
- 선행: BS-01
- 후행 의존: BE-01~12, BE-07
- 테스트 케이스: `AggregateRoot` 단위 테스트로 domainEvents 적재·pull 멱등성을 검증한다

---

### DB-01 — Flyway employee 스키마 마이그레이션 (V1)

- 카테고리: db · 사이즈: M · 담당: DBA + BE 협업
- 작업 내용:
  - `V1__create_person.sql` · `V2__create_employment.sql` · `V3__create_department.sql` · `V4__create_employment_history.sql`
  - 컬럼 길이 · NOT NULL · COMMENT · 인덱스(`employment.employee_number` 유니크, `department.path` LIKE 가속 prefix index, `employment.department_id` · `employment.manager_employment_id` 보조 인덱스) 명시
  - PII 컬럼(`person.personal_email` · `person.phone_number`) AES-256-GCM 컬럼 단위 암호화 컬럼 정의 (PRD §10.2)
  - FK 미사용 (PRD §10.4 서비스 경계)
  - TINYINT(1) for boolean
- 선행: BS-01
- 후행 의존: BE-01~04, BE-12
- 테스트 케이스: Testcontainers MySQL 8.0에서 V1~V4를 순차 적용 후 4개 테이블·인덱스·COMMENT가 정확히 생성된다

---

### KF-01 — Kafka 토픽 Terraform (event.hr.employee)

- 카테고리: kafka · 사이즈: S · 담당: Platform
- 작업 내용:
  - Terraform 모듈: 토픽 `event.hr.employee` (파티션 12 · 보존 7일 · cleanup.policy=delete)
  - 9종 이벤트 페이로드 JSON Schema 정의 (`employee.hired` · `employee.resigned` · `employee.suspended` · `employee.resumed` · `employee.promoted` · `employee.transferred` · `employee.salary_changed` · `department.changed` · `department.head_changed`)
  - 컨슈머 그룹 명세는 후속 도메인(auth/attendance/leave/...) 티켓에서 추가
- 선행: BS-01
- 후행 의존: BE-07, BE-12
- 테스트 케이스: `kafka-topics --list` 출력에 `event.hr.employee` 가 1건 보이고 파티션 수 12를 만족한다

---

### BE-01 — Person Entity + Repository

- 카테고리: domain · 사이즈: S · 담당: BE
- 작업 내용:
  - `domain/person/Person.kt` — Rich Domain Model. 메서드: `validateNotMinor()`, `updateContact()`, `updateEmergencyContacts()`
  - `domain/person/PersonRepository.kt` interface (도메인 layer 정의)
  - `infrastructure/person/PersonJpaRepository.kt` + `PersonRepositoryImpl.kt` (QueryDSL)
  - `@Type(JsonStringType::class)` 으로 emergency_contacts JSON 컬럼 처리
- 선행: DB-01, CM-01
- 후행 의존: BE-05
- 테스트 케이스: Person 생성 시 만 18세 미만이면 `MinorPersonNotAllowedException` 발생한다

---

### BE-02a — Employment Entity + 상태 머신 (도메인 메서드)

- 카테고리: domain · 사이즈: M · 담당: BE
- 작업 내용:
  - `domain/employment/Employment.kt` — 7종 비즈니스 메서드(`activate`/`suspend`/`resume`/`resign`/`resignDuringLeave`/`transferTo`/`promote`/`changeCompensation`)
  - `domain/employment/EmploymentStatus.kt` enum + `canTransitTo()` 캡슐화
  - `domain/employment/EmploymentType.kt` enum
  - `domain/employment/event/*.kt` — DomainEvent 9종 (CM-01의 DomainEvent 상속)
  - `pullDomainEvents()` / `addDomainEvent()` 패턴 (AggregateRoot 활용)
- 선행: CM-01 (DB-01 없어도 컴파일 가능, 영속화는 BE-02b)
- 후행 의존: BE-02b, BE-05
- 테스트 케이스: ACTIVE Employment에 suspend(reason, until) 호출 시 status=ON_LEAVE, EmployeeSuspendedEvent 1건이 적재된다

---

### BE-02b — Employment Repository + QueryDSL

- 카테고리: infrastructure · 사이즈: S · 담당: BE
- 작업 내용:
  - `domain/employment/EmploymentRepository.kt` interface
  - `infrastructure/employment/EmploymentJpaRepository.kt`
  - `infrastructure/employment/EmploymentRepositoryImpl.kt` (QueryDSL) — 권한 자동 범위 필터링용 쿼리(`findByDepartmentTreePath`, `findManagedBy`) 포함
- 선행: BE-02a, DB-01
- 후행 의존: BE-05
- 테스트 케이스: 부서 path `/1/12/%` 하위 모든 직원이 N+1 없이 단일 쿼리로 조회된다

---

### BE-03 — Department Entity + Repository (path 트리)

- 카테고리: domain · 사이즈: M · 담당: BE
- 작업 내용:
  - `domain/department/Department.kt` — Rich Domain Model. 메서드: `moveTo(newParent)`, `assignHead(employmentId)`, `removeHead()`, `validateActive(date)`
  - Materialized Path 알고리즘 (자식 path 재계산은 DomainService가 트랜잭션 안에서 처리)
  - `domain/department/DepartmentRepository.kt` interface + 구현 (QueryDSL `path LIKE 'prefix%'`)
  - `domain/department/event/DepartmentChangedEvent.kt`, `DepartmentHeadChangedEvent.kt`
- 선행: DB-01, CM-01
- 후행 의존: BE-06
- 테스트 케이스: Department.moveTo(newParent) 호출 시 자기 path 변경되고 자식 path가 일괄 갱신된다

---

### BE-04 — EmploymentHistory Entity + Repository

- 카테고리: domain · 사이즈: S · 담당: BE
- 작업 내용:
  - `domain/history/EmploymentHistory.kt` — append-only Entity. UPDATE 막기 위해 setter 노출 금지.
  - `EmploymentHistoryEventType` enum 7종
  - JSON oldValue/newValue 직렬화 (`@Type(JsonStringType::class)`)
  - `domain/history/EmploymentHistoryRepository.kt` interface + 구현 (effectiveDate desc 정렬)
- 선행: DB-01, CM-01
- 후행 의존: BE-05
- 테스트 케이스: 동일 employment_id에 대해 effectiveDate desc로 발령 이력 N건이 정렬 조회된다

---

### BE-05 — PersonDomainService + EmploymentDomainService

- 카테고리: domain-service · 사이즈: M · 담당: BE
- 작업 내용:
  - `domain/person/PersonDomainService.kt` — `findOrCreate()`, `updateContact()`, `updateEmergencyContacts()`
  - `domain/employment/EmploymentDomainService.kt` — `hire()`, `recordEvent()`, `cancelEvent()`, `suspend()`, `resume()`, `resign()`. **Repository · Gateway · DomainEventPublisher 직접 의존 + DomainEvent 발행**
  - `domain/history/EmploymentHistoryDomainService.kt` — `findByEmployment()`, `rebuildAt(yyyymm)` (시점 재구성)
  - `domain/query/EmployeeQueryDomainService.kt` — `viewer + criteria → page` 권한 자동 범위 필터링 (TEAM_LEAD/HR_MANAGER 분기). Department path 트리 활용
  - 단위 테스트(Kotest BehaviorSpec + MockK) — 도메인 서비스 4종 메서드별
  - 메서드 15줄 이내 강제 (`be-code-convention.md`)
  - 다른 도메인 패키지 import 금지 (Employment ↔ Department는 ID만)
- 선행: BE-01, BE-02a, BE-02b, BE-03, BE-04, CM-01
- 후행 의존: BE-08a~e, BE-09
- 테스트 케이스: EmploymentDomainService.hire() 호출 시 Person 1건 + Employment 1건 + EmploymentHistory(HIRE) 1건 + EmployeeHiredEvent 1건이 한 트랜잭션에서 발행되고, EmployeeQueryDomainService.search(viewer=TEAM_LEAD)는 viewer 자기 팀 path 하위 직원만 반환한다

---

### BE-06 — DepartmentDomainService (이벤트 발행 포함)

- 카테고리: domain-service · 사이즈: M · 담당: BE
- 작업 내용:
  - `domain/department/DepartmentDomainService.kt` — `create()`, `moveTo()`, `assignHead()`, `removeHead()`
  - 부서 이동 시 path 재계산 + 자식 일괄 UPDATE + DepartmentChangedEvent 발행
  - 부서장 변경 시 새 부서장 Employment.status=ACTIVE 검증
  - **DomainEventPublisher 의존 명시** (TPM 검수 누락 보강)
- 선행: BE-03, BE-02b (부서장 검증용), CM-01
- 후행 의존: BE-10
- 테스트 케이스: assignHead() 호출 시 ON_LEAVE Employment를 부서장으로 지정 시도하면 `IneligibleHeadException` 발생한다

---

### BE-07 — KafkaDomainEventPublisher 구현

- 카테고리: infrastructure · 사이즈: S · 담당: BE
- 작업 내용:
  - `infrastructure/kafka/KafkaDomainEventPublisher.kt` — `DomainEventPublisher` 구현
  - 토픽 라우팅: 13종 이벤트 → `event.hr.employee` (9종 정방향 + 4종 보상 `*.cancelled` — ADR-002 §발령 취소 보상 이벤트)
  - ZonedDateTime UTC ISO-8601 직렬화 (LocalDateTime 금지, `zoned_datetime` 룰)
  - `@TransactionalEventListener(AFTER_COMMIT)` 또는 outbox 패턴 (선택은 구현 단계에서 트레이드오프 검토 — ADR-005로 분리 예정)
  - Testcontainers Kafka 통합 테스트 (13종 페이로드 각각 검증)
- 선행: KF-01, CM-01
- 후행 의존: BE-12 (DomainService BE-05/BE-06은 인터페이스(domain layer)에만 의존하므로 컴파일 선행 아님. wave 4 조기 배치는 통합 테스트 영향 차단 목적)
- 테스트 케이스: DomainEvent 13종이 각각 정확한 JSON 페이로드로 `event.hr.employee` 토픽에 발행된다 (Testcontainers Kafka)

---

### BE-08a — UseCase: HireEmployee + Resign

- 카테고리: application · 사이즈: M · 담당: BE
- 작업 내용:
  - `application/employee/HireEmployeeUseCase.kt` — `@Transactional`, EmploymentDomainService.hire() 호출
  - `application/employee/ResignEmploymentUseCase.kt` — `@Transactional`, EmploymentDomainService.resign() 호출
  - Command · Response data class
  - `execute()` 10줄 이내 강제 — Repository · Publisher 직접 호출 금지
- 선행: BE-05, BE-07
- 후행 의존: BE-11a
- 테스트 케이스: HireEmployeeUseCase 단위 테스트(DomainService 모킹)에서 execute() 가 정확히 1회 DomainService.hire() 를 호출한다

---

### BE-08b — UseCase: RecordEmploymentEvent + Cancel

- 카테고리: application · 사이즈: S · 담당: BE
- 작업 내용:
  - `application/employee/RecordEmploymentEventUseCase.kt` — 승진/부서이동/연봉변경
  - `application/employee/CancelEmploymentEventUseCase.kt` — 직전 1건만 취소 허용 (그 이전 이력 변경 금지)
- 선행: BE-05
- 후행 의존: BE-11a
- 테스트 케이스: CancelEmploymentEventUseCase 가 직전이 아닌 이력에 대해 호출되면 `IneligibleCancellationException` 발생한다

---

### BE-08c — UseCase: Suspend + Resume

- 카테고리: application · 사이즈: S · 담당: BE
- 작업 내용:
  - `application/employee/SuspendEmploymentUseCase.kt` — `@Transactional`
  - `application/employee/ResumeEmploymentUseCase.kt` — `@Transactional`
- 선행: BE-05
- 후행 의존: BE-11a
- 테스트 케이스: SuspendEmploymentUseCase 가 RESIGNED Employment에 대해 호출되면 `InvalidStateTransitionException` 발생한다

---

### BE-08d — UseCase: UpdatePersonalInfo + UpdateEmergencyContacts (본인 영역)

- 카테고리: application · 사이즈: S · 담당: BE
- 작업 내용:
  - `application/employee/UpdatePersonalInfoUseCase.kt` — `@Transactional`, PersonDomainService.updateContact() 호출. 개인 영역만 (연락처·주소). 본인 외 호출 시 ForbiddenException.
  - `application/employee/UpdateEmergencyContactsUseCase.kt` — `@Transactional`, PersonDomainService.updateEmergencyContacts() 호출. JSON 배열 갱신.
  - 단위 테스트 — viewer가 본인이 아니면 차단되는 케이스 검증
- 선행: BE-05
- 후행 의존: BE-11a
- 테스트 케이스: UpdatePersonalInfoUseCase가 본인 외 personId로 호출되면 `ForbiddenException` 발생한다

---

### BE-08e — UseCase: 조회 (Search/Get + 발령 이력)

- 카테고리: application · 사이즈: S · 담당: BE
- 작업 내용:
  - `application/employee/SearchEmployeesUseCase.kt` — read-only. EmployeeQueryDomainService.search() 호출. 권한 자동 범위 필터링 (TEAM_LEAD/HR_MANAGER 분기)
  - `application/employee/GetEmployeeUseCase.kt` — read-only. 권한 검증(`Employment.isAccessibleBy(viewer)`)
  - `application/employee/GetEmploymentHistoryUseCase.kt` — read-only. EmploymentHistoryDomainService.findByEmployment() 호출
  - 단위 테스트 — TEAM_LEAD viewer가 다른 팀 직원 조회 시 ForbiddenException 검증
- 선행: BE-05 (EmployeeQueryDomainService + EmploymentHistoryDomainService 포함)
- 후행 의존: BE-11a
- 테스트 케이스: TEAM_LEAD viewer가 자기 팀 path 외 직원을 GetEmployeeUseCase로 조회하면 `ForbiddenException` 발생한다

---

### BE-09 — UseCase: BulkHire + BulkRecordEmploymentEvents (CSV)

- 카테고리: application · 사이즈: M · 담당: BE
- 작업 내용:
  - `application/employee/BulkHireUseCase.kt` — CSV 파싱 + 전부 성공 or 전부 롤백 (`@Transactional`)
  - `application/employee/BulkRecordEmploymentEventsUseCase.kt` — 동일 패턴
  - 결과 리포트 (성공 수 · 실패 수 · 실패 사유 리스트)
- 선행: BE-05
- 후행 의존: BE-11b
- 테스트 케이스: 100명 CSV 중 1명이 유효성 실패하면 트랜잭션 전체가 롤백되고 0건 저장된다

---

### BE-10 — UseCase: 부서 CRUD + 이동 + 부서장 변경

- 카테고리: application · 사이즈: M · 담당: BE
- 작업 내용:
  - `application/department/CreateDepartmentUseCase.kt`
  - `application/department/MoveDepartmentUseCase.kt`
  - `application/department/AssignDepartmentHeadUseCase.kt`
- 선행: BE-06, BE-07
- 후행 의존: BE-11a
- 테스트 케이스: MoveDepartmentUseCase 가 자기 자신을 부모로 지정하면 `CircularDepartmentException` 발생한다

---

### BE-11a — Controller: EmployeeApiController + DepartmentApiController

- 카테고리: presentation · 사이즈: M · 담당: BE
- 작업 내용:
  - `presentation/EmployeeApiController.kt` — `/employees/*` (목록 · 상세 · 입사 · 발령 · 휴직/복직 · 퇴사 · 발령 이력)
  - `presentation/DepartmentApiController.kt` — `/departments/*` (CRUD · 이동 · 부서장)
  - `presentation/MyEmployeeApiController.kt` — `/employees/me`, `/me/emergency-contacts`
  - JWT subject → Employment 추출 (auth-service 연동은 stub, 실제 RBAC은 auth-service 도메인 작업)
  - 권한 자동 범위 필터링 인터셉터 또는 메서드 어노테이션 (TEAM_LEAD/HR_MANAGER 분기)
- 선행: BE-08a, BE-08b, BE-08c, BE-10
- 후행 의존: BE-12
- 테스트 케이스: TEAM_LEAD 가 다른 팀 직원 상세 조회 시 403 Forbidden 을 반환한다

---

### BE-11b — Controller: EmployeeBulkApiController (CSV)

- 카테고리: presentation · 사이즈: S · 담당: BE
- 작업 내용:
  - `presentation/EmployeeBulkApiController.kt` — `POST /employees/bulk`, `POST /employees/{id}/employment-events/bulk` (multipart/form-data)
  - 결과 리포트 응답 (성공/실패 수 · 실패 행 인덱스 + 사유)
- 선행: BE-09
- 후행 의존: BE-12
- 테스트 케이스: 100명 CSV 업로드 후 응답에 `successCount=100`, `failureCount=0` 이 포함된다

---

### BE-12 — E2E 시나리오 + 비기능 검증 (X1, X4)

- 카테고리: test · 사이즈: M (테스트 코드 기준) · 담당: BE
- 작업 내용 (도메인·application 단위 테스트와 infrastructure/presentation 통합 테스트는 각 티켓 작업 범위에 포함되어 있음. BE-12는 5계층 중 **scenario 레이어** 전용):
  - Testcontainers MySQL 8.0 + Kafka 환경 (실제 인프라 부트)
  - 시나리오 X1: 퇴사 처리 → Employment.RESIGNED + EmploymentHistory(RESIGN) + employee.resigned Kafka 발행 + (auth-service 이벤트 구독은 별도 검증)
  - 시나리오 X4: 100명 일괄 등록 트랜잭션 보장 (1명 실패 시 전체 롤백)
  - 부서 이동 → path 재계산 + 자식 path 갱신 + department.changed 발행
  - 부서장 휴직 → department.head_employment_id null + department.head_changed 발행
  - 발령 취소 보상 시나리오: 부서 이동 → 직전 취소 → `employee.transferred.cancelled` 발행 + EmploymentHistory cancelledAt 기록
  - 비기능 검증: 1만명 검색 < 500ms (벤치마크)
- 선행: BE-11a, BE-11b, BE-07
- 후행 의존: (M1 종료)
- 테스트 케이스: X1·X4·발령 취소 시나리오 3종이 모두 통과하며 Kafka 토픽에 정확한 이벤트가 발행된다

---

## 의존 그래프 (DAG)

| 티켓 | 선행 |
|------|------|
| BS-01 | — |
| CM-01 | BS-01 |
| DB-01 | BS-01 |
| KF-01 | BS-01 |
| BE-01 | DB-01, CM-01 |
| BE-02a | CM-01 |
| BE-02b | BE-02a, DB-01 |
| BE-03 | DB-01, CM-01 |
| BE-04 | DB-01, CM-01 |
| BE-05 | BE-01, BE-02a, BE-02b, BE-03, BE-04, CM-01 |
| BE-06 | BE-03, BE-02b, CM-01 |
| BE-07 | KF-01, CM-01 |
| BE-08a | BE-05, BE-07 |
| BE-08b | BE-05 |
| BE-08c | BE-05 |
| BE-08d | BE-05 |
| BE-08e | BE-05 |
| BE-09 | BE-05 |
| BE-10 | BE-06, BE-07 |
| BE-11a | BE-08a, BE-08b, BE-08c, BE-08d, BE-08e, BE-10 |
| BE-11b | BE-09 |
| BE-12 | BE-11a, BE-11b, BE-07 |

## Wave 스케줄 (위상정렬 결과)

| Wave | 티켓 | 너비 | 비고 |
|:-:|---|:-:|---|
| 1 | BS-01 | 1 | 인프라 부트스트랩 단독 병목 |
| 2 | CM-01, DB-01, KF-01 | 3 | 공통 산출물 3종 동시 |
| 3 | BE-01, BE-02a, BE-03, BE-04 | 4 | 도메인 패키지 4종 fan-out (BE-02b는 BE-02a 의존이라 다음 wave) |
| 4 | BE-02b, BE-07 | 2 | Employment Repository + Kafka Publisher |
| 5 | BE-05, BE-06 | 2 | DomainService 2종 (BE-05에 EmployeeQueryDomainService + EmploymentHistoryDomainService 포함) |
| 6 | BE-08a, BE-08b, BE-08c, BE-08d, BE-08e, BE-09, BE-10 | 7 | UseCase fan-out (Single Writer 안전: 각 UseCase는 자기 파일만) |
| 7 | BE-11a, BE-11b | 2 | Controller — 11a/11b는 다른 파일이므로 같은 wave |
| 8 | BE-12 | 1 | E2E 시나리오 최종 게이트 |

### Fan-out 통계

- 총 티켓: **21개** (PR #117 p2 보강에서 BE-08d/BE-08e 추가)
- 총 wave: 8
- 평균 wave 너비: (1+3+4+2+2+7+2+1) / 8 = **2.75**
- 최대 wave 너비: 7 (Wave 6)
- 4인 팀 가동률: 7/4=175% (Wave 6) · 4/4=100% (Wave 3) · 3/4=75% (Wave 2) · 평균 ≈ **69%**
- 직선화 비율 (너비 1): 2/8 = 25% — `ticket-guide.md`의 50% 이하 기준 통과
- 70% 목표 미달분(약 1%)은 본 도메인이 SSOT라 통합 게이트(BE-12)·공통 Controller(BE-11a) 단독 wave가 본질적 병목

### Single Writer per File 검증

- Wave 3: BE-01(person/), BE-02a(employment/), BE-03(department/), BE-04(history/) — **신규 패키지 각자 다른 디렉토리** ✅
- Wave 4: BE-02b(infrastructure/employment/), BE-07(infrastructure/kafka/) — **다른 디렉토리** ✅
- Wave 5: BE-05(person+employment+history+query DomainService 4종), BE-06(department DomainService) — **다른 디렉토리** ✅
- Wave 6: BE-08a/b/c/d/e(application/employee/ 안에서 각자 다른 UseCase 파일), BE-09(application/employee/Bulk*), BE-10(application/department/) — **각 UseCase는 독립 파일** ✅
- Wave 7: BE-11a(presentation/Employee*Controller + Department*Controller), BE-11b(presentation/EmployeeBulkApiController) — **다른 파일** ✅

모든 wave에서 같은 파일을 동시 수정하지 않습니다.

### 5계층 테스트 분리 매핑 (`be-code-convention.md` 필수 테스트 레이어)

| 레이어 | 적용 티켓 |
|---|---|
| domain (단위) | BE-01, BE-02a, BE-03, BE-04 (Entity + 상태머신·트리 알고리즘 단위 테스트) |
| domain-service (단위) | BE-05, BE-06 (Kotest + MockK, DomainService 4+1종) |
| application (단위) | BE-08a~e, BE-09, BE-10 (DomainService 모킹) |
| infrastructure (통합) | BE-02b, BE-07 (Testcontainers MySQL + Kafka) |
| presentation (통합) | BE-11a, BE-11b (MockMvc/WebTestClient + 권한 인터셉터) |
| scenario (E2E) | BE-12 (X1·X4·발령 취소 보상 3종) |

## 후속 단계

1. **티켓 트래커 등록**: `/jira-ticket` 스킬 또는 Notion DB 등록 (사용자 호출 시점)
2. **위키 동기화**: `/doc-sync` 스킬로 본 TDD + ADR + 티켓 트래커 동기화 (사용자 호출 시점)
3. **구현 wave 진입**: 별도 `/feature` 또는 `/implement` 호출
   - 다음 호출에서 BS-01 단독 wave 스폰 → 머지 → Wave 2~8 순차 진행
   - 각 wave는 worktree 분기 + 병렬 스폰 + PR 생성 + pr-reviewer 후처리

## 작업 범위 외 (다음 호출에서 처리)

- 코드 작성 (Entity / UseCase / Repository / Controller / 테스트 Kotlin 파일)
- Flyway 마이그레이션 SQL 실제 작성
- Kafka Terraform 실제 작성
- `gh pr create`, hook 셀프리뷰, pr-reviewer 호출
- 다른 도메인(auth/attendance/leave/payroll/performance/notification) — 별도 `/feature` 호출

## Document History

| 날짜 | 변경 내용 | 작성자 |
|---|---|---|
| 2026-05-16 | 초안 — TPM 분석 16티켓 → 검수 권고에 따라 L 분할 후 19티켓, Wave 8 DAG | 메인 세션 |
| 2026-05-17 | PR #117 pr-reviewer p2 보강 6건 반영 — BE-08d/BE-08e 신규 추가(UC 5종), BE-05에 EmployeeQueryDomainService 책임 명시, BE-07 의존 표기 정정, BE-12 5계층 분리, 발령 취소 보상 이벤트 4종 추가, wave 너비 수치 정정(2.5→2.75, 가동률 62%→69%). 총 21티켓 | 메인 세션 |
