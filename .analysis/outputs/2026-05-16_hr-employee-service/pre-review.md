# PRD 사전 리뷰 — employee-service (Step 0)

**리뷰 일자**: 2026-05-16
**리뷰 대상**: hr-platform MVP PRD 中 employee 도메인 섹션
**원본 PRD**: `/Users/biuea/feature/flag_project/hr-platform/docs/prd/2026-05-16_mvp.md`
**판정**: REQUIRES_CLARIFICATION

리뷰 범위는 5.1 / 5.2 / 6.1 / 9.2 / 11.2 / 12.6(X1, X4) / 14.1로 한정합니다. 다른 도메인 검수는 본 문서에서 다루지 않습니다.

---

## 1. 명확도 (행위자 / 트리거 / 결과)

11.2 인수 기준 5건을 Given/When/Then 검증 가능성 관점에서 점검합니다.

| AC | 행위자 명확 | 트리거 명확 | 결과 검증 가능 | 판정 | 모호 포인트 |
|---|---|---|---|---|---|
| #1 입사 등록 | HR 매니저 ✅ | 저장 ✅ | Person 1 + Employment 1 + `employee.hired` 발행 ✅ | ✅ | — |
| #2 부서 이동 발령 | (행위자 누락) ❌ | 부서 이동 발령 ⚠️ | EmploymentHistory 추가 + 양쪽 부서장 알림 ⚠️ | ⚠️ | "누가 발령을 등록하는가" 불명(HR? TEAM_LEAD?) / "알림" 채널·이벤트 종류(`employee.transferred` 발행 여부) 미명시 / `effectiveDate` 미래 발령 가능 여부 미정 |
| #3 퇴사 처리 | (행위자 누락) ❌ | "즉시" ⚠️ | status=RESIGNED + 0시부로 권한 회수 ⚠️ | ⚠️ | "즉시"의 정의 불명(API 호출 시점? `effectiveDate` 도래 시점?) / "출근/휴가/결재 권한 회수" 주체 미명시(auth-service 동기 호출인지 이벤트 기반 비동기인지) / 진행 중 결재·휴가의 처리 정책(12.6 X1에 일부 기술되어 있음 — 본문과 연결 필요) |
| #4 권한 경계 | TEAM_LEAD ✅ | 다른 팀 직원 상세 조회 ✅ | 403 Forbidden ✅ | ✅ | "다른 팀"의 정의 명확화 필요(직속 부서? 자손 부서까지?) — 9.2 권한 매트릭스에 자동 범위 필터링 명시 부재 |
| #5 조직도 부서 이동 | HR 매니저(추정) ⚠️ | 드래그&드롭 저장 ✅ | parentId 변경 + 하위 path 재계산 ✅ | ⚠️ | 사이클 방지(자기 자손으로의 이동 차단) 미명시 / `path` 재계산 알고리즘 정의(머터리얼라이즈드 패스 길이 제한 등) 부재 / 이벤트 발행 여부(`employee.transferred`는 직원 이동 의미이므로 부서 자체 이동 이벤트가 별도로 필요한지) 불명 |

종합: AC #2, #3, #5에서 행위자·트리거 명확성이 부족합니다.

---

## 2. API 커버리지

9.2의 11개 API를 화면 요구사항·X1·X4 시나리오와 매핑합니다.

| 화면/요구사항 | 대응 API | 누락 여부 |
|---|---|---|
| 직원 검색/리스트 (이름·부서·직책 필터) | GET /employees | ✅ |
| 직원 상세 (프로필 + 발령 이력 + 휴가 잔여 + 평가 이력) | GET /employees/{id} | ⚠️ 발령 이력 단독 조회 API 부재(`GET /employees/{id}/employment-events` 또는 `/history`) — 상세 응답 내 임베딩인지 별도 페이지네이션 API인지 불명 |
| 조직도 트리 뷰 + 드래그&드롭 부서 이동 | GET /departments / PATCH /departments/{id} | ⚠️ "드래그&드롭으로 부서 이동"이 부서 자체 이동(parentId)인지 직원 부서 이동인지 모호. 직원 부서 이동은 `POST /employees/{id}/employment-events`로 가야 하는데, 화면 설명에 명시 없음 |
| 발령 등록 단건 | POST /employees/{id}/employment-events | ✅ |
| 발령 등록 일괄 CSV | ❌ **누락** | 화면에는 "단건/일괄 CSV"라고 명시되어 있는데 일괄 등록 API가 없음. `POST /employees/employment-events/bulk` 또는 `POST /employees/employment-events/import` 필요 |
| 내 프로필 편집 (개인 정보, 비상연락처) | PATCH /employees/me | ⚠️ "비상연락처"가 Person/Employment 어느 엔티티에도 정의되어 있지 않음(emergencyContactName/Phone/Relation 필드 자체가 6.1에 없음) |
| X4 100명 일괄 입력 | ❌ **누락** | 직원 일괄 등록 API(`POST /employees/bulk` or `/import`)가 없음. 12.6 X4가 "엑셀 100명 일괄 입력 + 트랜잭션 보장"을 요구하지만 단건 POST /employees만 정의됨 |
| 부서장 변경 | ⚠️ PATCH /departments/{id}로 추정 가능하나 명시 없음 | Department.headEmploymentId 변경이 `employee.transferred`/`employee.promoted`와 어떻게 연계되는지 불명 |
| 발령 이력 조회 (단독 페이지) | ❌ **누락** | 발령 이력 페이지네이션·필터링 API 부재 |
| 발령 취소/철회 | ❌ **누락** | 잘못 등록된 발령을 어떻게 정정하는지(soft delete? 반대 이벤트?) 미정의 |
| 부서 삭제 / 비활성화 | ❌ **누락** | DELETE /departments/{id} 부재. effectiveTo 사용으로 추정되나 API에 노출 없음 |
| 직원 활성/비활성 토글 (휴직 등 ON_LEAVE 전이) | ❌ **누락** | Employment.status=ON_LEAVE 전이 API 부재(휴직은 LeaveRequest의 SICK/PARENTAL로만 처리되는지 별도 발령인지 모호) |
| 조직도 조회 (트리 깊이/특정 부서 하위만) | GET /departments | ⚠️ 응답 구조(전체 트리 한 번에? lazy load?) 명시 없음. 대규모 조직(부서 200+)에서 성능 정책 부재 |

종합: **CSV 일괄 등록 2종(직원/발령), 발령 이력 단독 조회, 비상연락처 모델, 발령 취소, ON_LEAVE 전이 API**가 누락되었습니다.

---

## 3. 이벤트 커버리지

| 비즈니스 액션 | 발행 이벤트 | 누락 여부 |
|---|---|---|
| 입사 | `employee.hired` | ✅ |
| 퇴사 | `employee.resigned` | ✅ |
| 승진 | `employee.promoted` | ✅ |
| 부서 이동 | `employee.transferred` | ✅ |
| 연봉 변경 | `employee.salary_changed` | ✅ |
| 휴직 시작 (ON_LEAVE 전이) | ❌ **누락** | `employee.suspended` / `employee.on_leave_started` 부재. EmploymentHistory.eventType에는 SUSPEND/RESUME가 있는데 Kafka 이벤트로는 발행하지 않음 |
| 복직 (RESUME) | ❌ **누락** | `employee.resumed` 부재. Payroll/Attendance가 복직 직원 일할 계산을 트리거하려면 필요 |
| 부서 자체 변경 (이름/parentId 변경) | ❌ **누락** | `department.changed` 같은 이벤트 부재. 다른 도메인이 부서 트리를 캐시한다면 동기화 불가 |
| 부서장 변경 | ❌ **누락** | 결재선·OKR 정렬·1:1 매니저 매핑이 부서장 ID에 의존하는데 변경 이벤트 없음 |
| 매니저(직속 상사) 변경 | ❌ **누락** | `managerEmploymentId` 변경 시 결재선·1:1·알림 라우팅이 깨질 수 있음. 별도 이벤트가 없으면 `employee.transferred` 페이로드에 포함되어야 함이 명시되어야 함 |
| Person(불변 신원) 정보 변경 | ⚠️ | "불변 신원"이라 명명되어 있으나 이메일·전화번호·이름은 변경 가능. 이벤트 미발행 정책이라면 PRD에 명시 필요 |
| Employment 정보 일반 PATCH | ⚠️ | `PATCH /employees/{id}`로 어떤 필드 변경 시 어떤 이벤트가 발행되는지 매핑표 부재 |

종합: **휴직/복직/부서 변경/부서장 변경 이벤트가 누락**되었고, PATCH가 어떤 이벤트를 트리거하는지 매핑이 없습니다.

---

## 4. be-code-convention 적용

### 4.1 Rich Domain Model 후보 메서드

`be-code-convention.md` 기준으로 Entity에 캡슐화되어야 할 비즈니스 로직을 식별합니다.

**Person**
- `changeContact(email, phone)` — 변경 가능 필드 검증
- `validatePersonalEmailFormat()` — 포맷 검증
- (불변 신원이라면 `birthDate`·`nationality`·`gender`는 setter 불가 메서드로만 구성)

**Employment** ← 핵심 Rich Domain Model 후보
- `validateCanTransitTo(newStatus)` — 상태 머신 진입 검증 (§9 누락 항목 참조)
- `resign(effectiveDate, reason)` — RESIGNED 전이, EmploymentHistory 적재
- `startLeave(reason)` / `resume()` — ON_LEAVE ↔ ACTIVE
- `promote(newPositionId, effectiveDate)` — PROMOTION 이벤트 적재
- `transferDepartment(newDepartmentId, effectiveDate)` — DEPT_CHANGE 적재
- `changeSalary(newAmount, effectiveDate)` — SALARY_CHANGE 적재
- `assignManager(managerEmploymentId)` — 자기 자신 / 사이클 방지 검증
- `validateOwnsBy(userId)` / `validateInSameDepartmentAs(otherEmployment)` — 권한 경계 검증

**Department**
- `moveTo(newParentId)` — 자기 자손으로 이동 차단, path 재계산 트리거
- `assignHead(employmentId)` — 부서장 변경, 활성 직원 검증
- `recalculatePath()` — 머터리얼라이즈드 패스 갱신
- `validateNoCircularReference(newParentId)` — 사이클 검증
- `deactivate(effectiveTo)` — 부서 비활성

**EmploymentHistory**
- `static record(employment, eventType, oldValue, newValue, effectiveDate, actor, note)` — 불변 객체 생성 팩토리

PRD에서 위 메서드 후보들이 명시되어 있지 않습니다. TPM이 TDD 단계에서 Entity 메서드 시그니처를 보강해야 합니다.

### 4.2 Gateway 패턴 필요 여부

- **현재 PRD 범위(M1)**: 외부 시스템 호출 없음. ATS 연동은 Phase 2(15.1 가정)로 분리. Gateway 패턴 즉시 도입 불요.
- **단, 횡단 시나리오 X1(퇴사 즉시 권한 회수)에서 auth-service 호출이 필요**. 동기 REST 호출이면 `AuthGateway`(외부 시스템 = 다른 BC), 비동기 Kafka 이벤트면 `DomainEventPublisher`로 충분. PRD에서 통신 방식이 명시되지 않아 TPM이 결정해야 합니다.
- **notification 발송**은 모든 도메인 이벤트의 Kafka 발행으로 처리(5.1 도식)되므로 Gateway 불요.

### 4.3 typealias 호환 layer

신규 도메인이므로 해당 사항 없습니다. (확인만)

### 4.4 UseCase 안티패턴 사전 가드 메모

`be-code-convention.md` §UseCase 규칙에 따라 다음을 TDD 단계에서 강제해야 합니다.

- `ResignEmployeeUseCase` 같은 UseCase가 Repository / Gateway를 직접 호출하지 않도록 `EmploymentDomainService`를 명세에 포함시킬 것
- `@Transactional`은 UseCase에 선언, DomainService는 같은 트랜잭션 안에서 동작
- Kafka 이벤트 발행은 `DomainEventPublisher` interface(domain layer)로 추상화, infrastructure에서 Kafka 구현

---

## 5. 인프라 부트스트랩 가정 (PRD 누락 항목)

코드 0줄·Gradle 멀티모듈 미설정 상태에서 PRD가 BE 부트스트랩에 대해 제공하는 정보는 다음과 같습니다.

| 항목 | PRD 명시 여부 | 값/디폴트 |
|---|---|---|
| 언어/프레임워크 | ✅ 14.1 M1에 "Kotlin Spring Boot 멀티모듈" 명시 | Kotlin / Spring Boot (버전 미명시) |
| Kotlin 버전 | ❌ 누락 | — |
| Spring Boot 버전 | ❌ 누락 | — |
| JDK 버전 | ❌ 누락 | — |
| Gradle 멀티모듈 구조 | ❌ 누락 | 도메인별 모듈인지 서비스별 모듈인지 미정. 단, 10.4에 "도메인별 스키마 분리" 언급 있음 |
| 공통 모듈 골격 | ❌ 누락 | common-domain / common-infra / common-presentation 등 골격 부재 |
| DB | ⚠️ 부분 | "MySQL 8.0 (Flyway)" 10.4 / "MySQL + Kafka + Redis 인프라" 14.1 M1 — Flyway 버전, MySQL Connector 버전, JPA/Hibernate vs MyBatis vs QueryDSL 선택 미정. (BE 컨벤션상 QueryDSL 강제이나 PRD가 이를 알지 못함) |
| Kafka | ✅ 토픽 네이밍 `event.hr.{domain}` 명시 / 클러스터·스키마 레지스트리·Avro vs JSON 선택 미정 |
| Redis | ⚠️ 인프라 항목에 언급 있으나 용도(세션? 캐시? Rate Limit?) 미명시 |
| 인증 / JWT 라이브러리 | ❌ 누락 | jjwt? Spring Authorization Server? 미정 |
| 로깅 / 모니터링 스택 | ✅ 10.6에 Loki / Prometheus / Pinpoint 명시 |
| 빌드/배포 파이프라인 | ❌ 누락 | GitHub Actions / Jenkins / ArgoCD 선택 미정 |
| 테스트 프레임워크 | ✅ 16에 "Kotest BehaviorSpec + Testcontainers + 95% 커버리지" 명시 |
| 코드 컨벤션 도구 | ❌ 누락 | detekt / ktlint 사용 여부 미명시 (BE 컨벤션 §UseCase 안티패턴 차단 하네스는 detekt 룰 기반으로 추정) |

**메모 (TPM에게)**:
- BE 부트스트랩(루트 build.gradle.kts, settings.gradle.kts, 공통 모듈 골격, 의존성 버전 카탈로그)을 **별도 선행 티켓**으로 분리해야 합니다. employee-service 도메인 티켓들은 이 부트스트랩 티켓이 머지된 후 wave 2에 배치합니다.
- 부트스트랩 티켓 후보: `[INFRA-01]` Gradle 멀티모듈 부트스트랩 / `[INFRA-02]` MySQL+Flyway 통합 / `[INFRA-03]` Kafka 클러스터 + 토픽 네이밍 컨벤션 / `[INFRA-04]` Spring Boot 공통 설정(Security, Jackson, ZonedDateTime 직렬화) / `[INFRA-05]` 공통 도메인 모듈(`DomainEventPublisher` interface, BaseEntity, AuditLog 기본 골격).

---

## 6. 권한 경계

11.2 AC #4가 "TEAM_LEAD가 다른 팀 직원 상세 조회 → 403"을 요구하지만 9.2 API 명세에 다음이 누락되었습니다.

| 항목 | 누락 내용 |
|---|---|
| 권한 매트릭스 반영 | 9.1 권한 매트릭스에 "팀원 정보 보기"는 있지만 9.2 API별 권한 행이 없음. 각 GET /employees / GET /employees/{id} / PATCH /employees/{id} / POST /employees/{id}/employment-events에 누가 호출 가능한지 명시 부재 |
| 자동 범위 필터링 | 10.2에 "Employment·Department 범위 자동 검증" 한 줄만 있음. TEAM_LEAD가 `GET /employees` 호출 시 자기 부서(+자손 부서?) 직원만 반환되는지, 또는 쿼리 필터로 제한되는지 명시 부재 |
| "팀"의 정의 | 직속 부서만? 자손 부서 포함? 매니저 직접 보고선만? — 정의가 없으면 AC #4의 검증이 불가능 |
| HR_MANAGER 자기 정보 수정 | 12.6 X3에 "HR 매니저 자기 연봉 수정 시 audit + CFO 알림"이 있는데 9.2 API에 이 분기 로직 미명시 |
| 데이터 마스킹 | 자기 정보가 아닌 직원의 주민번호·연봉·계좌 등 민감 필드를 EMPLOYEE/TEAM_LEAD가 볼 수 있는지 마스킹 정책 부재 |

종합: 권한 매트릭스 → API 매핑 + "팀"의 정의 + 마스킹 정책이 PRD에 보강되어야 합니다.

---

## 7. 데이터 모델 모호성

### 7.1 Department
- `path`: 어떤 형식인가? 슬래시 구분 ID(`/1/3/7/`)? 슬래시 구분 코드? path 길이 제한? — 미정
- `order`: 같은 부모 안에서의 정렬 순서로 추정되나 명시 없음. 동시 수정 시 충돌 정책(낙관적 잠금? 재배열?) 부재
- `effectiveFrom` / `effectiveTo`: 시점 의미 불명 — 부서 생성 시점인가 회사 조직 개편 effective date인가? 미래 발효 부서 허용 여부, 과거 시점 조회 API 존재 여부 모두 미정
- `headEmploymentId`: nullable 여부 / 부서장 공석 허용 여부 미정 / 한 직원이 여러 부서장 겸직 가능 여부 미정

### 7.2 EmploymentHistory.oldValue / newValue
- "JSON 직렬화 문자열, 불변"으로만 명시. 직렬화 스키마(키 네이밍, snake_case vs camelCase, null 처리)·버전 관리(스키마 진화)·검색 가능성(JSON_EXTRACT 인덱스?) 미정
- eventType별 페이로드 형식 표준(예: PROMOTION은 `{positionId, salary}`, DEPT_CHANGE는 `{departmentId, managerEmploymentId}`)이 정의되지 않음 → 컨슈머가 파싱 불가
- `createdBy`: Employment.id인가 UserAccount.id(auth)인가 — 식별자 불일치 가능성

### 7.3 Employment
- `compensation`: 6.1에 "연봉/시급/상여 (PayrollItem 참조)"로 한 줄. 직접 컬럼인가, PayrollItem 조인 결과인가, 별도 SalaryRecord 엔티티인가? — 이력 추적 방식 미정
- `employeeNumber`: 회사 내 유니크인가 전체 시스템 유니크인가, 부여 정책(자동 시퀀스? HR 수동?) 미정
- `positionId`: 6.1에 있으나 Position 엔티티 자체가 PRD에 정의되지 않음 (직급/직책 체계 누락)

### 7.4 Person
- "불변 신원"이라 명명되었으나 이메일·전화번호는 실생활에서 변경됨. 변경 가능 정책 명확화 필요
- 외국인 직원: `nationality` ISO 3166만으로 식별 충분? 외국인 등록번호·여권번호 등은 어디에 저장하는가? (15.2 위험 "다국적 직원 모델 누락" 언급은 있으나 모델 반영은 없음)
- 주민등록번호·외국인등록번호 같은 PII 컬럼이 Person에 명시되지 않음. 10.2에 "컬럼 단위 암호화" 언급은 페이슬립·급여에 한정

---

## 8. i18n / 타임존

| 항목 | PRD 명시 | 누락 사항 |
|---|---|---|
| 시간 저장 정책 | ✅ 10.4 "모든 시간 컬럼 ZonedDateTime UTC 저장" | — |
| 표시 변환 정책 | ✅ 10.4 "표시 시 employmentTimezone 변환" | 변환 주체(BE 응답에 변환된 값을 내려보내는가, FE가 변환하는가) 미정 |
| API 응답 timezone 처리 | ❌ | 9.2 API 응답에 ZonedDateTime 직렬화 형식(`2026-05-16T09:00:00+09:00` ISO-8601? epoch? `Asia/Seoul` 명시 여부) 부재 |
| 이벤트 페이로드 timezone | ❌ | Kafka 이벤트 `employee.hired` 페이로드의 `hiredAt` 필드가 UTC인지 actor timezone인지 미정 → 다운스트림 컨슈머 혼란 우려 |
| `Employment.timezone` 사용처 | ⚠️ | Employment 엔티티에 있지만 어느 API/이벤트에서 어떻게 사용되는지 명시 부재 |
| Locale / 다국어 응답 | ⚠️ | 10.4 "ko-KR만"이라 Phase 1.0에서는 Accept-Language 처리 불요로 추정. 단, 에러 메시지·이벤트 메시지의 i18n 키 적용 범위 미정 |
| 통화 표시 | ⚠️ | 10.4 "정수 minorUnits 저장" — 표시 변환 책임(BE? FE?) 미정 |

---

## 9. 누락 명세 — Employment 상태 머신

§7 상태 머신 섹션에 LeaveRequest / ApprovalForm / PayrollRun / ObjectiveCycle / AttendanceRecord는 정의되어 있으나 **Employment 상태 머신은 누락**되어 있습니다. 6.1에 `status (PRE_HIRED / ACTIVE / ON_LEAVE / RESIGNED)`만 나열되어 있고 전이 규칙·트리거·이벤트 발행 시점이 명시되지 않습니다.

### 9.1 누락된 정의

- **허용 전이**: PRE_HIRED → ACTIVE / ACTIVE → ON_LEAVE / ON_LEAVE → ACTIVE / ACTIVE → RESIGNED / ON_LEAVE → RESIGNED — 전이 가능 여부 미정. 예) PRE_HIRED에서 바로 RESIGNED 가능한가(입사 취소)? RESIGNED에서 ACTIVE 복귀(재입사) 가능한가?
- **자동 전이 트리거**: PRE_HIRED → ACTIVE 전이 시점(입사일 도래 시 자동? HR 수동 활성화?)
- **이벤트 발행 매핑**:
  - PRE_HIRED → ACTIVE 시점에 `employee.hired` 발행? 아니면 POST /employees 호출 시점에 발행?
  - ACTIVE → ON_LEAVE / ON_LEAVE → ACTIVE 시점의 이벤트는 §3에서 보았듯 누락
- **불변 조건**: RESIGNED 직원의 EmploymentHistory 추가 가능 여부 / Employment.endDate 변경 가능 여부 / departmentId 변경 가능 여부 — 모두 미정
- **AttendanceRecord·LeaveRequest와의 결합**:
  - 11.2 AC #3 "퇴사 즉시 출근/휴가/결재 권한 회수"의 실제 메커니즘 부재(이벤트 기반? 동기 호출? Employment.status 조회로 매 요청 검증?)
  - 12.6 X1 "진행 중 결재는 위임 알림 + 미사용 연차 자동 정산"의 트리거가 누락된 이벤트(`employee.resigned`의 정확한 발행 시점·페이로드)에 의존

### 9.2 추가 누락 상태 머신

- **Department 상태**: `effectiveFrom`/`effectiveTo`만 있고 ACTIVE/ARCHIVED 같은 상태 enum 없음. 부서 비활성화 시 산하 직원 자동 이동 정책 부재
- **EmploymentHistory 무효화/취소 정책**: 잘못 등록된 발령을 어떻게 정정하는지(반대 이벤트 vs soft delete) 부재

---

## 종합 판정

### 판정: REQUIRES_CLARIFICATION

다음 항목은 TPM 분석 진행 전에 PM/PO 확인이 필요합니다.

### A. PM/PO 확인 사항 (PRD 보강 필요)

1. **Employment 상태 머신** — 전이 규칙·이벤트 발행 시점·불변 조건을 §7에 추가
2. **누락 이벤트** — `employee.suspended` / `employee.resumed` / `department.changed` / `department.head_changed` 발행 여부 결정
3. **누락 API** — 직원 일괄 등록(CSV), 발령 일괄 등록(CSV), 발령 이력 조회, 발령 취소, ON_LEAVE 전이, 비상연락처 필드/엔드포인트
4. **권한 범위 정의** — TEAM_LEAD의 "팀"이 직속 부서인가 자손 포함인가, 자동 범위 필터링 적용 API 목록
5. **퇴사 즉시성 정의** — AC #3 "즉시"의 정확한 의미와 권한 회수 메커니즘(동기/비동기)
6. **EmploymentHistory.oldValue/newValue 스키마** — eventType별 페이로드 표준
7. **데이터 모델 보강** — Person의 PII 컬럼(주민번호/외국인등록번호), Position 엔티티 정의, Employment.compensation 저장 방식, Department.path 형식

### B. TPM이 보강해야 할 항목 (PRD 변경 없이 TDD 단계에서 결정 가능)

1. **BE 부트스트랩 티켓 분리** — Gradle 멀티모듈, MySQL/Flyway, Kafka, Spring Boot 공통 설정, 공통 도메인 모듈을 wave 1 선행 티켓으로 배치 (코드 0줄 상태이므로 필수)
2. **Rich Domain Model 메서드 시그니처** — §4.1의 후보 메서드를 TDD Entity 절에 명시
3. **DomainEventPublisher interface 명세** — Kafka 이벤트 발행 패턴을 공통 도메인 모듈에 정의
4. **권한 매트릭스 → API 매핑표** — 9.2 API 각각의 호출 권한·자동 필터링 동작을 TDD에 명시
5. **i18n/timezone API 응답·이벤트 페이로드 표준** — ISO-8601 형식, UTC 저장 + employmentTimezone 변환 적용 위치 결정

### C. TPM 분석 진행 시 주의

- 위 A 항목 중 1·5·6은 비즈니스 의미 결정이 필요해 **PRD 본문 변경이 선행되어야** TPM 분석 결과가 안정적으로 산출됩니다.
- A 항목 2·3·4·7은 TPM 분석 단계에서 합리적 디폴트로 진행 가능하나, PM/PO 승인 결과를 반영하는 후속 보정 단계가 필요합니다.
- B 항목은 TPM이 자율적으로 결정·문서화 가능합니다.

---

**결론**: 본 PRD는 employee 도메인 골격은 충분하나, Employment 상태 머신·일괄 API·권한 범위 정의·이벤트 매핑 등 **핵심 누락 항목 4종**으로 인해 TPM 분석을 곧바로 진행하면 합리적 추정에 의존한 가설성 결정이 다수 발생합니다. PM/PO 보강 미니 라운드(섹션 A의 1·5·6 우선) 후 분석 재개를 권고합니다.
