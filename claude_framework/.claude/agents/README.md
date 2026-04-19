# .claude/agents — 역할별 에이전트

Claude Code의 Agent 툴에서 `subagent_type: <name>`으로 호출 가능한 서브에이전트 카탈로그.

## 에이전트 카탈로그

### 오케스트레이터

| 이름 | 역할 | 모델 |
|---|---|---|
| `pipeline-runner` | 파이프라인 오케스트레이터 (다른 에이전트 호출) | opus |

### 분석/설계

| 이름 | 역할 | 모델 |
|---|---|---|
| `prd-analyst` | PRD 분석, FR/NFR/AC/Open Questions 추출 | opus |
| `project-analyst` | 설계/TDD/티켓 8단계 수행 | opus |
| `ticket-splitter` | "1일/1PR" 티켓 분해 | sonnet |

### 페르소나 (병행 스폰 가능, 관점별 리뷰)

| 이름 | 역할 | 모델 | 언제 |
|---|---|---|---|
| `be-tech-lead` | 아키텍처 일관성, 서비스 간 영향, 기술 전략 | opus | 설계/마이그레이션 계획 리뷰 |
| `be-senior` | 프로덕션 안전성, 엣지 케이스, 운영 리스크 | opus | Phase 3 리뷰 게이트, PR 최종 |
| `fe-lead` | FE 아키텍처, 컴포넌트 재사용, 상태 관리 | opus | API 변경/이관 시 FE 영향 분석 |

### 구현

| 이름 | 역할 | 모델 |
|---|---|---|
| `be-implementer` | Kotlin/Spring Boot TDD 구현 (IC) | sonnet |
| `fe-implementer` | React/Next.js TDD 구현 (IC) | sonnet |

### 리뷰/감사

| 이름 | 역할 | 모델 |
|---|---|---|
| `pr-reviewer` | 하네스 룰 + 아키텍처 기반 PR 리뷰 | sonnet |
| `harness-auditor` | 룰 위반 전수 감사 | sonnet |

## 페르소나 병행 스폰 패턴

중요한 설계/리뷰 단계에서는 **관점별 페르소나를 동시 스폰**해 다면 의견 수렴:

```
# 설계 리뷰 단계
Agent(subagent_type="be-tech-lead", ...)      # 아키텍처 관점
Agent(subagent_type="be-senior", ...)          # 운영 관점
Agent(subagent_type="fe-lead", ...)            # FE 영향 관점
# → 3개 관점 취합해 최종 판단
```

## 공통 가이드 참조

모든 에이전트는 프롬프트 상단에 사용할 `.claude/common/*.md` 문서를 명시:

- [output-style](../common/output-style.md) — 문체/코드 참조
- [mermaid](../common/mermaid.md) — 다이어그램 규칙
- [ticket-guide](../common/ticket-guide.md) — 티켓 구조
- [jira-sync](../common/jira-sync.md) — Jira 매핑
- [tdd-template](../common/tdd-template.md) — TDD 섹션
- [document-sync](../common/document-sync.md) — 동기화 순서
- [be-code-convention](../common/be-code-convention.md) — BE 실전 컨벤션

## 파일 구조

```markdown
---
name: <id>
description: 언제 자동 위임되어야 하는지
tools: Read, Grep, Bash, ...
model: opus | sonnet | haiku
---

<프롬프트 본문 — 사용 스킬/공통 가이드 명시>
```