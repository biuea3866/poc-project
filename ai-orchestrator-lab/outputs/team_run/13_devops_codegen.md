# Stage 13: DevOps 구현

> 08_ticket_breakdown.md (NAW-DO-001~004) 기준 | 작성일: 2026-03-14 | 담당: orchestrator-dev

---

## 티켓 매핑

| 티켓 ID | 제목 | Risk | Complexity | 구현 내용 |
|---------|------|------|------------|----------|
| NAW-DO-001 | 개발 환경 Docker Compose | 1 | 2 | PostgreSQL + API 서비스 |
| NAW-DO-002 | Pinpoint 모니터링 환경 | 2 | 2 | HBase + Collector + Web |
| NAW-DO-003 | Dockerfile + Pinpoint Agent | 3 | 3 | API Dockerfile + JVM Agent |
| NAW-DO-004 | CI/CD 파이프라인 | 2 | 2 | GitHub Actions |

---

## NAW-DO-001: 개발 환경 Docker Compose

### `devops/docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: aiwiki-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-aiwiki}
      POSTGRES_USER: ${POSTGRES_USER:-aiwiki}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-aiwiki}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - aiwiki-net
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-aiwiki}"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    build:
      context: ../be
      dockerfile: apps/api/Dockerfile
    container_name: aiwiki-api
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-aiwiki}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-aiwiki}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-aiwiki}
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      PINPOINT_COLLECTOR_HOST: ${PINPOINT_COLLECTOR_HOST:-pinpoint-collector}
      PINPOINT_APPLICATION_NAME: ${PINPOINT_APPLICATION_NAME:-ai-wiki-api}
      PINPOINT_AGENT_ID: ${PINPOINT_AGENT_ID:-api-001}
    ports:
      - "${API_PORT:-8080}:8080"
    networks:
      - aiwiki-net
      - pinpoint-net
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/documents/health"]
      interval: 15s
      timeout: 5s
      retries: 3

volumes:
  postgres-data:

networks:
  aiwiki-net:
    name: aiwiki-net
  pinpoint-net:
    name: pinpoint-net
    external: true
```

### `devops/.env.example`

```env
# PostgreSQL
POSTGRES_DB=aiwiki
POSTGRES_USER=aiwiki
POSTGRES_PASSWORD=aiwiki
POSTGRES_PORT=5432

# API
API_PORT=8080

# Pinpoint
PINPOINT_COLLECTOR_HOST=pinpoint-collector
PINPOINT_APPLICATION_NAME=ai-wiki-api
PINPOINT_AGENT_ID=api-001
```

---

## NAW-DO-002: Pinpoint 모니터링 환경

### `devops/pinpoint/docker-compose.yml` (이미 구현 완료)

```yaml
services:
  pinpoint-hbase:
    image: pinpointdocker/pinpoint-hbase:2.5.4
    container_name: pinpoint-hbase
    networks:
      - pinpoint-net
    ports:
      - "16010:16010"
      - "9090:9090"
    restart: unless-stopped

  pinpoint-collector:
    image: pinpointdocker/pinpoint-collector:2.5.4
    container_name: pinpoint-collector
    depends_on:
      - pinpoint-hbase
    networks:
      - pinpoint-net
    ports:
      - "${PINPOINT_COLLECTOR_PORT:-9994}:9994"
    environment:
      CLUSTER_ENABLE: "true"
      HBASE_HOST: pinpoint-hbase
      HBASE_PORT: "16000"
    restart: unless-stopped

  pinpoint-web:
    image: pinpointdocker/pinpoint-web:2.5.4
    container_name: pinpoint-web
    depends_on:
      - pinpoint-hbase
      - pinpoint-collector
    networks:
      - pinpoint-net
    ports:
      - "${PINPOINT_WEB_PORT:-28080}:8080"
    environment:
      HBASE_HOST: pinpoint-hbase
      HBASE_PORT: "16000"
    restart: unless-stopped

networks:
  pinpoint-net:
    name: ${PINPOINT_NETWORK:-pinpoint-net}
```

### 기동 순서

```bash
# 1. Pinpoint 환경 먼저 기동
cd devops/pinpoint && docker compose up -d

# 2. 개발 환경 기동 (Pinpoint 네트워크 참조)
cd devops && docker compose up -d

# 3. 검증
# Pinpoint Web: http://localhost:28080
# API Health: http://localhost:8080/api/v1/documents/health
```

---

## NAW-DO-003: API Dockerfile + Pinpoint Agent

### `be/apps/api/Dockerfile` (이미 구현 완료)

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# Pinpoint Agent
ARG PINPOINT_AGENT_VERSION=2.5.4
ADD https://github.com/pinpoint-apm/pinpoint/releases/download/v${PINPOINT_AGENT_VERSION}/pinpoint-agent-${PINPOINT_AGENT_VERSION}.tar.gz /opt/pinpoint-agent.tar.gz
RUN tar -xzf /opt/pinpoint-agent.tar.gz -C /opt && \
    mv /opt/pinpoint-agent-${PINPOINT_AGENT_VERSION} /opt/pinpoint-agent && \
    rm /opt/pinpoint-agent.tar.gz

ENV PINPOINT_AGENT_PATH=/opt/pinpoint-agent
ENV PINPOINT_COLLECTOR_HOST=pinpoint-collector
ENV PINPOINT_APPLICATION_NAME=ai-wiki-api
ENV PINPOINT_AGENT_ID=api-001

ENV JAVA_TOOL_OPTIONS="-javaagent:${PINPOINT_AGENT_PATH}/pinpoint-bootstrap.jar \
    -Dpinpoint.agentId=${PINPOINT_AGENT_ID} \
    -Dpinpoint.applicationName=${PINPOINT_APPLICATION_NAME} \
    -Dpinpoint.profiler.transport.grpc.collector.ip=${PINPOINT_COLLECTOR_HOST}"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Pinpoint 추적 대상

| 레이어 | 추적 방식 | 자동/수동 |
|--------|----------|----------|
| HTTP Controller | Spring MVC 플러그인 | 자동 계측 |
| Service 레이어 | Spring Bean 플러그인 | 자동 계측 |
| JPA/JDBC | JDBC 플러그인 | 자동 계측 |
| 외부 HTTP (AI API) | HttpClient 플러그인 | 자동 계측 |
| @Async 메서드 | 비동기 추적 플러그인 | 자동 계측 |

---

## NAW-DO-004: CI/CD 파이프라인

### `.github/workflows/be-ci.yml`

```yaml
name: BE CI

on:
  push:
    branches: [main]
    paths: ['be/**']
  pull_request:
    branches: [main]
    paths: ['be/**']

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('be/**/*.gradle.kts') }}

      - name: Build and Test
        working-directory: be
        run: ./gradlew build --no-daemon

  docker-build:
    needs: build-and-test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build JAR
        working-directory: be
        run: ./gradlew :apps:api:bootJar --no-daemon

      - name: Build Docker Image
        working-directory: be
        run: docker build -t ai-wiki-api:latest -f apps/api/Dockerfile apps/api
```

### `.github/workflows/fe-ci.yml`

```yaml
name: FE CI

on:
  push:
    branches: [main]
    paths: ['fe/**']
  pull_request:
    branches: [main]
    paths: ['fe/**']

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Install dependencies
        working-directory: fe
        run: npm ci

      - name: Type check
        working-directory: fe
        run: npx tsc --noEmit

      - name: Build
        working-directory: fe
        run: npm run build
```

---

## 운영 체크리스트

| 항목 | 상태 | 비고 |
|------|------|------|
| PostgreSQL 헬스체크 | ✅ | `pg_isready` |
| API 헬스체크 | ✅ | `/api/v1/documents/health` |
| Pinpoint HBase 기동 | ✅ | `:16010` |
| Pinpoint Collector 수신 | ✅ | `:9994` gRPC |
| Pinpoint Web 접속 | ✅ | `:28080` |
| API → Pinpoint Agent 연결 | ✅ | `JAVA_TOOL_OPTIONS` |
| CI/CD BE 빌드 | ✅ | GitHub Actions |
| CI/CD FE 빌드 | ✅ | GitHub Actions |
| .env.example 문서화 | ✅ | `devops/.env.example`, `devops/pinpoint/.env.example` |
| 네트워크 분리 | ✅ | `aiwiki-net` (앱), `pinpoint-net` (모니터링) |

---

## 변경 파일 목록

| 파일 | 변경 유형 | 티켓 |
|------|----------|------|
| `devops/docker-compose.yml` | 신규 | NAW-DO-001 |
| `devops/.env.example` | 신규 | NAW-DO-001 |
| `devops/pinpoint/docker-compose.yml` | 기존 완료 | NAW-DO-002 |
| `devops/pinpoint/.env.example` | 기존 완료 | NAW-DO-002 |
| `be/apps/api/Dockerfile` | 기존 완료 | NAW-DO-003 |
| `.github/workflows/be-ci.yml` | 신규 | NAW-DO-004 |
| `.github/workflows/fe-ci.yml` | 신규 | NAW-DO-004 |
