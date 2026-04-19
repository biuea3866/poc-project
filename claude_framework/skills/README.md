# skills — 재사용 절차/노하우

Skill은 에이전트가 참조하는 "재사용 가능한 절차 모듈"이다. 파이프라인(무엇/순서), 에이전트(누가), 스킬(어떻게)의 삼각 관계에서 **how**에 해당한다.

## Skill 카탈로그

| Skill | 용도 | 주 사용 에이전트 | 참조 파이프라인 |
|---|---|---|---|
| `prd-analysis` | PRD에서 FR/NFR/AC/Open Questions 추출 | prd-analyst | `prd/` |
| `project-analysis-flow` | 8단계 설계+TDD+티켓 분해 | project-analyst | `project-analysis/` |
| `mermaid-diagrams` | Component/Sequence/ERD 작성 규칙 | project-analyst | `project-analysis/` |
| `ticket-breakdown` | "1명/1일/1PR" 티켓 분해 | ticket-splitter | `project-analysis/` |
| `tdd-loop` | Red→Green→Refactor 사이클 | be-implementer, fe-implementer | 구현 단계 |
| `kotlin-spring-impl` | Kotlin/Spring BE 구현 7대 원칙 (문법/함수형/패턴/OOP/Rich Domain/풀네임/Enum 전이) | be-implementer | 구현 단계 |
| `pr-review-checklist` | PR 리뷰 체크리스트 + Verdict | pr-reviewer | 리뷰 단계 |
| `harness-audit` | 하네스 룰 전수 감사 절차 | harness-auditor | 주기 감사 |
| `codebase-convention-scan` | 기존 코드베이스에서 컨벤션 추출 → 룰 델타 | convention-detective | `/init --scan` |

## Agent vs Skill vs Pipeline

| 구분 | 역할 | 형식 | 호출 |
|---|---|---|---|
| **Pipeline** | 무엇을/어떤 순서로 | `.analysis/*/PIPELINE.md` | 사용자 또는 pipeline-runner |
| **Agent** | 누가 실행 (페르소나/도구) | `agents/*.md` | Agent 툴 (`subagent_type`) |
| **Skill** | 어떻게 (절차/노하우/템플릿) | `skills/*/SKILL.md` | Skill 툴 또는 에이전트가 본문 참조 |

## 호출 방식

### 자동 위임
Claude가 frontmatter의 `description`을 보고 요청과 매칭되면 스스로 호출.

### 명시 호출
```
Skill(skill="prd-analysis")
Skill(skill="tdd-loop")
```

### 에이전트 내부 참조
에이전트 프롬프트 본문에서 "이 절차는 `prd-analysis` 스킬을 따른다"고 명시하면, 해당 에이전트가 실행될 때 스킬 본문을 로드해 참조.

## 파일 구조

```markdown
---
name: <id>
description: <언제 자동 활성화되어야 하는지 — trigger 조건 명확히>
---

# <Skill Name>

## 언제 사용하나
## 원칙
## 절차 (Step 1, 2, 3 ...)
## 산출물 템플릿
## 완료 체크
```