---
description: PR 리뷰 수행 — 하네스 룰 + 아키텍처 + 테스트 + 보안 체크리스트 기반
argument-hint: <PR number or URL>
---

`pr-reviewer` 에이전트로 `pr-review-checklist` 스킬을 사용해 PR을 리뷰해줘.

**PR**: $ARGUMENTS

**절차**
1. `gh pr view/diff`로 변경 범위 파악
2. A~G 체크리스트 순회 (하네스 / 레이어 / 테스트 / 보안 / 성능 / 가독성)
3. Critical / Major / Minor 분류
4. Verdict 결정
5. `gh pr review`로 코멘트 게시

**운영 규칙**
- FE/DevOps PR: 체크리스트 통과 시 자동 approve
- BE PR: comment만, 사람 리뷰 대기 (자동 approve 금지)

결과를 Summary/Findings/Verdict 형식으로 보고해줘.