---
description: PR 리뷰 수행 — Senior Gate(LLM-free) → 분야별 5인 병렬 리뷰 → 결과 병합
argument-hint: <PR number or URL>
---

`.analysis/pr-review/PIPELINE.md` 의 절차대로 PR 을 리뷰해줘.

**PR**: $ARGUMENTS

## 단계

### 1. 사전 검증 (LLM-free, 0초)
- `python3 -c "import json; json.load(open('.claude/harness-rules.json'))"` — JSON 유효성
- `python3 .claude/scripts/senior-gate.py --pr $ARGUMENTS` — Critical 4유형
- `python3 .claude/scripts/harness-audit.py --diff-files <변경파일들> --fail-on error`

Critical 1건 이상이면 즉시 fail → `gh pr review --request-changes` + 다음 단계 생략.

### 2. 분야별 5인 병렬 스폰
변경 파일 비율을 보고 적용 가능한 에이전트만 동시 호출:

| 변경 파일 | 호출할 에이전트 |
|-----------|------------------|
| `*.kt` (BE) | `pr-reviewer`, `be-senior`, `be-tech-lead`, `security-reviewer` |
| `*.tsx`/`*.ts` (FE) | `pr-reviewer`, `fe-lead`, `security-reviewer` |
| `*.sql`/마이그레이션 | `be-tech-lead` (데이터 소유권), `pr-reviewer` |
| 인프라 (`.github/`, `Dockerfile`) | `be-tech-lead`, `security-reviewer` |

각 에이전트는 frontmatter 의 `model:` 을 따른다 (대부분 opus).

### 3. 결과 병합
| 단일 verdict | 합산 결과 |
|--------------|-----------|
| 한 명이라도 `request-changes` (Critical) | **request-changes** |
| 모두 `comment` 또는 일부 `approve` | **comment** (사람 판단) |
| 모두 `approve` | **approve** (단, 자동 머지 금지) |

코멘트는 한 번에 작성 — 분야별 섹션:
```
## PR Review Summary
**Verdict**: <verdict>

### 🛡️ Senior Gate (LLM-free)
...
### 📐 pr-reviewer
...
### 🏗️ be-senior
...
### 🌐 be-tech-lead
...
### 🔒 security-reviewer
...
### 🎨 fe-lead
...
```

### 4. 메타-피드백 트리거 결정
verdict=`request-changes` 면 `.analysis/feedback-loop/PIPELINE.md` 의 트리거 1번 충족 → Stop 훅이 `process-reviewer` 발화 (활성화돼 있을 때).

## 운영 규칙

- 자동 머지 절대 금지 (verdict=approve 여도 사람 머지)
- FE/DevOps PR: comment 까지만 (자동 approve 금지)
- BE PR: comment 까지만, 사람 리뷰 대기
- 같은 PR 에 sync 발생 시 직전 컨텍스트 캐시
- 토큰/비용 상한: PR 1개당 최대 5인 × 1회

결과를 Summary/Findings/Verdict 형식으로 보고해줘.
