# Pinpoint APM - Closet Ecommerce

Pinpoint APM(Application Performance Management)을 Docker Compose 환경에서 실행하기 위한 구성입니다.

## 아키텍처

```
                          ┌──────────────────┐
                          │  Pinpoint Web UI │ :8079
                          └────────┬─────────┘
                                   │
                          ┌────────▼─────────┐
                          │ Pinpoint HBase   │ :16010 (Web UI)
                          │ (데이터 저장소)    │
                          └────────▲─────────┘
                                   │
                          ┌────────┴─────────┐
                          │Pinpoint Collector │ :9991-9994
                          └────────▲─────────┘
                                   │ (gRPC)
         ┌─────────┬───────────────┼───────────────┬──────────┐
         │         │               │               │          │
    [gateway] [member]       [product]        [order]   [payment]
     Agent     Agent          Agent           Agent      Agent
```

## 컴포넌트

| 컴포넌트 | 이미지 | 포트 | 설명 |
|----------|--------|------|------|
| pinpoint-hbase | pinpointdocker/pinpoint-hbase:2.5.1 | 16010 | 트레이스/통계 데이터 저장 |
| pinpoint-collector | pinpointdocker/pinpoint-collector:2.5.1 | 9991-9994 | Agent 데이터 수집 |
| pinpoint-web | pinpointdocker/pinpoint-web:2.5.1 | 8079 | Web UI 대시보드 |
| pinpoint-mysql | pinpointdocker/pinpoint-mysql:2.5.1 | 3307 | Web 사용자/알림 데이터 |

## 실행 방법

### 1. Pinpoint Agent 다운로드

```bash
cd docker/pinpoint
./download-agent.sh
```

이 스크립트가 `pinpoint-agent-2.5.1/` 디렉토리에 Agent를 다운로드합니다.

### 2. Pinpoint 서버 시작 (서버 컴포넌트만)

```bash
cd docker
docker compose up -d pinpoint-hbase pinpoint-mysql pinpoint-collector pinpoint-web
```

HBase 초기화에 약 1~2분 소요됩니다. 정상 기동 확인:

```bash
docker compose ps pinpoint-hbase pinpoint-collector pinpoint-web
```

### 3. 전체 스택 시작 (인프라 + Pinpoint + 애플리케이션)

```bash
cd docker
docker compose up -d
```

### 4. Pinpoint Web UI 접속

브라우저에서 [http://localhost:8079](http://localhost:8079) 접속

- HBase Master Web UI: [http://localhost:16010](http://localhost:16010)

## Agent 설정

### Docker Compose 환경 (기본)

`docker-compose.yml`에 이미 각 서비스에 Agent 볼륨 마운트와 JAVA_TOOL_OPTIONS가 설정되어 있습니다.

각 서비스는 다음과 같이 구성됩니다:
```yaml
volumes:
  - ./pinpoint/pinpoint-agent-2.5.1:/opt/pinpoint-agent-2.5.1
environment:
  JAVA_TOOL_OPTIONS: >-
    -javaagent:/opt/pinpoint-agent-2.5.1/pinpoint-bootstrap-2.5.1.jar
    -Dpinpoint.agentId=${SERVICE_NAME}
    -Dpinpoint.applicationName=closet-${SERVICE_NAME}
    -Dpinpoint.config=/opt/pinpoint-agent-2.5.1/pinpoint-root.config
```

### 로컬 실행 환경 (IDE/터미널)

Agent를 다운로드한 후, JVM 옵션에 다음을 추가합니다:

```bash
# Agent가 docker/pinpoint/pinpoint-agent-2.5.1 에 있다고 가정
AGENT_PATH=$(pwd)/docker/pinpoint/pinpoint-agent-2.5.1

java -javaagent:${AGENT_PATH}/pinpoint-bootstrap-2.5.1.jar \
     -Dpinpoint.agentId=local-product \
     -Dpinpoint.applicationName=closet-product \
     -Dpinpoint.config=${AGENT_PATH}/pinpoint-root.config \
     -jar closet-product/build/libs/*.jar \
     --spring.profiles.active=local
```

IntelliJ IDEA에서는 Run Configuration의 VM options에 추가:
```
-javaagent:/path/to/pinpoint-agent-2.5.1/pinpoint-bootstrap-2.5.1.jar
-Dpinpoint.agentId=local-product
-Dpinpoint.applicationName=closet-product
-Dpinpoint.config=/path/to/pinpoint-agent-2.5.1/pinpoint-root.config
```

로컬에서 Collector에 연결하려면 `pinpoint-agent.config`의 `profiler.transport.grpc.collector.ip`를
`localhost`로 변경하거나, 별도의 로컬용 config 파일을 생성하세요.

### Agent 커스텀 설정

`docker/pinpoint/pinpoint-agent.config` 파일에서 다음 항목을 조정할 수 있습니다:

| 설정 | 기본값 | 설명 |
|------|--------|------|
| `profiler.sampling.counting.sampling-rate` | 1 | 샘플링 비율 (1=100%, 10=10%) |
| `profiler.jdbc.tracesqlbindvalue` | true | SQL 바인드 값 추적 |
| `profiler.springboot.enable` | true | Spring Boot 플러그인 |
| `profiler.kafka.enable` | true | Kafka 트레이싱 |
| `profiler.redis.lettuce.enable` | true | Redis Lettuce 트레이싱 |

## 트러블슈팅

### HBase가 시작되지 않음

```bash
# 로그 확인
docker compose logs pinpoint-hbase

# HBase 컨테이너가 healthy 상태인지 확인
docker compose ps pinpoint-hbase

# 볼륨 초기화 후 재시작
docker compose down -v pinpoint-hbase
docker compose up -d pinpoint-hbase
```

### Collector가 HBase에 연결하지 못함

```bash
# Collector 로그 확인
docker compose logs pinpoint-collector

# HBase가 healthy 상태인지 먼저 확인
docker compose ps pinpoint-hbase

# Collector 재시작
docker compose restart pinpoint-collector
```

### Agent가 Collector에 연결하지 못함

1. Collector가 실행 중인지 확인:
   ```bash
   docker compose ps pinpoint-collector
   ```

2. 포트가 정상적으로 리스닝 중인지 확인:
   ```bash
   docker compose exec pinpoint-collector netstat -tlnp | grep -E '999[1-4]'
   ```

3. Agent 로그 확인 (서비스 컨테이너 내부):
   ```bash
   docker compose exec closet-product cat /opt/pinpoint-agent-2.5.1/log/pinpoint-agent.log
   ```

### Pinpoint Web UI에 데이터가 보이지 않음

1. Collector가 데이터를 수신하고 있는지 확인:
   ```bash
   docker compose logs -f pinpoint-collector 2>&1 | grep -i "agent"
   ```

2. 애플리케이션이 Agent와 함께 시작되었는지 확인:
   ```bash
   docker compose logs closet-product 2>&1 | grep -i "pinpoint"
   ```

3. HBase 테이블이 생성되었는지 확인:
   [http://localhost:16010](http://localhost:16010) 에서 Tables 탭 확인

### 포트 충돌

| 포트 | 용도 | 충돌 가능 |
|------|------|-----------|
| 8079 | Pinpoint Web UI | 기존 Gateway가 8080이므로 8079로 매핑 |
| 3307 | Pinpoint MySQL | 기존 MySQL이 3306이므로 3307로 매핑 |
| 16010 | HBase Master UI | - |
| 9991-9994 | Collector (gRPC/TCP) | - |

### 전체 초기화

```bash
cd docker
docker compose down
docker volume rm docker_hbase-data docker_pinpoint-mysql-data
docker compose up -d
```

## 참고

- [Pinpoint 공식 문서](https://pinpoint-apm.gitbook.io/pinpoint/)
- [Pinpoint Docker GitHub](https://github.com/pinpoint-apm/pinpoint-docker)
- Pinpoint 버전: 2.5.1
