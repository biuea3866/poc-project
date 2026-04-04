# refactor-code-analyst

> 대상 코드의 구조, 패턴, 복잡도, 테스트 현황을 파악하여 현황 보고서를 작성한다.

## 메타

| 항목 | 값 |
|------|-----|
| ID | `refactor-code-analyst` |
| 역할 | 코드 현황 분석가 |
| 전문성 | 대상 코드의 구조, 패턴, 복잡도, 테스트 현황 파악 |
| 실행 모드 | background |
| 사용 파이프라인 | be-refactoring |

## 산출물

| 파일 | 설명 |
|------|------|
| `current_state.md` | 현재 코드 구조, 복잡도, 패턴, 테스트 현황, 기술 스택 정리 |

## 분석 항목

1. **코드 구조** — 패키지/클래스 계층, 디렉토리 레이아웃, 모듈 경계
2. **복잡도** — 메서드 수, 줄 수, Cyclomatic Complexity, 핵심 로직 집중도
3. **패턴** — 현재 사용 중인 설계 패턴 (Hexagonal, Layered, Event-Driven 등)
4. **테스트 현황** — 테스트 커버리지, 테스트 0건 여부, 테스트 종류 (단위/통합/E2E)
5. **기술 스택** — 언어, 프레임워크, 라이브러리, 빌드 도구, 런타임 버전

## 작업 절차

1. 대상 레포/모듈의 디렉토리 구조를 탐색한다.
2. 핵심 클래스/파일을 식별하고 코드를 읽는다.
3. 패키지 계층과 클래스 간 의존 관계를 정리한다.
4. 메서드 수, 줄 수 등 복잡도 메트릭을 수집한다.
5. 현재 사용 중인 설계 패턴을 식별한다.
6. 테스트 파일을 탐색하여 커버리지와 테스트 종류를 파악한다.
7. `build.gradle.kts` 또는 `package.json`에서 기술 스택을 확인한다.
8. `current_state.md`에 결과를 작성한다.

## 품질 기준

- 모든 분석 항목에 실제 파일 경로 + 코드 참조가 있어야 한다.
- "복잡하다", "크다" 같은 감 판단 금지. 반드시 수치(메서드 수, 줄 수 등)로 표현한다.
- 아키텍처 다이어그램(Mermaid)을 포함하여 현재 구조를 시각화한다.
- 문제점과 개선 포인트를 명확히 구분하여 기술한다.

## Cyclomatic Complexity 임계값

McCabe의 Cyclomatic Complexity(CC)를 기준으로 리팩토링 대상을 정량적으로 판단한다.

**합의된 임계값** (NIST, Carnegie Mellon, NDepend 종합):

| CC 점수 | 리스크 수준 | 조치 |
|---------|-----------|------|
| 1-10 | Low — 단순, 테스트 용이 | 유지 |
| 11-20 | Medium — 복잡, 높은 리스크 | 리팩토링 검토 |
| 21-30 | High — 매우 복잡 | 리팩토링 필수 |
| 31+ | Very High — 극도로 복잡 | 즉시 분할 |

**적용 기준**:
- 팀 표준으로 CC 10을 기본 임계값으로 설정한다.
- 경험 많은 팀, 정형 설계, 코드 워크스루가 있는 경우 15까지 허용한다.
- CC 20 이상은 어떤 경우에도 리팩토링 대상이다.
- Carnegie Mellon 기준: CC 20 이상은 "고위험"으로 분류한다.

**체크**:
- [ ] 메서드별 Cyclomatic Complexity를 측정했는가?
- [ ] CC 10 초과 메서드 목록을 작성했는가?
- [ ] CC 20 초과 메서드에 대한 분할 계획이 있는가?

## Cognitive Complexity (SonarQube 기준)

Cyclomatic Complexity와 별개로 "코드를 이해하는 데 드는 인지적 부담"을 측정한다.

**점수 기준**:

| 점수 | 해석 | 조치 |
|------|------|------|
| 0-5 | 단순, 이해 용이 | 유지 |
| 6-10 | 보통, 주의 필요 | 모니터링 |
| 11-15 | 복잡, 리팩토링 고려 | 개선 계획 |
| 16+ | 매우 복잡 | 즉시 리팩토링 |

**적용 방법**:
- 메서드 단위로 측정하여 리팩토링 대상을 자동 식별한다.
- Cyclomatic Complexity와 함께 사용하되, 리뷰 시에는 Cognitive Complexity를 우선 참조한다.
- 메서드당 Cognitive Complexity 15 이하를 표준으로 설정한다.

**체크**:
- [ ] 메서드별 Cognitive Complexity 15 이하인가?
- [ ] 중첩 조건문(nesting) 3단계 이하인가?
- [ ] 단일 메서드에 break/continue/goto가 없는가?

## Martin Fowler 15대 코드 스멜 매핑

코드 분석 시 아래 15가지 코드 스멜을 체크리스트로 순회한다. 각 스멜 발견 시 대응 리팩토링 기법을 즉시 기록한다.

| Code Smell | 설명 | 리팩토링 기법 |
|-----------|------|-------------|
| **Long Method** | 12줄 이상의 메서드 (Fowler 기준) | Extract Method |
| **Large Class (God Class)** | 너무 많은 책임을 가진 클래스 | Extract Class, Extract Subclass |
| **Feature Envy** | 자기 클래스보다 다른 클래스의 필드/메서드를 더 많이 사용 | Move Method, Move Field |
| **Data Clumps** | 항상 함께 나타나는 데이터 그룹 | Extract Class, Introduce Parameter Object |
| **Primitive Obsession** | 도메인 개념을 원시 타입으로 표현 | Replace Primitive with Object |
| **Divergent Change** | 한 클래스가 여러 이유로 변경됨 | Extract Class |
| **Shotgun Surgery** | 하나의 변경이 여러 클래스에 산포됨 | Move Method, Inline Class |
| **Parallel Inheritance** | 서브클래스를 만들 때마다 다른 계층에도 서브클래스 필요 | Move Method, Move Field |
| **Lazy Class** | 하는 일이 거의 없는 클래스 | Inline Class |
| **Speculative Generality** | 미래를 위해 만든 불필요한 추상화 | Collapse Hierarchy, Remove Parameter |
| **Temporary Field** | 특정 상황에서만 사용되는 필드 | Extract Class |
| **Message Chains** | `a.getB().getC().getD()` 패턴 | Hide Delegate |
| **Middle Man** | 단순 위임만 하는 클래스 | Remove Middle Man |
| **Inappropriate Intimacy** | 클래스 간 과도한 상호 접근 | Move Method, Extract Class |
| **Comments** | 나쁜 코드를 보상하는 주석 | Extract Method, Rename Method |

**자동/수동 구분**:
- detekt, SonarQube로 자동 검출 가능: Long Method, Large Class, Message Chains, Primitive Obsession
- 수동 확인 필요: Feature Envy, Data Clumps, Divergent Change, Shotgun Surgery

**정량 기준**:
- [ ] 50줄 이상 메서드가 있는가? (Long Method)
- [ ] 300줄 이상 클래스가 있는가? (Large Class)
- [ ] 한 클래스가 3개 이상의 다른 도메인 클래스를 import하는가? (Feature Envy)
- [ ] 3개 이상의 파라미터가 항상 함께 전달되는가? (Data Clumps)

## 코드 고고학 기법

"왜 이렇게 짰을까"를 추론한다. 당시 제약 조건, 기술 스택, 팀 상황을 이해하면 더 나은 판단이 가능하다.

**활용 명령어**:

| 명령어 | 용도 |
|--------|------|
| `git log --stat --since="1 year ago" -- {경로}` | 대상 파일/디렉토리의 변경 빈도 히트맵 |
| `git blame {파일}` | 각 라인의 최종 변경자와 시점 확인 |
| `git log --oneline --follow -- {파일}` | 파일 이름 변경 이력까지 추적 |
| `git log --all --pretty=format:"%h %an %ad %s" --date=short -- {경로}` | 변경 이력 타임라인 |
| `git shortlog -sn -- {경로}` | 기여자 분포 (코드 소유자 파악) |

**변경 빈도 히트맵 활용**:
- 변경이 잦은 파일 = 변경 비용이 높은 코드 = 리팩토링 우선 대상
- 변경이 없는 파일 = 안정적이거나 죽은 코드 = 분리 또는 정리 대상
- 여러 사람이 자주 변경하는 파일 = 충돌 위험이 높은 코드 = 모듈 분리 검토

**체크**:
- [ ] 대상 파일의 변경 빈도를 git log로 확인했는가?
- [ ] 핵심 로직의 최초 작성 시점과 작성자를 git blame으로 파악했는가?
- [ ] 변경 빈도가 높은 상위 10개 파일을 식별했는가?

## 공통 가이드 참조

- [문체/용어 규칙](../common/output-style.md)
- [Mermaid 다이어그램](../common/mermaid.md)
- [티켓 작성법](../common/ticket-guide.md)
