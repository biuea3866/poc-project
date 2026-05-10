---
description: PR 리뷰 — Senior Gate(LLM-free) → 분야별 5인 병렬 리뷰 → 결과 병합
argument-hint: <PR number or URL>
---

# /review-pr — PR 리뷰 절차

**PR**: $ARGUMENTS


PR 의 **출력물(코드 + 문서) 리뷰** 절차. 메타-피드백(`outputs/feedback-loop/`) 과 분리.

## 진입점

- 자동: `pr-senior-review.yml` 워크플로우가 PR open/sync 시 호출
- 수동: `/review-pr <PR번호>` (사용자가 호출)

## 단계

### 1. 사전 검증 (자동/LLM-free, 0초)
- harness-rules.json JSON 유효성 검증 (silent-pass 방지)
- senior-gate.py 실행 — Critical 4유형 검출:
  - 관리자 엔드포인트 `@RoleRequired` 누락
  - HttpServletRequest 직접 주입
  - Listener 의 Repository 직접 호출
  - UseCase `@Transactional` 누락
- harness-audit.py --diff-files 로 변경 파일만 룰 위반 검사

Critical 1건 이상이면 즉시 fail → 다음 단계 생략, REQUEST_CHANGES.

### 2. 분야별 리뷰어 병렬 스폰
다음 5개 에이전트를 동시 호출:

| 에이전트 | 모델 | 관점 |
|----------|------|------|
| `pr-reviewer` | opus | 룰셋·레이어 경계 |
| `be-senior` | opus | 운영 리스크·트랜잭션·동시성 |
| `be-tech-lead` | opus | 시스템 경계·데이터 소유권 |
| `security-reviewer` | opus | OWASP·인증·권한·시크릿 |
| `fe-lead` | opus | FE 아키텍처·BFF Facade |

> FE/BE 변경 비율에 따라 일부 에이전트는 생략 가능 (변경 0줄이면 스폰 생략).

### 3. 결과 병합
각 에이전트의 verdict 를 다음 규칙으로 합산:
- 한 명이라도 `request-changes` (Critical) → PR `request-changes`
- 모두 `comment` 또는 `approve` 가 섞임 → `comment` (사람 판단)
- 모두 `approve` → `approve`

코멘트는 분야별 섹션으로 분리해 한 번에 작성:
```
## PR Review Summary
**Verdict**: request-changes

### 🛡️ Senior Gate (LLM-free)
...

### 📐 pr-reviewer
...

### 🏗️ be-senior
...

### 🔒 security-reviewer
...

### 🎨 fe-lead
...
```

### 4. 피드백 루프 트리거 결정
- `request-changes` verdict → `outputs/feedback-loop/` 의 트리거 조건 1번 충족
- Stop 훅이 process-reviewer 발화 (조건 충족 시)

### 5. 재시도 (선택)
- 사용자가 `/review-pr <num> --retry` 호출 시 재실행
- main-orchestrator 가 자동으로 N회 재시도 (default 3, 비용 상한 적용)

## 산출물

- GitHub PR 코멘트 (gh CLI)
- 각 에이전트의 raw output (디버깅용, `outputs/review-pr/<date>-<pr>/<agent>.md`)

## 룰

- Critical 검출은 senior-gate.py 가 우선 (LLM 호출 없이 0초)
- LLM 리뷰는 Critical 통과 후에만 (비용 보호)
- 같은 PR 에 sync 가 발생하면 직전 리뷰 컨텍스트 캐시
- 자동 머지 금지 (verdict=approve 여도 사람 머지)

## 참고

- 워크플로우: `.github/workflows/pr-senior-review.yml`
- LLM-free 게이트: `.claude/scripts/senior-gate.py`
- 상위 설계: `REFACTOR.md` §4 6단계, §5.1

## 완료 단언 규칙

> "완료/검증 끝" 같은 단언은 [`rules/COMPLETION-RULE.md`](../COMPLETION-RULE.md) 의 §1~4 (강제 산출물 / 검증 아티팩트 / 도구 호출 선행 / "지금 시작" 단언 금지) 를 모두 충족해야 한다. 충족 안 된 항목이 있으면 `in-progress` 로 보고.
