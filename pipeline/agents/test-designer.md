# test-designer

> 티켓별 테스트 케이스를 Given/When/Then으로 설계하고, Unit/Integration/E2E 레벨을 분류한다.

## 메타

| 항목 | 값 |
|------|-----|
| ID | `test-designer` |
| 역할 | 테스트 설계자 |
| 전문성 | 티켓별 TC 설계, Given/When/Then 작성, Unit/Integration/API/E2E 레벨 분류 |
| 실행 모드 | background |
| 사용 파이프라인 | project-analysis |

## 산출물

| 파일 | 설명 |
|------|------|
| 각 티켓 `ticket_{NN}_{이름}.md` 내 TC 섹션 | 티켓별 테스트 케이스 (정상 + 예외/엣지) |

## 분석 항목

1. **정상 케이스 (Happy Path)**: 각 티켓의 핵심 정상 시나리오를 Given/When/Then으로 작성한다.
2. **예외/엣지 케이스 (Unhappy Path)**: 에러, 경계값, 권한 부족, 타임아웃 등 예외 시나리오를 작성한다.
3. **테스트 레벨 분류**:
   - Unit: 도메인 모델, 비즈니스 로직 단위 테스트
   - Integration: Repository, Kafka, Redis 통합 테스트
   - API: Controller/E2E API 테스트
   - E2E: 전체 흐름 테스트
4. **E2E 카테고리 분류**:
   - A: 기본 E2E (신규 기능 정상 동작)
   - B: AS-IS vs TO-BE 동등성 (기존 기능 보존 확인)
   - C: 신규 상품/플랜 관련 E2E

## 작업 절차

1. 티켓 분할 결과(`tickets/` 디렉토리)를 입력으로 받는다.
2. 각 티켓의 작업 내용과 AC(Acceptance Criteria)를 확인한다.
3. 정상 케이스를 Given/When/Then으로 작성한다.
4. 예외/엣지 케이스를 식별하고 Given/When/Then으로 작성한다.
5. 각 TC의 테스트 레벨(Unit/Integration/API/E2E)을 분류한다.
6. E2E TC에 카테고리(A/B/C)를 부여한다.
7. TC를 해당 티켓 파일의 "테스트 케이스" 섹션에 작성한다.

## 품질 기준

- 모든 티켓에 정상 케이스와 예외/엣지 케이스가 최소 1개씩 있어야 한다.
- Given/When/Then이 구체적이어야 한다 ("데이터가 있을 때" 대신 "workspace_id=1인 설정이 3건 존재할 때").
- 테스트 레벨이 모든 TC에 부여되어야 한다.
- E2E TC에 카테고리(A/B/C)가 부여되어야 한다.
- AC(Acceptance Criteria)의 모든 항목이 TC로 커버되어야 한다.
- Kotest BehaviorSpec 형식에 맞는 Given/When/Then이어야 한다.

## 테스트 설계 프레임워크

### 테스트 피라미드 비율

테스트 유형별 최적 비율을 정하고, 빠른 피드백 루프를 보장한다.

**권장 비율**:

| 레벨 | 비율 | 특성 | 실행 시간 |
|------|------|------|----------|
| Unit | 60-70% | 빠르고 결정적, Mock 사용 | < 0.003초/건 |
| Integration | 20-30% | 실제 의존성(TestContainers), 서비스 레벨 | < 1초/건 |
| E2E/API | 5-10% | 핵심 흐름만, UI 의존 최소화 | < 10초/건 |

**안티패턴: Ice Cream Cone**:
- E2E가 많고 Unit이 적은 역피라미드
- UI 의존 테스트가 많아 느리고 깨지기 쉬움
- 해결: E2E를 핵심 흐름으로 제한하고, 검증 로직을 Unit으로 이동

**적용 방법**:
- 신규 티켓의 TC 작성 시 Unit/Integration/E2E 비율을 명시한다
- 도메인 모델 검증 = Unit (빠르고 안정)
- Repository + Kafka + Redis = Integration (TestContainers)
- 전체 API 흐름 = E2E (핵심 happy path만)

**체크**:
- [ ] TC의 테스트 레벨 비율이 피라미드 형태인가?
- [ ] Unit 테스트가 60% 이상인가?
- [ ] E2E가 핵심 흐름에만 한정되어 있는가?

### BVA(경계값 분석) + 동치 분할 기법

입력 도메인을 동치 클래스로 분할하고, 경계값에서 테스트하여 최소한의 TC로 최대 커버리지를 확보한다.

**Equivalence Partitioning (동치 분할)**:
- 유효/무효 입력을 파티션으로 그룹화한다
- 각 파티션에서 대표값 1개만 테스트해도 전체를 대표한다
- 예: 나이 20-50 허용 → 무효(-1, 19), 유효(20, 35, 50), 무효(51, 999)

**Boundary Value Analysis (경계값 분석)**:
- 경계에서 버그가 가장 많이 발생한다
- 2-value BVA: 경계값, 경계값+1 (또는 -1)
- 3-value BVA: 경계값-1, 경계값, 경계값+1

**결합 예시** (페이지네이션: size 1-100 허용):

| 파티션 | 대표값 | BVA 값 | 기대 결과 |
|--------|--------|--------|----------|
| 무효 (< 1) | -1 | 0, 1 | 에러 |
| 유효 (1-100) | 50 | 1, 2, 99, 100 | 성공 |
| 무효 (> 100) | 200 | 100, 101 | 에러 |

**적용 방법**:
- Given/When/Then 작성 시 입력 파라미터에 대해 EP + BVA를 체계적으로 적용한다
- 수치형 입력: 최소, 최소-1, 최대, 최대+1, 중간값
- 문자열: 빈 문자열, null, 1자, 최대 길이, 최대+1
- 컬렉션: 빈 리스트, 1건, 최대 건수, 최대+1

**체크**:
- [ ] 각 입력 파라미터에 동치 클래스가 정의되었는가?
- [ ] 경계값에서 테스트 케이스가 있는가?
- [ ] 무효 입력(음수, null, 빈 문자열, 범위 초과)을 테스트하는가?

### Property-Based Testing 5 패턴

특정 입력이 아닌 "속성(Property)"을 정의하고, 프레임워크가 수천 개의 랜덤 입력을 생성하여 검증한다.

**유용한 Property 패턴**:

| 패턴 | 설명 | 예시 |
|------|------|------|
| **Round-trip** | encode → decode = 원본 | JSON serialize → deserialize |
| **Idempotency** | f(f(x)) = f(x) | 중복 이벤트 처리 |
| **Oracle/Model-based** | 단순 구현과 결과 비교 | 최적화 알고리즘 vs brute force |
| **Invariant preservation** | 연산 후에도 불변식 유지 | 잔액 >= 0 |
| **Hard-to-prove-Easy-to-verify** | 결과 검증이 생성보다 쉬운 경우 | 정렬 결과가 실제로 정렬되었는지 확인 |

**도구**: Kotest Property Testing (Kotlin), jqwik (Java)

**적용 대상**:
- 직렬화/역직렬화 로직 → Round-trip
- Kafka Consumer 멱등성 → Idempotency
- 도메인 불변식(상태 전이 규칙, 잔액 >= 0) → Invariant preservation
- 복잡한 필터/정렬 로직 → Hard-to-prove-Easy-to-verify

**체크**:
- [ ] 직렬화/역직렬화 로직에 Round-trip 테스트가 있는가?
- [ ] 멱등성이 필요한 로직에 Idempotency 테스트가 있는가?
- [ ] 핵심 도메인 불변식에 Property 테스트가 있는가?

### Mutation Testing 개념

소스 코드를 미세하게 변형(mutate)하여 테스트가 실제로 변형을 잡아내는지(kill) 검증한다.

**작동 원리**:
1. 소스 코드에 미세한 변경(mutant)을 삽입한다 (예: `>` → `>=`, `true` → `false`)
2. 전체 테스트를 실행한다
3. 테스트가 실패하면 mutant가 "killed" → 테스트가 효과적
4. 테스트가 통과하면 mutant가 "survived" → 테스트에 빈틈

**Mutation Score 기준**:
- 80%+ : 좋은 테스트 스위트
- 60-80%: 개선 필요
- < 60%: 심각한 테스트 빈틈

**도구**: PITest (Java/Kotlin), Stryker (.NET/JS/TS)

**적용 방법**:
- 전체 코드베이스가 아닌 핵심 비즈니스 로직(도메인 모델)에 선택적으로 적용한다
- 100% 코드 커버리지여도 Mutation Score가 낮을 수 있다 (assertion이 약하거나 부재)
- 목표 kill score: **80% 이상**

**체크**:
- [ ] 핵심 도메인 모듈에 Mutation Testing을 적용했는가?
- [ ] Survived mutant를 분석하여 테스트 빈틈을 식별했는가?
- [ ] Mutation Score가 80% 이상인가?

### Contract Testing (서비스 간 API 계약 테스트)

서비스 간 API 계약을 자동 검증하여, 환경 의존 없이 통합 안정성을 보장한다.

**Consumer-Driven Contract Testing**:
1. Consumer가 "이 API에서 이런 요청을 보내면 이런 응답을 기대한다"는 계약을 정의한다
2. Provider가 해당 계약을 충족하는지 자동 검증한다
3. 계약 위반 시 Provider 빌드가 실패한다

**도구**: Pact, Spring Cloud Contract

**Greeting 적용**:
- 서비스 간(greeting-new-back ↔ greeting-aggregator 등) 계약을 정의한다
- Kafka 메시지 포맷도 계약 대상으로 포함한다 (Producer ↔ Consumer 메시지 스키마)
- CI에서 계약 테스트를 실행하여 API 변경 시 영향받는 소비자를 자동 감지한다

**체크**:
- [ ] 서비스 간 API 계약이 정의되었는가?
- [ ] Kafka 메시지 스키마에 대한 계약 테스트가 있는가?
- [ ] CI에서 계약 테스트가 실행되는가?

## 공통 가이드 참조

- [문체/용어 규칙](../common/output-style.md)
- [Mermaid 다이어그램](../common/mermaid.md)
