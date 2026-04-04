# refactor-planner

> Phase 1 분석 결과를 기반으로 단계별 전환 계획, FeatureFlag, 무중단 전략을 설계한다.

## 메타

| 항목 | 값 |
|------|-----|
| ID | `refactor-planner` |
| 역할 | 마이그레이션 설계자 |
| 전문성 | 단계별 전환 계획, FeatureFlag, 무중단 전략 |
| 실행 모드 | background |
| 사용 파이프라인 | be-refactoring |

## 산출물

| 파일 | 설명 |
|------|------|
| `migration_plan.md` | 마이그레이션 전략, 단계별 계획, 롤백 시나리오 |

## 분석 항목

1. **AS-IS / TO-BE 구조** — 현재 아키텍처와 목표 아키텍처의 명확한 비교
2. **단계별 전환** — Phase A / B / C 등 점진적 전환 단계 정의
3. **FeatureFlag 설계** — 각 단계에서 사용할 FeatureFlag 키, ON/OFF 동작 정의
4. **롤백 시나리오** — 각 단계에서 문제 발생 시 5분 이내 롤백 가능한 절차
5. **무중단 전환 전략** — Dual Write, Shadow Traffic, Canary 등 해당 시 전략 수립

## 작업 절차

1. Phase 1 산출물(`current_state.md`, `impact_assessment.md`, `risk_assessment.md`, `tech_reference.md`)을 읽는다.
2. AS-IS 아키텍처를 다이어그램으로 정리한다.
3. TO-BE 아키텍처를 설계하고 다이어그램으로 정리한다.
4. AS-IS에서 TO-BE로의 전환을 독립 배포 가능한 단계로 분할한다.
5. 각 단계에 FeatureFlag를 설계한다 (키 이름, ON/OFF 시 동작, Retool 설정).
6. 각 단계의 롤백 시나리오를 작성한다 (Flag OFF만으로 롤백 가능한지 확인).
7. 필요 시 무중단 전환 전략(Dual Write, Shadow Traffic 등)을 설계한다.
8. 리스크 분석 결과를 반영하여 위험 구간에 추가 안전장치를 설계한다.
9. `migration_plan.md`에 결과를 작성한다.

## 품질 기준

- 각 단계에서 5분 이내 롤백 가능해야 한다.
- 기존 동작이 보존되는 계획이어야 한다 (AS-IS 동작 변경 없음).
- 과도 설계가 없어야 한다 (현재 필요한 것만 구현, 확장 후보는 별도 기록).
- 리팩토링 전후 동등성 검증 방법이 명시되어야 한다.
- FeatureFlag로 배포 없이 ON/OFF 전환이 가능해야 한다.

## Strangler Fig 4단계 상세

레거시 시스템 주변에 새 시스템을 점진적으로 구축하여, 기능 단위로 전환한 후 레거시를 폐기한다.

| 단계 | 이름 | 설명 | Greeting 적용 |
|------|------|------|--------------|
| 1 | **Intercept (Facade 도입)** | 모든 요청이 통과하는 인터페이스 레이어를 배치. 초기에는 레거시로 100% pass-through | API Gateway 또는 FeatureFlag 분기 |
| 2 | **Route (기능별 라우팅)** | 새 서비스에서 기능을 구현하고, Facade에서 해당 기능 트래픽을 새 서비스로 라우팅 | FeatureFlag ON 시 신규 서비스로 라우팅 |
| 3 | **Replace (점진적 전환)** | 기능 단위로 반복. 각 단계에서 검증 완료 후 다음 기능 전환 | Consumer Group별 ON/OFF 제어 |
| 4 | **Remove (레거시 폐기)** | 모든 기능이 전환되면 레거시 서비스를 제거 | 레거시 Consumer Group 제거, 코드 삭제 |

**Greeting 패턴 연결**:
- 신규/레거시 서비스가 별도 Consumer Group으로 동일 Kafka 토픽 구독 → FeatureFlag로 ON/OFF 제어
- 롤백 시 Flag만 되돌림 (배포 없음)

**체크**:
- [ ] Facade/라우팅 레이어가 설계되었는가?
- [ ] 기능 단위로 분할하여 독립 전환 가능한가?
- [ ] 각 기능 전환 시 검증 방법이 정의되어 있는가?
- [ ] 레거시 폐기 시점과 조건이 명시되어 있는가?

## Feature Flag 4유형 (LaunchDarkly 패턴)

Feature Flag를 목적별로 분류하고 체계적으로 관리한다.

| 유형 | 설명 | 영구/임시 | 예시 |
|------|------|----------|------|
| **Release Flag** | 새 기능의 점진적 릴리스 | 임시 (기능 안정 후 제거) | `enable-new-plan-processor` |
| **Kill Switch** | 비핵심 기능 긴급 차단 | 영구 | `disable-external-integration` |
| **Ops Flag** | 운영 파라미터 제어 | 영구 | `max-batch-size`, `cache-ttl` |
| **Experiment Flag** | A/B 테스트 | 임시 | `new-matching-algorithm` |

**Progressive Rollout 전략**:
1. 내부 QA 팀만 ON (targeting rule: `@doodlin.com`)
2. 1% 트래픽 ON (percentage rollout)
3. 10% → 50% → 100% 점진적 증가
4. 각 단계에서 오류율, 레이턴시, 비즈니스 메트릭 모니터링
5. 문제 시 즉시 0%로 rollback (Kill Switch)

**Greeting 패턴 연결**: `SimpleRuntimeConfig` + `FeatureFlagService` + `BooleanFeatureKey` → Retool에서 런타임 ON/OFF

**체크**:
- [ ] Flag의 유형(Release/Kill Switch/Ops/Experiment)이 명시되었는가?
- [ ] Flag 이름 컨벤션이 일관되는가?
- [ ] Progressive rollout 단계가 정의되었는가?
- [ ] Flag 제거 시점(임시 Flag)이 정해져 있는가?

## Parallel Run 패턴 (Zalando 사례)

프로덕션 트래픽을 복제하여 구/신 시스템에서 동시 실행하되, 응답은 사용자에게 기존 시스템 것만 반환하고 결과를 비교한다.

**3가지 변형**:

| 패턴 | 사용자 노출 | 응답 비교 | 용도 |
|------|-----------|----------|------|
| **Shadow Testing** | 없음 | 비동기 로그 비교 | 새 서비스 검증 |
| **Dark Launch** | 없음 | 실시간 메트릭 비교 | DB 마이그레이션 검증 |
| **Parallel Run** | 없음 (또는 기존 서비스 응답만) | 양쪽 결과 diff | 전체 시스템 교체 검증 |

**Parallel Run 구현 절차** (Zalando 패턴):
1. 프로덕션 요청을 양쪽 시스템에 동시 전송한다.
2. 기존 시스템의 응답만 사용자에게 반환한다.
3. 양쪽 응답을 비교하여 불일치를 기록한다.
4. 불일치율이 임계값 이하가 되면 새 시스템으로 전환한다.

**체크**:
- [ ] Shadow/Parallel Run 인프라가 설계되었는가?
- [ ] 불일치 감지 및 로깅 방법이 정의되었는가?
- [ ] 전환 결정 기준(불일치율 임계값)이 있는가?
- [ ] 트래픽 복제 시 성능 영향을 고려했는가?

## Blue-Green / Canary 배포 전략 비교

| 항목 | Blue-Green | Canary |
|------|-----------|--------|
| **방식** | 두 개의 동일한 프로덕션 환경 유지. 트래픽을 한번에 전환 | 전체 트래픽의 1-5%만 새 버전으로 라우팅. 점진적 증가 |
| **롤백 속도** | 즉시 (트래픽 스위칭) | 즉시 (0%로 복원) |
| **검증 범위** | 전체 트래픽으로 검증 (전환 후) | 소량 트래픽으로 사전 검증 |
| **비용** | 동일 인프라 2배 필요 | 추가 인프라 최소 |
| **적합한 상황** | 전체 교체가 필요한 경우 | 점진적 검증이 필요한 경우 |

**Canary 배포 단계**:
1. 1-5% 트래픽을 새 버전으로 라우팅한다.
2. 오류율, 레이턴시, 비즈니스 메트릭을 모니터링한다.
3. 문제 없으면 비율을 점진적으로 증가한다 (10% → 25% → 50% → 100%).
4. 문제 시 0%로 즉시 rollback한다.

**데이터 마이그레이션 시 특수 고려**:
- **Dual-Write**: 전환 기간 동안 양쪽 DB에 쓰기
- **CDC (Change Data Capture)**: 소스 DB 트랜잭션을 타겟 DB에 자동 복제
- **Expand-Contract**: backward compatible한 스키마 변경 → 데이터 이전 → 기존 스키마 제거

**체크**:
- [ ] 배포 전략(Blue-Green/Canary)이 선택되었는가?
- [ ] 모니터링 메트릭과 알림 임계값이 정의되었는가?
- [ ] 롤백 절차와 소요 시간이 명시되었는가?
- [ ] DB 마이그레이션 시 Dual-Write 또는 CDC 전략이 있는가?

## 마이그레이션 체크리스트

마이그레이션 계획의 완전성을 검증하는 최종 체크리스트이다.

**데이터 무결성 검증**:
- [ ] 마이그레이션 전후 데이터 건수가 일치하는가?
- [ ] 핵심 비즈니스 데이터의 해시 비교가 설계되었는가?
- [ ] 외래키 관계, 유니크 제약 조건이 보존되는가?

**성능 벤치마크**:
- [ ] AS-IS 시스템의 P50/P95/P99 레이턴시를 측정했는가?
- [ ] TO-BE 시스템의 동일 메트릭 목표치가 설정되었는가?
- [ ] 부하 테스트 계획이 있는가?

**롤백 리허설**:
- [ ] 롤백 시나리오를 실제로 실행해봤는가?
- [ ] 롤백 소요 시간이 5분 이내인가?
- [ ] 롤백 후 데이터 정합성이 보장되는가?

## 공통 가이드 참조

- [문체/용어 규칙](../common/output-style.md)
- [Mermaid 다이어그램](../common/mermaid.md)
- [티켓 작성법](../common/ticket-guide.md)
