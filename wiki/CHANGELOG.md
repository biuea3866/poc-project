# Changelog

## [v0.2.0] - 2026-03-16

### Added
- **N-1** Transactional Outbox 패턴 — Kafka 발행 신뢰성 보장 (at-least-once delivery)
- **N-2** RAG 벡터 시맨틱 검색 — pgvector 코사인 유사도 + RRF Hybrid 검색
- **N-3** Refresh 토큰 회전(Rotation) + 탈취 감지 — 토큰 Family 체인 추적
- **N-4** SLO/알람 대시보드 + AI 비용 모니터링 (Grafana + Prometheus)
- **S3-1** 댓글/대댓글 CRUD + FE CommentSection 컴포넌트
- **S3-2** 인앱 알림 시스템 + SSE 실시간 알림 + NotificationBell UI

### Fixed
- FE 마크다운 미리보기 (MarkdownEditor에 MarkdownRenderer 통합)
- FE→BE API 프록시 (Next.js rewrites)
- @tailwindcss/typography 플러그인 추가

### Infrastructure
- wiki-worker AI 메트릭 (Micrometer) — Anthropic/OpenAI API 호출 카운터
- Loki 로그 보관 정책 (30일 hot retention)

## [v0.1.0] - 2026-03-10

### Added
- MVP: 문서 CRUD, AI 파이프라인 (요약/태깅/임베딩), SSE 실시간 상태
- Auth API (회원가입/로그인/Refresh)
- FE: 에디터, 문서 트리, 검색, Trash
