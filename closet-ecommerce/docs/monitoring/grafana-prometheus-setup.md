# Grafana + Prometheus 모니터링 가이드

## 접근 방법

| 서비스 | URL | 인증 |
|--------|-----|------|
| **Grafana** | http://localhost:3001 | admin / closet |
| **Prometheus** | http://localhost:9090 | - |
| **Loki** (로그) | http://localhost:3100 | - |

## 대시보드 목록

### 1. Closet Service Overview (`closet-service-overview`)
서비스 전체 현황을 한눈에 파악하는 대시보드.
- 서비스 상태 (UP/DOWN) 표시
- 서비스별 총 요청률 (req/s, stacked)
- 응답 시간 분포 (p50/p95/p99)
- 에러율 (4xx/5xx)
- 서비스별 활성 요청 게이지

### 2. Closet JVM Metrics (`closet-jvm-metrics`)
각 Spring Boot 서비스의 JVM 상세 메트릭.
- Heap / Non-Heap 메모리 사용량
- GC Pause Time
- Thread Count (Live/Daemon/Peak)
- Process CPU Usage
- HikariCP 커넥션 풀 (Active/Idle/Max)
- HikariCP Connection Acquire Time
- Flyway Migration 상태

### 3. Closet Infrastructure (`closet-infrastructure`)
외부 인프라 서비스 메트릭.
- **MySQL**: Connections, QPS (SELECT/INSERT/UPDATE/DELETE), Slow Queries, Table Size, InnoDB Buffer Pool
- **Redis**: Memory Used/Max, Connected Clients, Commands/sec, Hit Rate, Evictions
- **Kafka**: Messages In/Out per sec, Consumer Lag, Partition 수, Broker 수
- **Host**: CPU Usage, Memory Usage, Disk Usage, Network I/O

## 알림 규칙 (8개)

| 알림 | 조건 | 심각도 |
|------|------|--------|
| ServiceDown | `up == 0` (1분 이상) | critical |
| HighLatency | p95 응답시간 > 1초 (5분 이상) | warning |
| HighErrorRate | 5xx 에러율 > 5% (5분 이상) | critical |
| HighHeapUsage | JVM Heap > 80% (5분 이상) | warning |
| HighMySQLConnections | MySQL 커넥션 > 80% (5분 이상) | warning |
| HighRedisMemory | Redis 메모리 > 80% (5분 이상) | warning |
| HighKafkaLag | Consumer Lag > 1000 (5분 이상) | warning |
| DBConnectionPoolExhausted | HikariCP 풀 > 90% (2분 이상) | critical |

## Exporter 설명

| Exporter | 포트 | 용도 |
|----------|------|------|
| **mysql-exporter** | 9104 | MySQL 서버 메트릭 수집 (커넥션, QPS, InnoDB 등) |
| **redis-exporter** | 9121 | Redis 서버 메트릭 수집 (메모리, 클라이언트, 커맨드 등) |
| **kafka-exporter** | 9308 | Kafka 클러스터 메트릭 수집 (토픽, 파티션, Consumer Lag 등) |
| **node-exporter** | 9100 | 호스트 OS 메트릭 수집 (CPU, 메모리, 디스크, 네트워크) |

> **참고**: `node-exporter`는 macOS에서 `/proc`, `/sys` 마운트 불가로 정상 동작하지 않을 수 있음. Linux 환경에서만 사용 권장.

## 모니터링 스택 실행

```bash
# 전체 모니터링 스택 실행
docker compose -f docker/docker-compose.yml up -d prometheus grafana loki mysql-exporter redis-exporter kafka-exporter

# 확인
curl -s http://localhost:9090/-/healthy   # Prometheus 헬스
curl -s http://localhost:3001/api/health   # Grafana 헬스
```

## 커스텀 메트릭 추가 방법

### 1. Counter 메트릭 추가
```kotlin
@Component
class OrderMetrics(private val meterRegistry: MeterRegistry) {
    private val orderCounter = Counter.builder("closet.orders.created")
        .description("생성된 주문 수")
        .tag("type", "normal")
        .register(meterRegistry)

    fun incrementOrderCount() {
        orderCounter.increment()
    }
}
```

### 2. Timer 메트릭 추가
```kotlin
@Component
class PaymentMetrics(private val meterRegistry: MeterRegistry) {
    fun recordPaymentDuration(duration: Duration) {
        Timer.builder("closet.payment.duration")
            .description("결제 처리 시간")
            .register(meterRegistry)
            .record(duration)
    }
}
```

### 3. Gauge 메트릭 추가
```kotlin
@Component
class InventoryMetrics(
    meterRegistry: MeterRegistry,
    private val inventoryRepository: InventoryRepository
) {
    init {
        Gauge.builder("closet.inventory.low_stock_count") {
            inventoryRepository.countLowStock().toDouble()
        }
        .description("안전재고 미달 상품 수")
        .register(meterRegistry)
    }
}
```

### 4. Grafana에서 커스텀 메트릭 조회
Prometheus에 `closet_` 접두사로 자동 노출됨 (`.` -> `_` 변환).
Grafana에서 `closet_orders_created_total` 등으로 쿼리.

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                      Grafana (:3001)                        │
│  ┌──────────────────┬─────────────────┬──────────────────┐  │
│  │ Service Overview │  JVM Metrics    │ Infrastructure   │  │
│  └────────┬─────────┴────────┬────────┴────────┬─────────┘  │
│           │                  │                 │            │
│           └──────────────────┼─────────────────┘            │
│                              │                              │
│                     ┌────────▼────────┐                     │
│                     │  Prometheus     │                     │
│                     │  (:9090)        │                     │
│                     └────────┬────────┘                     │
│              ┌───────┬───────┼───────┬───────┐              │
│              │       │       │       │       │              │
│         ┌────▼──┐┌───▼──┐┌───▼──┐┌───▼──┐┌───▼───┐         │
│         │MySQL  ││Redis ││Kafka ││Node  ││Spring │         │
│         │Export ││Export││Export││Export││Boot   │         │
│         │:9104  ││:9121 ││:9308 ││:9100 ││:808x  │         │
│         └───┬───┘└───┬──┘└───┬──┘└───┬──┘└───┬───┘         │
│             │        │       │       │       │              │
│         ┌───▼───┐┌───▼──┐┌───▼──┐┌───▼──┐   │              │
│         │MySQL  ││Redis ││Kafka ││Host  │   │              │
│         │:3306  ││:6379 ││:9092 ││OS    │   │              │
│         └───────┘└──────┘└──────┘└──────┘   │              │
│                                              │              │
│    ┌─────────┬─────────┬──────────┬──────────┤              │
│    │         │         │          │          │              │
│  gateway  member   product    order     payment             │
│  :8080    :8081    :8082      :8083     :8084               │
└─────────────────────────────────────────────────────────────┘
```
