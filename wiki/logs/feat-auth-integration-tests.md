---
### 2026-02-18 22:02
- **Agent:** Codex
- **Task:** Testcontainers 기반 인증 엔드포인트 통합 시나리오 테스트 추가 및 Observability 로드맵 반영
- **Changes:** wiki/wiki-api/src/test/kotlin/com/biuea/wiki/integration/AuthApiScenarioIntegrationTest.kt, wiki/wiki-api/build.gradle.kts, wiki/wiki-domain/build.gradle.kts, wiki/wiki-domain/src/main/kotlin/com/biuea/wiki/domain/document/entity/DocumentRevision.kt, wiki/wiki-domain/src/main/kotlin/com/biuea/wiki/domain/ai/AiAgentLog.kt, wiki/ROADMAP.md, wiki/logs/feat-auth-integration-tests.md
- **Decisions:** 인증 시나리오는 현재 구현된 엔드포인트(auth) 기준으로 전부 E2E 검증. 컨테이너는 MySQL/Redis를 동적 주입. Boot4/Hibernate7 호환을 위해 hypersistence 의존 제거 및 JSON 매핑을 표준 Hibernate 방식으로 전환.
- **Issues:** 기존 엔티티 매핑 불일치(`Document.agentLogs` mappedBy 대상 부재)와 Hibernate 버전 불일치로 컨텍스트 기동 실패가 있었고 함께 수정.
- **Next:** 브랜치 푸시 후 PR 생성, 이후 Observability 스택(OTel/Grafana/Loki/Prometheus/Actuator) 실제 구성 작업 진행.
---
### 2026-02-18 22:26
- **Agent:** Codex
- **Task:** Observability 스택(Actuator, OTel Collector, Tempo, Prometheus, Loki, Promtail, Grafana) 실제 compose 구성 및 기동 검증
- **Changes:** wiki/docker-compose.yml, wiki/docker/wiki-api/Dockerfile, wiki/docker/observability/otel-collector-config.yml, wiki/docker/observability/tempo.yml, wiki/docker/observability/prometheus.yml, wiki/docker/observability/loki-config.yml, wiki/docker/observability/promtail-config.yml, wiki/docker/observability/grafana/provisioning/datasources/datasources.yml, wiki/docker/observability/README.md, wiki/wiki-api/build.gradle.kts, wiki/wiki-api/src/main/kotlin/com/biuea/wiki/config/SecurityConfig.kt, wiki/wiki-domain/src/main/resources/application.yml, wiki/wiki-domain/src/main/kotlin/com/biuea/wiki/domain/ai/AiAgentLog.kt, wiki/ROADMAP.md, wiki/logs/feat-auth-integration-tests.md
- **Decisions:** Kafka producer/consumer trace를 위해 Spring Kafka observation(`listener/template`) 활성화. Prometheus 스크랩을 위해 actuator health/prometheus 공개. 로컬 compose 기동 안정성을 위해 wiki-api는 `SPRING_JPA_HIBERNATE_DDL_AUTO=update` 적용.
- **Issues:** 초기 기동 시 Docker credential helper 및 Tempo volume 권한, JPA schema validate 불일치로 wiki-api/tempo가 종료되어 각각 Dockerfile/tempo user/DDL 설정으로 보정.
- **Next:** PR #15에서 observability 변경 검토 후 main 반영. 이후 대시보드/알람(SLO) 구체화.
---
### 2026-02-18 22:59
- **Agent:** Codex
- **Task:** 외부 인프라 지표(node/cadvisor/mysql/kafka/kafka-jvm) 수집 및 Grafana 대시보드 자동 프로비저닝 구축
- **Changes:** wiki/docker-compose.yml, wiki/docker/observability/prometheus.yml, wiki/docker/observability/kafka-jmx-exporter.yml, wiki/docker/observability/README.md, wiki/docker/observability/grafana/provisioning/dashboards/dashboards.yml, wiki/docker/observability/grafana/dashboards/wiki-app-jvm.json, wiki/docker/observability/grafana/dashboards/wiki-infra-host-container.json, wiki/docker/observability/grafana/dashboards/wiki-mysql.json, wiki/docker/observability/grafana/dashboards/wiki-kafka.json, wiki/logs/feat-auth-integration-tests.md
- **Decisions:** Kafka consumer/producer trace는 Spring Kafka observation 설정 유지, Kafka JVM 메트릭은 JMX exporter 사이드카로 수집. Mac Docker mount 제약으로 node-exporter volume propagation 옵션 제거.
- **Issues:** mysqld-exporter 인증 방식 변경(v0.16), kafka-jmx-exporter jar URL 404, cAdvisor/node-exporter mount 제약 이슈가 있어 compose 설정 보정.
- **Next:** 필요 시 대시보드 패널/알람(SLO) 세부 튜닝 및 Alertmanager 연동.
---
### 2026-02-18 23:05
- **Agent:** Codex
- **Task:** Prometheus SLO/인프라 알람 룰 및 Alertmanager 연동 구축
- **Changes:** wiki/docker/observability/prometheus.yml, wiki/docker/observability/prometheus-rules.yml, wiki/docker/observability/alertmanager.yml, wiki/docker-compose.yml, wiki/docker/observability/README.md, wiki/logs/feat-auth-integration-tests.md
- **Decisions:** 알람 파이프라인은 Prometheus rule_files + Alertmanager 기본 라우팅으로 구성. host 포트 충돌(9093)로 Alertmanager를 19093에 노출, Prometheus 내부 타깃은 서비스명(alertmanager:9093) 유지.
- **Issues:** Alertmanager 9093 포트 점유로 기동 실패가 있어 compose 포트 매핑 변경으로 해결.
- **Next:** Slack/Email/PagerDuty receiver 및 Alertmanager route 고도화, SLO 임계치 실사용 트래픽 기준 튜닝.
---
