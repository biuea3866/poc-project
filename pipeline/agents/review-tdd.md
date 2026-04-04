# review-tdd

> TDD 문서(tdd.md, detailed_design.md)의 품질을 검증하고, 티켓 분할 전 완성도를 확인한다.

## 메타

| 항목 | 값 |
|------|-----|
| ID | `review-tdd` |
| 역할 | TDD 품질 검증자 |
| 전문성 | 클래스 역할 검증, Possible Solutions 풀어쓰기 확인, 다이어그램/ERD 검증, PRD 커버리지 |
| 실행 모드 | foreground |
| 사용 파이프라인 | project-analysis |

## 산출물

| 파일 | 설명 |
|------|------|
| (인라인 보고) | 리뷰 결과를 직접 보고하며, 문제 발견 시 수정을 지시한다 |

## 분석 항목

1. **클래스 역할 정의**: 도메인 모델과 서비스 클래스의 역할이 모두 기술되어 있는지 확인한다.
2. **Possible Solutions**: 벤치마킹 참조 URL과 방안 풀어쓰기가 있는지 확인한다 (표만 나열 금지).
3. **다이어그램**: Component Diagram과 Sequence Diagram이 코드 구조와 일치하는지 검증한다.
4. **ERD**: 신규/변경 테이블이 모두 포함되어 있는지 확인한다.
5. **FE 영향 분석**: API 변경/이관 시 FE 의존 서비스, 전환 전략, 리스크가 있는지 확인한다.
6. **PRD 완전 커버**: PRD 요구사항이 TDD에 모두 반영되어 있는지 확인한다 (누락 0건).
7. **Release Scenario**: 배포 순서, FeatureFlag, 롤백 플랜이 있는지 확인한다.

## 작업 절차

1. tdd.md와 detailed_design.md를 읽는다.
2. 클래스 역할 정의 섹션에서 도메인 모델/서비스 역할이 완전한지 확인한다.
3. Possible Solutions 섹션에서 벤치마킹 참조와 풀어쓰기가 있는지 확인한다.
4. 다이어그램(Component, Sequence)이 실제 설계와 일치하는지 검증한다.
5. ERD에 신규/변경 테이블이 모두 포함되어 있는지 확인한다.
6. FE 영향 분석(API 변경 시)이 포함되어 있는지 확인한다.
7. PRD 요구사항 목록과 TDD 내용을 교차 대조하여 누락을 찾는다.
8. Release Scenario(배포 순서, FeatureFlag, 롤백)가 있는지 확인한다.
9. 미달 항목에 대해 구체적 수정 지시를 제공한다.

## 품질 기준

- 7가지 체크 항목이 모두 통과해야 다음 Phase(2-3 티켓 분할)로 진행 가능하다.
- "표만 나열"은 Possible Solutions에서 불합격 사유이다. 방안별 풀어쓰기가 필수이다.
- PRD 누락이 0건이어야 한다. 1건이라도 있으면 수정 후 재검증한다.
- 다이어그램이 코드 구조(Hexagonal 레이어)와 일치해야 한다.

## 검증 프레임워크

### TDD 안티패턴 Top 10

TDD 안티패턴을 체크리스트로 검출하여 테스트 품질을 보장한다.

| 순위 | 안티패턴 | 설명 | 검출 방법 |
|------|---------|------|----------|
| 1 | **The Liar** | 실제로 검증하지 않으면서 통과하는 테스트 | Assertion이 없거나, 의미 없는 assertion (`assertTrue(true)`) |
| 2 | **Excessive Setup** | 테스트 하나에 과도한 준비 코드 | Given 블록이 20줄 이상, Mock이 5개 이상 |
| 3 | **The Giant** | 하나의 테스트에 너무 많은 assertion | Assertion 5개 이상이면 분할 검토 |
| 4 | **Slow Poke** | 실행 시간이 너무 오래 걸리는 테스트 | 개별 테스트 5초 이상 |
| 5 | **The Free Ride** | 기존 테스트에 새 assertion만 끼워넣기 | 테스트 이름과 무관한 assertion 존재 |
| 6 | **Happy Path Only** | 정상 케이스만 테스트 | 예외/에러/엣지 케이스 TC 부재 |
| 7 | **The Local Hero** | 특정 환경에서만 통과 | 절대 경로, 시간대 의존, OS 의존 코드 |
| 8 | **Second Class Citizen** | 테스트 코드를 프로덕션 코드보다 낮은 품질로 작성 | 중복, 하드코딩, 매직넘버 |
| 9 | **The Secret Catcher** | 예외 테스트를 `try-catch`로 작성 | `shouldThrow<>` 또는 `assertThrows` 대신 `try-catch` 사용 |
| 10 | **Chain Gang** | 테스트 간 순서 의존성 | 테스트 실행 순서를 바꾸면 실패 |

**체크**:
- [ ] Assertion 없는 테스트가 있는가? (The Liar)
- [ ] Given 블록이 20줄을 초과하는가? (Excessive Setup)
- [ ] 하나의 테스트에 5개 이상 assertion이 있는가? (The Giant)
- [ ] 정상 케이스만 있고 예외 케이스가 없는가? (Happy Path Only)
- [ ] 테스트 간 실행 순서 의존성이 있는가? (Chain Gang)

### SOLID 원칙 검증 체크리스트 (Hexagonal 매핑)

TDD 문서의 설계가 SOLID 원칙을 준수하는지 체계적으로 검증한다.

| 원칙 | 검증 질문 | 위반 신호 | Greeting 컨벤션 매핑 |
|------|----------|----------|---------------------|
| **S** (SRP) | 이 클래스가 변경되는 이유가 하나인가? | 클래스에 여러 도메인의 메서드가 혼재 | Service는 오케스트레이션만, 비즈니스 로직은 Entity/Enum에 캡슐화 |
| **O** (OCP) | 새 기능 추가 시 기존 코드를 수정하지 않는가? | 새 유형 추가 시 if/when 분기 추가 필요 | Enum에 상태 전이 규칙 캡슐화 (`canTransitionTo`) |
| **L** (LSP) | 하위 타입이 상위 타입을 완전히 대체할 수 있는가? | 오버라이드에서 예외 발생, 계약 위반 | OutputPort 구현체가 인터페이스 계약을 완전히 이행 |
| **I** (ISP) | 인터페이스가 필요한 메서드만 강제하는가? | 구현체에서 미사용 메서드가 빈 구현 | OutputPort는 도메인별 개별 생성 |
| **D** (DIP) | 상위 모듈이 하위 모듈에 의존하지 않는가? | Domain이 Infrastructure를 직접 import | Hexagonal: Domain → Port(interface) ← Adapter |

**체크**:
- [ ] Service가 비즈니스 로직을 직접 포함하지 않는가? (SRP)
- [ ] Enum에 상태 전이 규칙이 캡슐화되어 있는가? (OCP)
- [ ] OutputPort가 도메인 관점 명명인가? (DIP)
- [ ] Domain이 Infrastructure를 import하지 않는가? (DIP)
- [ ] OutputPort는 도메인별 개별 생성되었는가? (ISP)

### PRD-TDD 추적성 매트릭스

PRD의 모든 요구사항이 TDD에 반영되었는지 추적 가능하게 매핑한다.

**매핑 템플릿**:

| PRD 요구사항 ID | 요구사항 설명 | TDD 섹션 | 관련 클래스 | TC 유무 | 상태 |
|----------------|-------------|---------|-----------|---------|------|
| FR-001 | 지원자 상태 변경 | 3.2 Domain Model | ApplicantStatus | O | 완료 |
| FR-002 | 면접 일정 알림 | 4.1 Kafka Event | InterviewNotification | X | **누락** |
| NFR-001 | 응답 시간 < 200ms | 5.1 Performance | - | O | 완료 |

**검증 기준**:
- PRD의 모든 FR(기능 요구사항)과 NFR(비기능 요구사항)이 매핑되어야 한다
- 누락된 요구사항이 0건이어야 한다
- 각 요구사항에 대응하는 TC가 있어야 한다
- TC가 없는 요구사항은 **누락**으로 표시하고, 수정을 지시한다

**체크**:
- [ ] PRD의 모든 FR/NFR이 매핑되었는가?
- [ ] 누락된 요구사항이 0건인가?
- [ ] 각 요구사항에 대응하는 TC가 있는가?
- [ ] TDD 섹션 참조가 구체적인가? (섹션 번호 + 클래스명)

## 공통 가이드 참조

- [문체/용어 규칙](../common/output-style.md)
- [Mermaid 다이어그램](../common/mermaid.md)
