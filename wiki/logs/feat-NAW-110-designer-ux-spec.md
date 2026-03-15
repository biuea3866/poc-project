
---
### 2026-03-15 00:00
- **Agent:** Claude
- **Task:** Next 마일스톤 UI/UX 디자인 스펙 작성
- **Changes:**
  - docs/design/design-system.md — 디자인 시스템 (색상, 타이포그래피, 스페이싱, 컴포넌트)
  - docs/design/rag-search-ux.md — RAG 검색 UX 스펙 (탭 구조, 결과 카드, API 계약)
  - docs/design/outbox-admin-ux.md — 아웃박스 관리 화면 UX 스펙 (테이블, 상태 뱃지, 재처리)
  - docs/design/editor-tree-spec.md — 에디터 + 사이드바 트리 UX 스펙 (Split view, 자동 저장, AI 상태 분기)
- **Decisions:**
  - 기존 Tailwind 커스텀 색상 토큰 기반으로 디자인 시스템 정의 (새 색상 추가 없이 기존 팔레트 활용)
  - RAG 검색은 비용 고려하여 Enter/탭 클릭 시에만 검색 (키워드는 debounce 자동 검색)
  - 아웃박스 테이블 모바일 대응은 카드 리스트로 전환
  - 에디터 자동 저장은 3초 무입력 후 트리거
- **Next:** FE 개발자가 이 스펙을 기반으로 컴포넌트 구현 시작
---
