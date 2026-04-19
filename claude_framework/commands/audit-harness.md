---
description: 하네스 룰 전수 감사 — harness-rules.json 기준 코드베이스 스캔
argument-hint: [optional path, defaults to be-repos/ + fe-repos/]
---

`harness-auditor` 에이전트로 `harness-audit` 스킬을 사용해 코드베이스를 감사해줘.

**감사 범위**: ${ARGUMENTS:-be-repos/ + fe-repos/ + devops-repos/}

**절차**
1. `.claude/harness-rules.json` 로드 → `forbidden_patterns.rules` 전수
2. 각 룰 `file_glob` + `exclude_glob` 필터 적용하여 Grep
3. 위반 목록을 severity/rule별 집계
4. false positive 판단은 이유 명시

**수정 금지** — 발견만 보고. 수정은 별도 티켓으로 `be-implementer`/`fe-implementer`에 할당.

Summary(총계/severity/rule별) + Findings(파일:라인:룰ID) + 권고사항 형식으로 보고해줘.