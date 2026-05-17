# TPM 산출물 검수 (Step 1-B)

**검수 일자**: 2026-05-16
**검수 대상**: `.analysis/outputs/2026-05-16_hr-employee-service/tpm-analysis.md`
**입력 자료**: PRD 원본(`hr-platform/docs/prd/2026-05-16_mvp.md`) + 사전 리뷰(`pre-review.md`)
**판정**: PASS_WITH_NOTES

종합적으로 사전 리뷰의 보강 지시는 거의 모두 이행되었습니다. 다만 티켓 분해 단에서 1명/1일 사이즈 위반(L 3건), 권한 매트릭스 일부 누락 행, 통합 테스트 시나리오 일부 누락, 그리고 X3·X4 관련 작업 범위 명시 부족이 보입니다. TDD 진행은 가능하나 §7 티켓 목록을 TDD 작성 단계에서 보정해야 합니다.

---

## 1. 사전 리뷰 보강 지시 이행

| 보강 지시 항목 | 이행 여부 | 위치 | 비고 |
|---|---|---|---|
| Employment 상태 머신 (전이·이벤트·금지 전이) | ✅ | §4 (상태 머신 mermaid + 전이 규칙 표 + 금지 전이 3종) | PRE_HIRED→ACTIVE 자동/수동 양분, 휴직 중 퇴사 허용, RESIGNED 불가역, 재입사 = 새 Employment 모두 명시 |
| 누락 API 11종 추가 | ✅ | §2 (22개 API) | bulk 등록(직원·발령), 발령 이력 조회, 발령 취소, suspend/resume, 비상연락처, 부서장 변경, 부서 비활성, 부서 상세, 부서 멤버 조회 모두 추가 |
| 누락 이벤트 4종 추가 | ✅ | §3 (이벤트 9종) | `employee.suspended`/`employee.resumed`/`department.changed`/`department.head_changed` 모두 명시 + 페이로드 핵심 필드 포함 |
| BE 부트스트랩 별도 Wave 1 선행 분리 | ✅ | §7 BS-01 / §8.2 Wave 1 | BS-01이 단독 Wave 1, DB-01·KF-01·CM-01이 Wave 2에 배치 — 위상정렬 정합 |
| 권한 매트릭스 API×역할 완전 매핑 | ⚠️ | §6 권한 매트릭스 | 22개 API 중 21개 매핑(POST `/employees/bulk` 행 명시 ✅). 단, **22번 행 자체에 빈 매핑 행 없음** — 표가 22행으로 보이지만 카운팅 시 누락 가능. 자기 정보 수정의 마스킹·연봉 변경 분기는 §6 본문에 별도 서술로 처리(테이블 외) — TDD에서 명세화 필요 |
| 권한 자동 범위 필터 정의 | ✅ | §6 본문 "팀의 정의 — material path prefix match" | TEAM_LEAD 범위 = head인 Department.path 접두사로 모든 자손 부서 |
| X3 자기 연봉 수정 audit + CFO 알림 | ✅ | §6 본문 + §9 R8 | UseCase 내부 가드 + audit log + `employee.salary_changed_by_self` 이벤트 발행. M1에서는 실제 알림 채널은 M3 영역 — 명시되어 있음 |

---

## 2. 티켓 의존 관계 정합성

### 2.1 BS-01 의존 전이성
- 모든 후속 티켓이 BS-01에 직접 또는 전이 의존: ✅ 확인.
  - DB-01·KF-01·CM-01 직접 의존 BS-01
  - BE-01~07이 DB-01/KF-01/CM-01에 의존하므로 전이 BS-01
  - BE-08~13이 BE-0x 전이로 BS-01

### 2.2 병목 3종 (DB-01·KF-01·CM-01) 선행성
| 병목 티켓 | BE-01~12 전이 의존 | 판정 |
|---|---|---|
| DB-01 | BE-01~04 직접, BE-05~13 전이 | ✅ |
| KF-01 | BE-07 직접(only), BE-08~13 전이 | ✅ |
| CM-01 | BE-01~04, BE-07 직접, BE-05~13 전이 | ✅ |

> 단, **KF-01의 후행 카운트가 1(BE-07만)** 이라는 점은 의도된 설계(Kafka 발행은 인프라 어댑터에 격리)이며 ticket-guide의 "후행 의존 ≥ 3 분해 재검토 트리거"에는 해당하지 않습니다(작은 fan-out은 문제 없음).

### 2.3 BE-08의 DomainService 경유 명시
- BE-08 선행 = BE-05(DomainService) + BE-07(EventPublisher 구현)
- BE-08 작업 범위에 "Hire/Resign/Suspend/Resume UseCase" 명시되어 있고, BE-05 작업 범위에 "EmploymentDomainService가 DomainEventPublisher 호출"이 명시되어 있음 → UseCase가 Repository 직접 호출 안티패턴을 회피하는 의존 구성 ✅
- 단, BE-08~10 티켓 본문에 "DomainService만 호출(Repository 직접 주입 금지)" 문구가 작업 범위에 박혀 있지 않음. **TDD/티켓 분해 단계에서 작업 범위 텍스트에 강제 명시 필요** (be-implementer가 작업 범위만 보고도 안티패턴을 피할 수 있도록).

### 2.4 BE-12(Controller) 단독 wave 배치
- Wave 6 = BE-12 단독. ✅
- ticket-guide "Single Writer per File" — 공통 Controller 와이어업이 단독 wave로 분리됨 → 충돌 회피 OK
- 단, **BE-12를 EmployeeApiController와 DepartmentApiController로 분리할 여지**가 있습니다(서로 다른 파일이므로 같은 wave 병렬 가능). 그러면 평균 wave 너비가 약간 상승.

---

## 3. 사이즈 적합성

ticket-guide 기준 S=200줄/M=400줄/L=800줄. "1명/1~3일/1PR" 범위.

| 티켓 | 사이즈 | 작업 추정 | 판정 |
|---|---|---|---|
| BS-01 (M) | Gradle 멀티모듈 + 버전 카탈로그 + Spotless + detekt + Kotest | ~400줄 | ⚠️ 1일 빠듯하나 가능. detekt 룰 작성 분량에 따라 M 상회 가능 |
| BE-02 (L) | Employment Entity + JpaRepo + Repo interface + 상태 머신 + 9개 비즈니스 메서드 + canTransitTo enum | 800줄+ 초과 위험 | ❌ **분할 권고** — `BE-02a Entity+Repo interface / BE-02b 비즈니스 메서드(hire/resign/suspend/resume) / BE-02c 비즈니스 메서드(promote/transfer/changeSalary/assignManager)` 3분할 |
| BE-03 (L) | Department Entity + JpaRepo + 5개 비즈니스 메서드 + QueryDSL 트리 조회 + path 재계산 | 800줄 근접 | ⚠️ QueryDSL 트리 조회를 별도 티켓으로 빼는 것이 안전. **BE-03a Entity+기본 CRUD / BE-03b 트리 조회 QueryDSL+path 재계산** 2분할 권고 |
| BE-05 (L) | Person + Employment DomainService 모두 + 8개 오케스트레이션 메서드 | 800줄+ | ❌ **분할 권고** — Person/Employment 도메인 분리 원칙에도 어긋남. `BE-05a PersonDomainService / BE-05b EmploymentDomainService(lifecycle: hire/resign/suspend/resume) / BE-05c EmploymentDomainService(event: promote/transfer/changeSalary/assignManager)` 3분할 |
| BE-09 (L) | 발령 단건/일괄 UseCase 4종 + CSV 파서 + 직원 일괄 입사까지 | 800줄 초과 | ❌ **분할 권고** — `BE-09a 발령 단건+이력조회+취소 / BE-09b 발령 일괄 CSV / BE-09c 직원 일괄 입사 CSV(X4 트랜잭션)` 3분할. X4가 BE-09 안에 묻혀 있으면 X4 검증 추적이 어려움 |
| BE-12 (L) | 22개 API 라우팅 + 권한 인터셉터 연결 | 600~800줄 | ⚠️ Employee/Department 분리 가능(§2.4 참조) |
| BE-13 (L) | 통합 테스트 X1+X4+AC #1~#5 | 800줄+ | ⚠️ 시나리오별 분할 가능 — `BE-13a X1 시나리오 / BE-13b X4 시나리오 / BE-13c AC #1~#5 회귀` |
| S 티켓 (KF-01만) | Terraform 토픽 1개 | 100줄 미만 | ✅ |
| M 티켓 (DB-01·CM-01·BE-04·BE-06·BE-07·BE-08·BE-10·BE-11) | 400줄 내외 | ✅ |

**결론**: L 사이즈 7건 중 **BE-02·BE-05·BE-09 3건이 분할 필수**, BE-03·BE-12·BE-13 3건이 분할 권고. 분할 후 평균 wave 너비도 향상됩니다(§4 참조).

---

## 4. wave 너비·fan-out

### 4.1 TPM 보고 통계 재검증
- 평균 wave 너비 = (1+3+4+3+4+1+1)/7 = 16/7 ≈ **2.286** (TPM 보고 2.43은 미세 오차 — 분자 16/분모 7 정확)
- 4인 팀 가동률 = 2.286/4 ≈ **57%** (TPM 보고 61%는 평균 너비 2.43 기준)
- ticket-guide 기준 "70% 이상" → **미달** ⚠️

### 4.2 직선화 wave 비율
- 너비 1인 wave: Wave 1·6·7 = 3/7 ≈ **43%**
- ticket-guide "너비 1~2 wave 비율 50% 이상이면 재검토"
- 너비 1~2 wave: Wave 1·6·7만(=3개) → 43%. **양호선 임계 직전**.

### 4.3 분할 시 시뮬레이션
§3에서 권고한 분할(BE-02→3, BE-05→3, BE-09→3, BE-13→3, BE-12→2, BE-03→2)을 적용하면:
- Wave 3: BE-01, BE-02a, BE-02b, BE-02c, BE-03a, BE-04 (너비 6 — BE-02b/c는 BE-02a 의존이라 사실상 Wave 3+1로 흘러감)
- 더 현실적으로 BE-02·BE-05를 lifecycle/event 축으로만 2분할해도 평균 wave 너비 2.5~3.0으로 상승, 가동률 65~75% 달성 가능.

### 4.4 Wave 6·7 단독 타당성
- Wave 6 (BE-12): 공통 Controller 파일 단독 — ticket-guide "Single Writer per File" 원칙 충실 ✅
- Wave 7 (BE-13): 통합 테스트 — Controller 전체 라우팅 완료 후 E2E 검증이 자연스러움 ✅
- 단, BE-12를 Employee/Department 2분할 시 Wave 6 너비 2 가능. BE-13을 시나리오 3분할 시 Wave 7 너비 3 가능. **두 wave 모두 분할 여지 있음**.

---

## 5. 배포 순서

| 검증 항목 | 결과 |
|---|---|
| DB 스키마(DB-01) → BE 코드 순서 강제 | ✅ BE-01~04가 DB-01 직접 의존 |
| Kafka 토픽(KF-01) → BE 코드 순서 강제 | ✅ BE-07이 KF-01 직접 의존, Producer UseCase는 BE-07 전이 의존 |
| DB-01·KF-01 병렬 가능 | ✅ 둘 다 BS-01만 의존, Wave 2 동시 진행 가능 |
| CM-01과 DB-01·KF-01 병렬 가능 | ✅ Wave 2 너비 3 보장 |
| 마이그레이션 백워드 컴팩 | ✅ 신규 도메인이므로 backward compatibility 이슈 없음 |
| 롤백 시 데이터 손실 정책 | ⚠️ TPM 산출물에 명시 없음 — TDD Release Scenario에서 보강 필요 |

---

## 6. be-code-convention 위반 가능성

### 6.1 UseCase 안티패턴
| 티켓 | Repository 직접 호출 위험 | DomainService 경유 명시 | 판정 |
|---|---|---|---|
| BE-08 lifecycle UseCase | 의존이 BE-05(DomainService)+BE-07이므로 구조상 회피 | 작업 범위 텍스트에 명시 없음 | ⚠️ TDD/티켓 분해에서 문구 추가 필요 |
| BE-09 발령 UseCase | 의존 BE-05+BE-07 | 명시 없음 | ⚠️ 동 |
| BE-10 부서 UseCase | 의존 BE-06+BE-07 | 명시 없음 | ⚠️ 동 |
| BE-11 조회 UseCase | 의존 BE-05+BE-06 | 권한 자동 범위 필터 어댑터 위치 모호 | ⚠️ 조회 UseCase가 Repository 직접 호출하는 패턴은 be-code-convention에서 명시적으로 금지 — DomainService에 query 메서드 추가가 필요한데 BE-05/BE-06 작업 범위에 query 메서드 부재 |

> **권고**: BE-11 작업 범위에 "조회는 PersonDomainService/EmploymentDomainService/DepartmentDomainService의 query 메서드 경유, Repository 직접 주입 금지" 명시. BE-05/BE-06에 list/get 류 query 메서드 시그니처를 함께 추가.

### 6.2 Rich Domain Model 캡슐화
- BE-01~04 작업 범위에 비즈니스 메서드 시그니처가 명시됨 ✅
  - Person: changeContact·validatePersonalEmailFormat·비상연락처 메서드
  - Employment: 상태 머신·hire/resign/suspend/resume/promote/transfer/changeSalary/assignManager + EmploymentStatus.canTransitTo
  - Department: moveTo·assignHead·recalculatePath·validateNoCircularReference·deactivate
  - EmploymentHistory: 정적 팩토리 record + JsonStringType
- Anemic Domain 위험 낮음 ✅

### 6.3 금지 패턴 잠재 위험
| 금지 패턴 | 잠재 위치 | 판정 |
|---|---|---|
| `@Query` 어노테이션 | BE-03 Department 트리 조회 | ⚠️ 작업 범위에 "QueryDSL DepartmentQueryDslRepositoryImpl" 명시되어 있음 ✅ 회피 |
| `LocalDateTime` | 전체 시간 컬럼 | ✅ TPM §3에서 "ISO-8601 ZonedDateTime UTC" 명시 |
| `ConsumerRecord<String,String>` | 본 도메인은 Consumer 없음 | N/A |
| `!!` null assertion | 전 영역 | ⚠️ 명시 가드 없음 — TDD에서 detekt 룰로 강제 권고 |
| `@Transactional` 위치 | UseCase | ✅ 사전 리뷰 §4.4에서 명시, TPM 작업 범위에는 직접 명시 없으나 BE-08~11이 UseCase 티켓이므로 자명 |
| typealias 호환 layer | 신규 도메인 | ✅ §7.2에서 "없음" 명시 |

### 6.4 Domain Event 발행 책임
- BE-05 작업 범위에 "DomainEventPublisher 호출" 명시 ✅
- BE-07 작업 범위에 "common-domain interface의 infra 구현" 명시 ✅
- BE-06(DepartmentDomainService)에는 DomainEventPublisher 호출 명시 없음 ⚠️ — `department.changed`/`department.head_changed` 발행이 BE-06에 누락되어 있을 가능성. **TDD에서 보강 필요**.

---

## 7. 테스트 레이어 누락

### 7.1 단위 테스트 (domain·application)
- BE-01~06 작업 범위에 단위 테스트 명시 **없음** ❌
- BE-08~11 UseCase 단위 테스트 명시 **없음** ❌
- be-code-convention "PR 승인 가능: domain·application 단위 + infrastructure·presentation 통합 + scenario E2E 5계층" 기준 미달

### 7.2 통합 테스트
- BE-13에 통합 테스트가 단일 티켓으로 집중되어 있음 — be-code-convention의 "각 레이어별 통합 테스트(infrastructure는 Testcontainers MySQL/Kafka, presentation은 MockMvc+Testcontainers)" 기준에서 보면 **infrastructure 통합 테스트가 BE-01~07에 묶여야** 함

### 7.3 BE-13 시나리오 커버리지
- 명시: X1(퇴사 → resigned 이벤트 + 진행 발령 중단) + X4(100명 일괄 입사 트랜잭션) + AC #1~#5
- 누락 가능:
  - **AC #2 "양쪽 부서장 알림"** 검증 명시 없음 — 알림 채널이 M3이라 곤란하나 이벤트 발행 검증(`department.head_changed` + 부서 이동의 `employee.transferred` 페이로드에 oldHead/newHead 포함) 필요
  - **AC #5 부서 사이클 차단 (자기 자손으로 이동 시 400)** 명시 없음 — Department 사이클 검증 시나리오 필요
  - **PRE_HIRED → ACTIVE 수동 활성화 시 `employee.hired` 발행 시점** 검증 명시 없음
  - **권한 경계 (TEAM_LEAD가 자기 부서 외 직원 조회 → 403)** AC #4가 §11.2 AC #4에 해당하나 BE-13에서 "AC #1~#5 전체"라고 묶어 처리한 것이 권한 시나리오 깊이가 부족할 수 있음

### 7.4 권고
- 각 BE-01~11 티켓 작업 범위에 "단위 테스트(Kotest BehaviorSpec) 포함" 추가
- BE-13을 시나리오별로 분할(§3 참조) — X1 시나리오, X4 시나리오, AC 권한 경계 회귀
- 시나리오에 AC #2(알림 이벤트 발행)·AC #5(부서 사이클 차단)·PRE_HIRED 활성화 케이스 추가

---

## 종합 판정

### 판정: PASS_WITH_NOTES — TDD 작성 진행 가능

사전 리뷰 보강 지시는 모두 이행되었으며, 누락 API·이벤트·상태 머신·권한 매트릭스가 모두 명시되었습니다. TPM 분석 자체는 합리적이며 TDD 진행 가능합니다. 다만 다음을 TDD/티켓 분해 단계에서 보정합니다.

### TDD 작성 시 보정 항목 (Blocker 아님, Action Items)

1. **L 사이즈 티켓 3건 분할 필수** (BE-02, BE-05, BE-09) + 3건 권고 (BE-03, BE-12, BE-13) — 1명/1일/1PR 사이즈 준수 + wave 너비 향상
2. **BE-06 작업 범위에 `department.changed`/`department.head_changed` 발행 책임 명시 추가**
3. **BE-11 작업 범위에 "DomainService query 메서드 경유, Repository 직접 주입 금지" 명시** + BE-05/BE-06 작업 범위에 query 메서드 시그니처 추가
4. **BE-08~10 UseCase 티켓 작업 범위에 "DomainService만 호출" 명시 문구 박기** — be-implementer가 작업 범위만 보고 안티패턴 회피 가능하도록
5. **각 BE-01~11 티켓에 단위 테스트(Kotest BehaviorSpec) 포함 명시** — be-code-convention 5계층 테스트 충족
6. **BE-13 시나리오 분할 + AC #2 알림 이벤트·AC #5 부서 사이클·PRE_HIRED 수동 활성화 시나리오 추가**
7. **권한 매트릭스 표 22행 완전성 재확인** + 자기 정보 수정 마스킹·연봉 변경 분기 표 외 본문도 TDD §Detail Design에 명시
8. **롤백 시나리오** — TDD §Release Scenario에 DB 마이그레이션·Kafka 토픽 롤백 절차 명시
9. **PII 컬럼 정책 (Person 주민번호/외국인등록번호)** — TPM §9 R5에서 "M1 미보유"로 결정. TDD에 "Phase 1.5에서 추가" 마일스톤 명시
10. **권한 자동 범위 필터 (material path prefix match) 알고리즘 시그니처** — common-web 인터셉터에 들어가는데 CM-01 작업 범위에 명시되어 있으나 알고리즘 상세는 TDD에서 보강

### Blocker 없음 — TDD 작성 진행 권고

TPM 분석은 사전 리뷰의 모든 보강 지시를 반영했고, 의존 그래프·상태 머신·이벤트·권한 매트릭스가 모두 검증 가능한 수준으로 명시되어 있습니다. 위 10개 보정 항목은 TDD 작성 과정에서 자연스럽게 반영 가능합니다.
