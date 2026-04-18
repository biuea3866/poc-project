---
description: PRD 파이프라인 실행 — PRD 링크/경로를 받아 .analysis/prd/PIPELINE.md 전체 단계를 수행
argument-hint: <PRD URL or path>
---

`pipeline-runner` 에이전트로 `.analysis/prd/PIPELINE.md`를 실행해줘.

**입력**: $ARGUMENTS

**요구사항**
- 파이프라인의 Exit Criteria 전부 통과해야 완료
- 산출물 경로: `.analysis/prd/YYYY-MM-DD-<feature-slug>.md`
- Open Questions가 비어있으면 재분석

완료 후 산출물 경로와 Open Questions 목록을 요약해 보고해줘.