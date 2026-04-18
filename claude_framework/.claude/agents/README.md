# .claude/agents — 역할별 에이전트

프로젝트 특화 서브에이전트. 각 파일은 Markdown + frontmatter 형식이며 Claude Code의 Agent 툴에서 `subagent_type: <name>`으로 호출 가능.

## 에이전트 카탈로그

| 이름 | 역할 | 주 사용 파이프라인 | 모델 |
|---|---|---|---|
| `pipeline-runner` | 파이프라인 오케스트레이터 (다른 에이전트 호출) | 모두 | opus |
| `prd-analyst` | PRD 분석, 요구사항/질의사항/수락기준 추출 | `prd/` | opus |
| `project-analyst` | TDD 전략, 상세 설계, 티켓 분해까지 | `project-analysis/` | opus |
| `ticket-splitter` | 큰 기능을 "1일/1PR" 티켓 단위로 분해 | `project-analysis/` | sonnet |
| `be-implementer` | Kotlin/Spring Boot TDD 구현 | 구현 단계 | sonnet |
| `fe-implementer` | React/Next.js TDD 구현 | 구현 단계 | sonnet |
| `pr-reviewer` | 하네스 룰 + 아키텍처 기준 PR 리뷰 | 리뷰 단계 | sonnet |
| `harness-auditor` | 코드베이스 하네스 룰 위반 전수 감사 | 주기 감사 | sonnet |

## 호출 예시

```
Agent(subagent_type="prd-analyst", description="PRD 분석", prompt="...")
Agent(subagent_type="pipeline-runner", description="PRD 파이프라인 실행", prompt="...")
```

## 파일 구조

```markdown
---
name: <id>
description: 언제 이 에이전트를 써야 하는지 (자동 위임 판단에 사용됨)
tools: Read, Grep, Bash, ...
model: opus | sonnet | haiku
---

<시스템 프롬프트 본문>
```