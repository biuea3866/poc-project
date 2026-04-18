# PRD 분석 파이프라인

## 목적
PM/PO가 작성한 PRD를 기술 관점에서 검증 가능한 형태로 정제한다. 이 파이프라인의 산출물은 `project-analysis` 파이프라인의 입력이 된다.

## 담당 에이전트 & 스킬

| 역할 | 에이전트 | 스킬 | 참조 |
|---|---|---|---|
| 오케스트레이션 | `pipeline-runner` | — | `.claude/agents/pipeline-runner.md` |
| 분석 실행 | `prd-analyst` | `prd-analysis` | `.claude/agents/prd-analyst.md` / `.claude/skills/prd-analysis/SKILL.md` |

**관계**: `pipeline-runner`가 이 파이프라인을 읽고 → `prd-analyst` 에이전트를 spawn → 에이전트 내부에서 `prd-analysis` 스킬의 절차를 따라 실행.

호출 방식:
```
Agent(subagent_type="pipeline-runner", prompt="PRD 파이프라인 실행: <PRD URL>")
# 또는 스킬을 직접 호출
Skill(skill="prd-analysis")
```

## 입력 (필수)
- **PRD 링크** — Notion/Confluence/Google Docs
- **관련 Jira 에픽/스토리 ID** (있으면)
- **이해관계자** — PM, PO, Tech Lead 이름/슬랙 핸들
- **데드라인 또는 마일스톤**

입력이 부족하면 pipeline-runner는 진행을 중단하고 사용자에게 확인 요청한다.

## 단계별 상세

### 1. 컨텍스트 수집
- PRD 전문 다운로드 (WebFetch 또는 Notion MCP)
- 관련 기존 설계 문서 검색 (`.analysis/prd/`, `.analysis/project-analysis/` 과거 산출물)
- 유사 기능 선행 사례 리서치 (자체 제품 + 경쟁사)

### 2. 요구사항 추출
- **Functional Requirements** — 시스템이 해야 하는 것 (번호 부여: FR-01, FR-02 …)
- **Non-Functional Requirements** — 성능, 가용성, 보안, 접근성, 국제화 (NFR-01 …)
- 각 항목에 출처(PRD 섹션 번호 또는 인용) 병기

### 3. 모호한 점 식별
- 해석 여지 있는 조건
- 충돌하는 요구사항
- 기술적으로 불가/고비용 가능성 있는 항목
- 표 형식: `| 항목 | 모호한 이유 | 물어볼 대상 | 긴급도 |`

### 4. 경쟁사/선행사례 리서치
- 최소 2개 경쟁 제품 벤치마킹
- 스크린샷 링크, UX 패턴, 데이터 모델 추정
- "우리 제품에 적용 가능한가" 판단 기록

### 5. 수락 기준(Acceptance Criteria) 초안
- Given/When/Then 형식
- 테스트 가능해야 함 ("잘 동작한다" 같은 모호한 표현 금지)
- 각 FR과 1:N 매핑

### 6. 리스크 & 가정
- Assumptions (입증되지 않은 전제)
- Risks (일정/기술/비즈니스)
- 각 리스크의 완화 방안

### 7. 질의사항 정리 (가장 중요)
- PM/PO에게 돌려줄 질문 목록
- "모르겠다"를 방치하지 않고 모두 Open Question으로 올림
- 각 질문에 "답변 받으면 어떤 결정을 바꿀 수 있는지" 포함

## 산출물 구조

파일 경로: `.analysis/prd/YYYY-MM-DD-<feature-slug>.md`

```markdown
# <Feature Name> — PRD Analysis

**Date:** YYYY-MM-DD
**PRD:** <link>
**Stakeholders:** PM=@a, PO=@b, TL=@c
**Deadline:** YYYY-MM-DD

## Summary
<2-3문장 요약>

## Context
<관련 이전 작업, 기존 시스템 컨텍스트>

## Requirements
### Functional
- FR-01: ...
- FR-02: ...
### Non-Functional
- NFR-01: ...

## Ambiguities
| 항목 | 이유 | 담당자 | 긴급도 |
|---|---|---|---|

## Competitive Research
### <경쟁사 A>
- ...
### <경쟁사 B>
- ...

## Acceptance Criteria
- AC-01 (FR-01): Given ..., When ..., Then ...

## Risks & Assumptions
- A-01: ...
- R-01: ... (완화책: ...)

## Open Questions
- Q-01 → @PM: <질문> (결정 영향: ...)
- Q-02 → @TL: ...

## References
- PRD: <link>
- 경쟁사: ...
- 선행 ADR: ...
```

## Exit Criteria
아래 항목이 모두 참이어야 파이프라인 완료로 간주한다.

- [ ] FR/NFR에 모호한 표현 없음 ("적절히", "빠르게" 등)
- [ ] 모든 Open Question에 답변 확보 또는 PM 티켓 발행됨
- [ ] Acceptance Criteria가 테스트 가능한 Given/When/Then 형식
- [ ] 최소 2개 경쟁사 리서치 포함
- [ ] 리스크 각각에 완화 방안 명시

Exit Criteria 미충족 시 pipeline-runner는 재작업 지시. 단순 반복 실패 시 사용자에게 에스컬레이션.

## 다음 단계
산출물이 준비되면 `project-analysis/PIPELINE.md`로 넘어간다 — 설계/TDD/티켓 분해 단계.
