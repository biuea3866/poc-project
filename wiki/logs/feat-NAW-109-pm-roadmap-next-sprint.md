# feat/NAW-109-pm-roadmap-next-sprint 작업 이력

---
### 2026-03-15 00:00
- **Agent:** Claude
- **Task:** Next 마일스톤 로드맵 상세화 및 스프린트 계획 수립
- **Changes:**
  - `ROADMAP.md` — MVP 완료 항목 체크(NAW-105~108), 현재 구현 상태 갱신, Next 섹션 상세 스펙 추가 (Outbox/RAG/Refresh Token/운영보안)
  - `docs/sprint-next-plan.md` — 신규 생성. 스프린트 목표, BE/FE/DevOps 티켓 브레이크다운, 의존성 맵, 디자인 핸드오프, 리스크 분석
- **Decisions:**
  - Next 마일스톤 스프린트 기간을 2주(03-17~03-28)로 설정
  - RAG 검색 API를 `/api/v1/search/semantic`으로 분리하고, 기존 통합 검색에 `mode` 파라미터 추가
  - Hybrid 검색 랭킹은 RRF(Reciprocal Rank Fusion) 알고리즘 채택
  - Refresh 토큰 회전 시 Family 개념 도입하여 탈취 감지 구현
  - Outbox DEAD_LETTER 전이 기준을 5회로 설정
- **Next:** Jira에 Next 마일스톤 티켓 생성 (BE-1~11, FE-1~3, DO-1~2), 디자이너 NAW-110 핸드오프 확인
---
