# Zipkin 분산 트레이싱 설정

## 개요

Zipkin은 분산 트레이싱 시스템으로, 마이크로서비스 간 요청 흐름을 추적하고 지연 시간을 분석한다.
기존 Tempo(OTLP) 트레이싱과 병행하여 Zipkin을 추가 백엔드로 운영한다.

## 접근

- **Zipkin UI**: http://localhost:9411
- `make zipkin` 명령으로 브라우저에서 바로 열 수 있다.

## 주요 기능

### 1. 트레이스 검색
- 서비스명, 스팬명, 태그, 시간 범위로 트레이스 검색
- 최소/최대 지연 시간 필터링

### 2. 트레이스 타임라인
- 요청이 서비스를 거치는 전체 경로를 타임라인으로 시각화
- 각 스팬의 시작 시간, 소요 시간, 에러 여부 확인

### 3. 서비스 의존성 그래프
- 서비스 간 호출 관계를 방향성 그래프로 표시
- 호출 빈도와 에러율 시각화

### 4. 지연 분석
- 서비스별 / 엔드포인트별 지연 시간 분포 확인
- 병목 구간 식별

## 아키텍처

```
[Spring Boot 서비스] ---(OpenTelemetry Zipkin Exporter)---> [Zipkin :9411]
                    \
                     ---(OTLP Exporter)---> [Tempo :4317/4318] ---> [Grafana]
```

모든 서비스는 OpenTelemetry 기반으로 트레이싱하며, Zipkin Exporter를 통해 Zipkin에도 스팬을 전송한다.
Tempo(OTLP)와 Zipkin 두 백엔드에 동시에 전송하므로 양쪽 UI에서 모두 트레이스 조회가 가능하다.

## 설정

### Gradle 의존성 (root build.gradle.kts)

```kotlin
implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
```

### application.yml (각 서비스)

```yaml
management:
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
  tracing:
    sampling:
      probability: 1.0
```

### Docker

```bash
# 단독 실행
docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin:latest

# docker-compose로 실행 (docker/docker-compose.yml에 포함)
make up
```

## 대상 서비스

| 서비스 | 포트 | Zipkin 전송 |
|--------|------|------------|
| closet-gateway | 8080 | O |
| closet-member | 8081 | O |
| closet-product | 8082 | O |
| closet-order | 8083 | O |
| closet-payment | 8084 | O |
| closet-bff | 8085 | O |
| closet-shipping | 8086 | O |
| closet-inventory | 8087 | O |
| closet-search | 8088 | O |
| closet-review | 8089 | O |
| closet-promotion | 8090 | O |
| closet-display | 8091 | O |
| closet-seller | 8092 | O |
| closet-cs | 8093 | O |

## 트러블슈팅

### 트레이스가 보이지 않는 경우
1. Zipkin 컨테이너 상태 확인: `docker ps | grep zipkin`
2. 포트 9411 접근 확인: `curl -s http://localhost:9411/api/v2/services`
3. 서비스 로그에서 Zipkin 연결 에러 확인
4. `management.tracing.sampling.probability`가 0이 아닌지 확인 (1.0 = 100% 샘플링)
