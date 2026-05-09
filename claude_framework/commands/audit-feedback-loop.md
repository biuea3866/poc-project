---
description: 피드백 루프 건강 점검 (일일 발화 / Stale 제안 / 효과 측정 / 안전 게이트)
argument-hint: [날짜 YYYY-MM-DD, 기본=오늘]
---

`feedback-loop-guardian` 에이전트로 메타-피드백 루프의 건강을 점검해줘.

**기준일**: $ARGUMENTS (생략 시 오늘)

## 단계

### 1. 일일 발화 상한
- `docs/feedback-loop/proposals/<YYYYMMDD>-*.md` 카운트 → 5개 초과 = high 경고
- 폭주 의심 사유 보고 (반복되는 trigger 인지, 동일 prompt 인지)

### 2. Stale 제안 감지
- frontmatter `status: proposed` 가 7일 이상 지속된 파일 식별
- 같은 trigger 가 그 사이 반복됐는지 확인 → 반복됐는데 미반영이면 med 경고
- 권고 액션 제시 (사람 검토 / archived / closed)

### 3. 효과 측정
- 머지된 `refactor/feedback/*` PR 추적
- 머지 시점 이후 같은 trigger 재발 빈도:
  - 일주일 내 0회 = `effective`
  - 1~2회 = `partial`
  - 3회 이상 = `ineffective`
- `ineffective` 2회 연속 → Issue 자동 발행 후보

### 4. 안전 게이트
- 최근 24h 의 `git log --diff-filter=M -- agents/ skills/ commands/ .claude/harness-rules.json`
- author 가 자동화 봇이면 high 경고 (자동 .md 수정 안티패턴 위반)
- `git_upstream_guard` 가 차단한 이벤트 카운트
- 보호 브랜치(main/master/dev) 에 PR 없이 직접 push 된 commit 감지

### 5. 보고서 작성
`.analysis/feedback-loop/<YYYY-MM-DD>-health.md` 에 저장.

```markdown
# 피드백 루프 건강 보고서

- **Date**: <YYYY-MM-DD>
- **호출 컨텍스트**: nightly | manual
- **종합 verdict**: healthy | warning | critical

## 1. 일일 발화 상한
## 2. Stale 제안
## 3. 효과 측정
## 4. 안전 게이트
## 5. 권고 액션
```

## 운영 규칙

- 제안 파일 자체 생성 금지 (process-reviewer 의 책임)
- `.md` 직접 수정 금지
- "효과 있어 보임" 같은 정성 판정 금지 — 정확한 카운트만

## 참고

- 워크플로우: `.analysis/feedback-loop/PIPELINE.md`
- 가디언 정의: `agents/feedback-loop-guardian.md`
