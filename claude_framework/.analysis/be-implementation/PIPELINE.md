# BE Implementation Pipeline

BE 기능 구현 절차. PRD 분석 → 설계 → TDD → 티켓 → 구현 → 리뷰.

## 진입점

- 사용자: `/plan-project <PRD path>` 또는 main-orchestrator 호출

## 단계

### 1. PRD 정제 (선행 의존)
- `.analysis/prd/PIPELINE.md` 의 산출물 (`requirements.md`, `acceptance.md`) 사용
- 의문점이 남아있다면 PM/PO 질의 → 답변 후 진행

### 2. 도메인 식별
- 영향받는 Bounded Context 목록
- 신규 BC 인지, 기존 BC 확장인지
- 도메인 간 데이터 소유권 명확화 (be-tech-lead 검토)

### 3. 설계 (ADR)
- 컴포넌트 다이어그램 (mermaid)
- 시퀀스 다이어그램 (주요 플로우)
- ERD (테이블 변경)
- API 명세 (request/response)
- 외부 의존 (Kafka 토픽, 외부 API)
- ADR 작성: 결정 + 대안 + 트레이드오프

### 4. TDD 전략
- 테스트 레이어 매핑 (domain / application / infra / presentation / scenario)
- 우선 작성할 시나리오 (해피 패스 + 주요 엣지)
- Testcontainers 필요 여부

### 5. 티켓 분해
- `skills/ticket-breakdown/SKILL.md` 절차 적용
- 1티켓 = 1명 / 1일 / 1PR
- 의존 그래프 (선후 관계 명시)
- 각 티켓: AC, 대응 테스트, 추정치, 종속성

### 6. 구현 (티켓 단위 병렬 가능)
- be-implementer 가 티켓 1개씩
- TDD 사이클: Red → Green → Refactor
- 각 티켓 완료 시 PR open → `.analysis/pr-review/PIPELINE.md`

### 7. 통합·시나리오 테스트
- 모든 티켓 머지 후 E2E 시나리오 검증
- 비기능 요건(성능/부하) 측정

### 8. 회고
- harness-rules 추가 후보 (반복 발견된 안티패턴)
- 메타-피드백 트리거 (skill/command 보완)

## 산출물

- `.analysis/be-implementation/<feature>/`
  - `00-overview.md`
  - `01-design.md` (mermaid 포함)
  - `02-adr.md`
  - `03-tdd-strategy.md`
  - `04-tickets.md`
  - `05-retrospective.md`

## 안전 장치

- 도메인 경계 위반 (FQCN cross-domain) 사전 차단
- 마이그레이션은 별도 티켓으로 분리
- breaking 변경은 release pipeline 과 연동

## 참고

- PRD 단계: `.analysis/prd/PIPELINE.md`
- 프로젝트 분석: `.analysis/project-analysis/PIPELINE.md`
- 리뷰: `.analysis/pr-review/PIPELINE.md`
