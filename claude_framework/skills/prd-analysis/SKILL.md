---
name: prd-analysis
description: PRD 원문에서 FR/NFR, 모호한 점, Acceptance Criteria를 추출하는 절차. PRD 링크나 원문을 받으면 자동 활성화되며, 최소 2개 경쟁사 리서치와 Open Questions 수집을 강제한다. prd-analyst 에이전트가 내부에서 참조한다.
---

# PRD Analysis Skill

## 언제 사용하나
- 새 기능/제품 PRD가 들어왔을 때
- 기존 PRD 개정본 재분석 시
- "PRD 분석", "요구사항 정리", "AC 만들어줘" 류의 요청

## 핵심 원칙
- **빈 껍데기 금지** — Open Questions가 0개면 아직 이해 부족
- **모호한 표현 제거** — "빠르게", "적절히", "사용자 친화적으로" → 측정 가능한 지표로 재작성
- **PRD 원문 복붙 금지** — 반드시 재구성

## 절차

### Step 1. 요구사항 태깅
PRD 전문을 읽으며 각 문장을 다음 중 하나로 태깅:
- `FR` (Functional Requirement)
- `NFR` (Non-Functional Requirement)
- `CTX` (배경/맥락, 요구사항 아님)
- `ASSUME` (가정)
- `OUT` (범위 밖)

### Step 2. 번호 부여
- FR-01, FR-02 … / NFR-01, NFR-02 …
- 각 항목 뒤에 PRD 출처 인용 `[PRD §2.3]`

### Step 3. 모호성 스캔
다음 표현을 grep하고 각각 Open Question 생성:
- "빠르게", "느리게", "많이", "적게"
- "사용자 친화적", "직관적", "모던한"
- "나중에", "우선", "추후 논의"
- "기존과 동일", "자동으로"

### Step 4. Acceptance Criteria 작성 규칙
```
AC-<번호> (FR-<번호>): 
  Given <전제>
  When <동작>
  Then <관측 가능한 결과>
```
- 모호한 술어 금지 (`동작한다` ✗, `200 OK + body.status == "success"` ✓)
- 부정 케이스(실패 경로) 최소 1개 포함

### Step 5. 경쟁사 리서치 (최소 2개)
각 경쟁사에 대해:
- 제품명 + URL
- 동일 기능의 UX/데이터 흐름
- 스크린샷 또는 API 문서 링크
- "우리 제품에 차용 가능한 점" / "차용 불가한 이유"

### Step 6. Open Questions 포맷
```
Q-<번호> → @<담당자>: <질문>
  결정 영향: <답변에 따라 바뀌는 설계 요소>
  긴급도: P0/P1/P2
```

### Step 7. 리스크 & 가정 분리
- **Assumption**: 입증되지 않았지만 진행을 위해 전제로 두는 것
- **Risk**: 발생 시 일정/기술/비즈니스에 타격 있는 사건
- 각 Risk에 완화 방안 필수

## 산출물 템플릿
`.analysis/prd/YYYY-MM-DD-<feature-slug>.md`

```markdown
# <Feature Name> — PRD Analysis
**Date:** YYYY-MM-DD | **PRD:** <link> | **PM:** @a / **PO:** @b / **TL:** @c

## Summary
## Context
## Requirements
### Functional
### Non-Functional
## Ambiguities
## Competitive Research
## Acceptance Criteria
## Risks & Assumptions
## Open Questions
## References
```

## 완료 체크
- [ ] 모호한 표현 0개
- [ ] AC 각각이 Given/When/Then
- [ ] 경쟁사 ≥ 2
- [ ] Open Questions 담당자 + 긴급도 명시
- [ ] Risk 각각에 완화 방안