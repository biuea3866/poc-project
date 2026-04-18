---
description: 설계 산출물을 "1명/1일/1PR" 티켓으로 분해
argument-hint: <path to .analysis/project-analysis/YYYY-MM-DD-feature/>
---

`ticket-splitter` 에이전트로 `ticket-breakdown` 스킬을 사용해 티켓을 분해해줘.

**입력 설계 폴더**: $ARGUMENTS

**요구사항**
- 산출물: `$ARGUMENTS/03-tickets.md`
- 각 티켓 YAML 블록 + 종속성 Mermaid 그래프
- 모든 티켓 ≤1일, 모든 FR이 티켓 커버
- Jira MCP 활성 시 바로 올릴 수 있는 포맷으로 출력

완료 후 티켓 목록(id/title/estimate/deps)을 표로 보고해줘.