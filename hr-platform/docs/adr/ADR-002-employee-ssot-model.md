# ADR-002 — Employee SSOT 모델 (Person + Employment + Department + EmploymentHistory)

**상태**: Accepted
**일자**: 2026-05-16
**작성자**: 메인 세션
**관련 문서**: ADR-001, TDD-001-employee-service.md, PRD §5.1·5.2·6.1·9.2

## Context

employee-service는 hr-platform의 **단일 진실 원천(SSOT)** 입니다. attendance·leave-approval·payroll·performance·notification 등 모든 후행 도메인이 이 서비스에서 발행하는 데이터·이벤트에 의존합니다. SSOT 모델이 잘못 잡히면 5개월 MVP 전체가 흔들리고, 후속 도메인 마이그레이션 비용이 폭증합니다.

PRD §6.1이 4개 엔티티를 제안합니다:
- **Person** (불변 신원: 이름·연락처·생년월일·국적·성별)
- **Employment** (고용 인스턴스: 회사·사번·고용형태·상태·기간·국가·통화·타임존·조직·정책·보상)
- **Department** (조직: 회사·부모·path·부서장·유효기간)
- **EmploymentHistory** (발령 이력: 이벤트유형·oldValue·newValue·effectiveDate)

이 ADR은 위 4개 엔티티를 어떻게 책임 분리하고 도메인 메서드를 캡슐화할지 결정합니다.

## Decision

### 1. Person ↔ Employment 1:N 분리

| 항목 | 결정 |
|---|---|
| Person | **불변 신원**. 한 사람이 여러 회사에 고용될 수 있음 (글로벌 확장 대비). 개인 식별 정보(PII)만 보유. |
| Employment | **고용 인스턴스**. Person·Company 페어당 하나. 상태·정책·조직·보상은 모두 여기에. 퇴사 후 재입사 시 새 Employment 생성. |
| 관계 | Person 1 ↔ N Employment (`employment.personId` FK 컬럼 미사용, ID 참조만) |

### 2. Department 트리 — `path` 머터리얼라이즈드 패스

| 항목 | 결정 |
|---|---|
| 트리 모델 | Materialized Path. `path` 컬럼에 부모 경로를 `/` 구분자로 저장 (예: `/1/12/35/`) |
| `parentId` | 동시에 유지 (간단한 부모 조회는 parentId, 서브트리 조회는 path LIKE) |
| `order` | 같은 부모 안 정렬 순서. 드래그&드롭 이동 시 갱신. |
| 유효기간 | `effectiveFrom`·`effectiveTo`로 폐지된 부서 이력 보존 (조직 개편 추적) |
| 부서장 | `headEmploymentId` — Employment 참조. 부서장이 휴직/퇴사하면 null 또는 후임으로 갱신, `department.head_changed` 이벤트. |

### 3. EmploymentHistory 불변 이력

| 항목 | 결정 |
|---|---|
| 책임 | **모든** Employment 변경(채용·승진·부서이동·연봉변경·휴직·복직·퇴사)을 append-only 로그로 보존 |
| `oldValue`·`newValue` | JSON 직렬화 문자열 (`@Type(JsonStringType::class)`, ObjectMapper 직접 사용 금지) |
| 스키마 | 이벤트유형별 부분 스냅샷(전체 Employment 직렬화 금지) — `{"departmentId":12}` → `{"departmentId":35}` |
| 불변성 | UPDATE 금지, INSERT만. `effectiveDate`로 정렬해 시점별 상태 재구성 가능. |
| 이벤트 발행 | EmploymentHistory 생성과 같은 트랜잭션에서 `event.hr.employee.*` 이벤트 발행 (DomainEventPublisher) |

### 4. Employment 상태 머신 (PRD §7 누락 보강)

```
[*]  ─hire──▶  PRE_HIRED  ─activate──▶  ACTIVE  ─suspend──▶  ON_LEAVE
                                          │                      │
                                          │                      └─resume──▶ ACTIVE
                                          │                      └─resign──▶ RESIGNED
                                          └─resign──▶ RESIGNED
RESIGNED  ─[*]  (재입사는 새 Employment 생성)
```

| 전이 | Domain Event | 호출 메서드 |
|---|---|---|
| PRE_HIRED → ACTIVE | `employee.hired` | `Employment.activate(now)` |
| ACTIVE → ON_LEAVE | `employee.suspended` | `Employment.suspend(reason, until)` |
| ON_LEAVE → ACTIVE | `employee.resumed` | `Employment.resume(now)` |
| ACTIVE → RESIGNED | `employee.resigned` | `Employment.resign(now, reason)` |
| ON_LEAVE → RESIGNED | `employee.resigned` | `Employment.resignDuringLeave(now)` |
| 부서 변경 | `employee.transferred` + `department.changed` | `Employment.transferTo(newDeptId, now)` |
| 직책 변경 | `employee.promoted` | `Employment.promote(newPositionId, now)` |
| 연봉 변경 | `employee.salary_changed` | `Employment.changeCompensation(newComp, now)` |

**금지 전이**: PRE_HIRED → RESIGNED 직접, RESIGNED → 다른 상태, RESIGNED → 재활성 (Entity의 검증 메서드에서 throw).

### 5. Rich Domain Model — Entity 캡슐화 메서드 목록

#### Person
- 자기 자신 검증: `validateNotMinor()` (미성년자 입사 제한)
- 정보 변경: `updateContact(phone, email)`, `updateAddress(...)` (PII 변경 audit log)

#### Employment
- 상태 전이: 위 7종 메서드 (각각 상태 검증 + DomainEvent 적재)
- 검증: `validateActive()`, `validateNotResigned()`, `validateBelongsToCompany(companyId)`
- 권한 범위 판정: `isAccessibleBy(viewerEmployment: Employment)` — TEAM_LEAD 자동 범위 필터링용
- 비용 계산: `calculateProrated(yearMonth)` (월 중 입퇴사 일할 계산, payroll-service에서 호출)

#### Department
- 트리 조작: `moveTo(newParent: Department)` (path 재계산 + 자식 path 갱신은 Domain Service가 수행)
- 부서장: `assignHead(employmentId)`, `removeHead()`
- 유효성: `validateActive(date)`

#### EmploymentHistory
- 생성: `Employment` 메서드 안에서 `pullDomainEvents()`로 적재. 직접 생성 금지.
- 조회: 시점별 재구성 — `Employment.rebuildAt(yyyymm)` (감사/이력 조회용)

### 6. 비밀번호·인증 정보 미보유

- `UserAccount`·인증 정보·비밀번호는 **auth-service**(별도 도메인)가 소유.
- employee-service는 `personalEmail`·`phoneNumber` 등 PII만 저장하고, 로그인 ID·비밀번호·2FA·세션은 직접 다루지 않음.
- employee → auth 연결 키: `employmentId` (auth가 UserAccount.employmentId로 참조).

### 7. 다른 도메인과의 경계

| 다른 도메인 | employee가 제공 | employee가 받지 않음 |
|---|---|---|
| auth | `employmentId`·`role`·`companyId` (이벤트 + 권한 검증 호출) | 비밀번호·세션·OTP |
| attendance | `employmentId`·`workSchedulePolicyId`·`timezone` | 출퇴근 데이터 |
| leave-approval | `employmentId`·`departmentId`·`leavePolicyId`·`managerEmploymentId` | 휴가 신청·결재 데이터 |
| payroll | `employmentId`·`compensation`·`country`·`currency` | 급여 계산 결과 |
| performance | `employmentId`·`managerEmploymentId` | OKR·1:1 노트 |
| notification | `employmentId`·`preferredChannel` (이벤트로 부여) | 발송 이력 |

employee는 다른 도메인 데이터를 **import하지 않습니다**. ID(Long)만 보유합니다.

## Alternatives Considered

### A. Single User Entity (Person + Employment 통합)

- Workday 이전 세대 모델. 한 사람·한 회사·한 고용으로 가정.
- **미채택 사유**: PRD 1.1·1.2가 명시적으로 "글로벌 확장 가능한 Person+Employment 모델" 채택. 한 사람이 여러 회사를 거치거나(연결 채용·인수합병) 다국적 고용 인스턴스를 가질 수 있는 미래를 차단함.

### B. Department FK 대신 EAV(Entity-Attribute-Value) 트리

- 부서 속성이 회사마다 다르다는 가정으로 EAV 채택.
- **미채택 사유**: Phase 2 다국가 확장에서도 부서는 고정 속성(이름·코드·부서장·유효기간)이 충분. EAV는 쿼리·인덱스 복잡도 폭증.

### C. EmploymentHistory를 Event Sourcing의 Aggregate Root로

- Employment를 EventStore에서 재구성. Read Model 분리.
- **미채택 사유**: 5개월 MVP에 Event Sourcing 학습·인프라 비용 과다. Append-only Log + 현재 상태 동시 보존(snapshot)이 ROI 우월.

### D. Adjacency List만 (parentId만 보유, path 미보유)

- 단순 부모-자식 관계만.
- **미채택 사유**: PRD §8.4 "직원 검색(1만명) < 500ms"·조직도 트리 뷰 요구. 서브트리 LIKE 조회를 N단 JOIN으로 풀면 성능 미달.

### E. Closure Table (별도 ancestor-descendant 테이블)

- 트리 조회 성능 가장 우수.
- **미채택 사유**: 부서 이동마다 closure 테이블 N행 갱신. 50~300인 SMB 부서 수(~30개) 규모에서는 path가 더 단순. 1000+ 부서가 되면 마이그레이션 검토.

## Rationale

1. **Person + Employment 분리**가 PRD 1.2 "글로벌 확장 사전 대비" 요구를 만족하는 유일한 안.
2. **상태 머신 명시화**가 PRD §7 누락을 메우고, payroll/attendance/leave가 모두 의존하는 상태 전이의 일관성 보장.
3. **EmploymentHistory append-only**가 audit 5년 보관(§10.2) + 컴플라이언스 위반 시점 추적을 한 번에 해결.
4. **Materialized Path**가 SMB 규모(부서 수 < 100)에서 트리 조회 성능과 구현 단순성의 균형점.
5. **Rich Domain Model**이 메모리 룰 `encapsulation`·`be-code-convention.md`와 정확히 일치 — UseCase에 if-throw 검증 나열 금지.
6. **인증 정보 미보유**가 auth-service와 책임 분리 — employee는 "사람과 고용", auth는 "로그인과 권한".

## Consequences

### Positive
- 글로벌 확장(Phase 2 일본·미국) 시 Employment의 `country`·`currency`·`timezone`만 다른 값으로 새 Employment 생성하면 Person 재사용 가능.
- 상태 머신이 코드로 캡슐화 → 잘못된 전이가 정적으로 차단됨 (`Employment.validate*` 메서드).
- 모든 변경이 EmploymentHistory에 남아 audit·컴플라이언스 대응 용이.

### Negative
- Person 1:N Employment 모델은 단순한 1:1 가정 코드보다 쿼리·검색이 복잡 (디폴트 활성 Employment 조회 로직 필요).
- Materialized Path는 부서 이동 시 자식 path 일괄 갱신 필요. 트랜잭션 안에서 처리.
- EmploymentHistory가 빠르게 자람. 정기 파티셔닝·아카이빙 정책 필요 (Phase 1.5).

### Risks
- `Employment.compensation`을 JSON으로 저장하면 payroll-service에서 쿼리·집계가 어려움. 정규화 컬럼(`baseSalary`·`currency`)을 함께 두는 하이브리드 권장 — TDD에서 구체화.
- 부서장(`headEmploymentId`)이 퇴사하면 부서가 head-less 상태. notification으로 HR에 알림 필요 (TDD 시퀀스 참조).

## Verification

ADR이 코드에 반영됐는지:

```bash
# Person·Employment 분리
ls hr-platform/employee-service/src/main/kotlin/com/hrplatform/employee/domain/
# → person/, employment/, department/, history/, common/ 5개 디렉토리

# Anemic 검사 — Employment Entity에 비즈니스 메서드 존재
grep -E "fun (activate|suspend|resume|resign|transferTo|promote)" \
  hr-platform/employee-service/src/main/kotlin/com/hrplatform/employee/domain/employment/Employment.kt \
  | wc -l   # ≥ 6

# 상태 머신 enum
grep "PRE_HIRED\|ACTIVE\|ON_LEAVE\|RESIGNED" \
  hr-platform/employee-service/src/main/kotlin/com/hrplatform/employee/domain/employment/EmploymentStatus.kt \
  | wc -l   # ≥ 4

# 인증 정보 미보유 — Person/Employment에 password/totp 컬럼 0건
grep -rn "password\|totp\|otp_secret" \
  hr-platform/employee-service/src/main/kotlin/com/hrplatform/employee/domain/ | wc -l   # 0
```

위 4개 검증 모두 통과해야 employee-service 도메인 코드 완료로 인정.
