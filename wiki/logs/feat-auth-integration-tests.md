---
### 2026-02-18 22:02
- **Agent:** Codex
- **Task:** Testcontainers 기반 인증 엔드포인트 통합 시나리오 테스트 추가 및 Observability 로드맵 반영
- **Changes:** wiki/wiki-api/src/test/kotlin/com/biuea/wiki/integration/AuthApiScenarioIntegrationTest.kt, wiki/wiki-api/build.gradle.kts, wiki/wiki-domain/build.gradle.kts, wiki/wiki-domain/src/main/kotlin/com/biuea/wiki/domain/document/entity/DocumentRevision.kt, wiki/wiki-domain/src/main/kotlin/com/biuea/wiki/domain/ai/AiAgentLog.kt, wiki/ROADMAP.md, wiki/logs/feat-auth-integration-tests.md
- **Decisions:** 인증 시나리오는 현재 구현된 엔드포인트(auth) 기준으로 전부 E2E 검증. 컨테이너는 MySQL/Redis를 동적 주입. Boot4/Hibernate7 호환을 위해 hypersistence 의존 제거 및 JSON 매핑을 표준 Hibernate 방식으로 전환.
- **Issues:** 기존 엔티티 매핑 불일치(`Document.agentLogs` mappedBy 대상 부재)와 Hibernate 버전 불일치로 컨텍스트 기동 실패가 있었고 함께 수정.
- **Next:** 브랜치 푸시 후 PR 생성, 이후 Observability 스택(OTel/Grafana/Loki/Prometheus/Actuator) 실제 구성 작업 진행.
---
