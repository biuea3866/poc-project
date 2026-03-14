# Pinpoint JVM 옵션 적용 예시

## Gradle 실행 예시

```bash
JAVA_TOOL_OPTIONS="$(
  ./ai-orchestrator-lab/be/pinpoint/pinpoint-bootstrap.sh | tr '\n' ' '
)" ./gradlew bootRun
```

## java -jar 실행 예시

```bash
java $(
  ./ai-orchestrator-lab/be/pinpoint/pinpoint-bootstrap.sh | tr '\n' ' '
) -jar app.jar
```

## 점검 포인트

- `applicationName`은 서비스 단위로 고정한다.
- `agentId`는 환경별로 유일해야 한다.
- collector 주소는 DevOps baseline과 동일해야 한다.
