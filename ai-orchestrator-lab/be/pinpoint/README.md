# Pinpoint Integration Notes

## Purpose

백엔드 애플리케이션에서 Pinpoint 에이전트를 어떻게 주입하고 어떤 트래픽을 추적할지 정리하는 스캐폴드입니다.

## Initial Tasks

1. JVM 시작 옵션에 Pinpoint agent 경로를 연결합니다.
2. 서비스 이름, 애플리케이션 이름, 에이전트 id 규칙을 정합니다.
3. 민감 정보가 포함된 요청/응답은 추적 범위를 제한합니다.
4. 배포 환경별 agent 주입 방식을 DevOps와 함께 정합니다.

## Recommended Tech Doc Topics

- Pinpoint agent injection strategy
- Trace scope for HTTP, Kafka, DB
- Local/dev/prod rollout plan
- Sampling and overhead policy

## Example Runtime Options

```bash
-javaagent:/opt/pinpoint/pinpoint-bootstrap.jar
-Dpinpoint.applicationName=ai-orchestrator-api
-Dpinpoint.agentId=ai-orchestrator-api-dev
```
