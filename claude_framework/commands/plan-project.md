---
description: project-analysis 파이프라인 실행 — 정제된 PRD 경로를 받아 설계/TDD/티켓 8단계 수행
argument-hint: <path to .analysis/prd/YYYY-MM-DD-feature.md>
---

`pipeline-runner` 에이전트로 `.analysis/project-analysis/PIPELINE.md`를 실행해줘.

**입력 PRD 산출물**: $ARGUMENTS

**요구사항**
- 4개 파일 모두 생성: `00-overview.md`, `01-design.md`, `02-tdd.md`, `03-tickets.md`
- Mermaid 3종(Component AS-IS/TO-BE, Sequence, ERD) 포함
- 모든 FR이 ≥1 TC에 연결
- 티켓 각각이 1일 이내 + 대응 TC 링크

완료 후 산출물 폴더 경로, 총 티켓 수, Critical path 길이를 보고해줘.