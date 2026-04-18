---
name: project-analysis-flow
description: PRD 분석 산출물을 받아 Background→Terminology→Problem→Solutions→Design→Security→TDD→Tickets 8단계를 순서대로 수행하는 절차. Mermaid 다이어그램(Component/Sequence/ERD) 3종과 TDD 케이스 목록, 1일 단위 티켓 분해를 강제한다. project-analyst 에이전트가 참조한다.
---

# Project Analysis Flow Skill

## 언제 사용하나
- PRD 분석이 끝난 뒤 실제 구현 설계 단계
- "티켓 쪼개줘", "설계해줘", "TDD 계획" 요청

## 원칙
- Possible Solutions는 **최소 2개 비교** + 선택 근거
- 설계 산출물에는 **Mermaid 3종 필수**: Component(AS-IS/TO-BE), Sequence, ERD
- TDD 섹션은 **FR 개수 ≥ TC 개수** — 모든 요구사항이 최소 1개 테스트로 증명 가능해야 함
- 티켓 = 1명 × 1일 × 1PR

## 8단계 절차

### 1. Background
- 비즈니스 맥락 (왜 지금)
- 관련 OKR/KPI
- 의존/연관 시스템

### 2. Terminology
- 한/영 병기 용어집
- BC 경계 그림 (Mermaid subgraph)
- 약어 금지 (`workspaceId` ✓, `ws` ✗)

### 3. Define Problem
- 사용자 시나리오 기반 문제 정의
- 현재 시스템 한계
- 성공 지표 (측정 가능)

### 4. Possible Solutions (≥2)
| 대안 | 개요 | 장점 | 단점 | 비용 | 리스크 |
|---|---|---|---|---|---|
| A | ... | ... | ... | L/M/H | ... |
| B | ... | ... | ... | L/M/H | ... |

**선택: B** — <근거>

### 5. Detail Design
필수 요소:
1. **Component Diagram AS-IS** (Mermaid `graph TD`)
2. **Component Diagram TO-BE** (변경 컴포넌트 강조)
3. **Sequence Diagram** (Mermaid `sequenceDiagram`) — 핵심 유스케이스 2-3개
4. **ERD** (Mermaid `erDiagram`) — 새 테이블/컬럼/관계(DB FK 금지)
5. **API 스펙** (표 또는 OpenAPI snippet)
6. **Kafka 이벤트 계약** (필요 시) — `event.<project>.<domain>` 형식

### 6. Security Information
- 인증/인가 (필요 권한)
- PII 처리 / 암호화 / 마스킹
- Rate Limiting / Abuse 방지
- Audit 로깅 포인트

### 7. TDD 전략
- TC 목록 (번호 부여): `TC-01 (FR-01): <title>`
- 각 TC: Type(단위/통합/E2E), Given/When/Then
- Testcontainers 요구 (MySQL/Redis/Kafka)
- 커버리지 목표 (일반 80%, 핵심 플로우 95%)

### 8. Ticket Breakdown
→ `ticket-breakdown` 스킬로 위임

## 산출물 구조
`.analysis/project-analysis/YYYY-MM-DD-<feature>/`
```
00-overview.md   # Background + Terminology + Problem + Solutions
01-design.md     # Detail Design + Security
02-tdd.md        # TDD 전략 + TC 목록
03-tickets.md    # 티켓 분해 (ticket-breakdown 산출)
```

## 완료 체크
- [ ] 4개 파일 모두 작성, 빈 섹션 없음
- [ ] Possible Solutions ≥ 2 + 선택 근거
- [ ] Mermaid 다이어그램 3종 렌더 가능
- [ ] 모든 FR이 ≥1 TC에 연결
- [ ] 모든 티켓이 ≤1일 + 대응 TC 링크
- [ ] harness-rules.json 위반 설계 요소 사전 검토 완료