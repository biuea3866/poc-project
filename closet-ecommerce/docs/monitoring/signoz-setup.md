# SigNoz APM 설치 가이드

## 개요

SigNoz는 Datadog 스타일의 오픈소스 APM(Application Performance Monitoring) 도구이다.
트레이싱, 메트릭, 로그를 하나의 UI에서 통합 관리할 수 있다.

## 주요 기능

| 기능 | 설명 |
|------|------|
| **Service Map** | 서비스 간 의존성 그래프 |
| **Trace Flame Graph** | 요청 단위 플레임 그래프 (지연 구간 시각화) |
| **Metrics Dashboard** | P99/P95 레이턴시, RPS, 에러율 등 |
| **Log Aggregation** | 서비스 로그 통합 조회 및 검색 |
| **Alerts** | 메트릭 기반 알림 설정 |

## 접속 정보

| 항목 | URL / 포트 |
|------|-----------|
| **SigNoz UI** | http://localhost:3301 |
| **OTLP gRPC** | localhost:14317 |
| **OTLP HTTP** | localhost:14318 |
| **Query Service** | http://localhost:8180 |
| **ClickHouse HTTP** | http://localhost:8123 |

## 시작 / 중지

```bash
# 시작
make signoz-up

# 중지
make signoz-down
```

## 아키텍처

```
  Spring Boot App
       │
       │  OTLP (gRPC :14317 / HTTP :14318)
       ▼
  ┌──────────────┐
  │ OTel Collector│
  └──────┬───────┘
         │
    ┌────▼─────┐
    │ ClickHouse│  ← 트레이스/메트릭/로그 저장
    └────┬─────┘
         │
  ┌──────▼───────┐
  │ Query Service │
  └──────┬───────┘
         │
  ┌──────▼───────┐
  │   Frontend   │  ← http://localhost:3301
  └──────────────┘
```

## Spring Boot 연동

애플리케이션에서 SigNoz로 트레이스를 전송하려면 OpenTelemetry Java Agent를 사용한다.

```bash
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=closet-order \
  -Dotel.exporter.otlp.endpoint=http://localhost:14317 \
  -jar app.jar
```

또는 `application.yml`에서 설정:

```yaml
otel:
  exporter:
    otlp:
      endpoint: http://localhost:14317
  service:
    name: ${spring.application.name}
```

## 포트 충돌 참고

| 포트 | 서비스 | 비고 |
|------|--------|------|
| 14317 | OTLP gRPC | 기본 4317에서 변경 (Jaeger 충돌 방지) |
| 14318 | OTLP HTTP | 기본 4318에서 변경 |
| 9001 | ClickHouse TCP | 기본 9000에서 변경 |
| 3301 | SigNoz UI | 고유 포트 |
