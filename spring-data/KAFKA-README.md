# Spring Kafka Consumer 예제

Spring Boot + Kafka를 사용한 Consumer 예제 프로젝트입니다. 기본 Consumer부터 고처리량 최적화 Consumer까지 다양한 전략을 비교할 수 있습니다.

## 프로젝트 구조

```
src/main/kotlin/com/biuea/springdata/kafka/
├── config/
│   ├── KafkaTopics.kt              # 토픽 상수 정의
│   ├── KafkaProducerConfig.kt      # Producer 설정
│   ├── KafkaConsumerConfig.kt      # 기본 Consumer 설정
│   ├── NormalConsumerConfig.kt     # 일반 Consumer 설정
│   ├── BatchConsumerConfig.kt      # 배치 Consumer 설정
│   ├── OptimizedConsumerConfig.kt  # 최적화 Consumer 설정
│   ├── ParallelConsumerConfig.kt   # Parallel Consumer 설정 (Confluent)
│   └── MetricsConfig.kt            # 메트릭 설정
├── dto/
│   ├── User.kt                     # 기본 예제용 DTO
│   └── Event.kt                    # 고처리량 예제용 DTO
├── listener/
│   ├── UserEventListener.kt        # 기본 User 이벤트 리스너
│   ├── NormalEventListener.kt      # 일반 Consumer
│   ├── BatchEventListener.kt       # 배치 Consumer
│   ├── OptimizedEventListener.kt   # 최적화 Consumer
│   └── ParallelEventListener.kt    # Parallel Consumer (Confluent)
├── producer/
│   ├── UserEventProducer.kt        # User 이벤트 Producer
│   └── EventProducer.kt            # 부하 테스트용 Producer
├── service/
│   ├── EventProcessor.kt           # 이벤트 처리 로직
│   └── PerformanceMonitor.kt       # 성능 모니터링
└── controller/
    ├── UserController.kt           # 기본 예제 API
    ├── LoadTestController.kt       # 부하 테스트 API
    ├── KafkaAdminController.kt     # 관리자 API
    └── ParallelConsumerController.kt # Parallel Consumer 전용 API
```

## Consumer 전략 비교

| 전략 | 토픽 | Concurrency | 배치 | 특징 |
|------|------|-------------|------|------|
| Normal | `normal-events` | 1 | X | 단건 처리, 비교 기준 |
| Batch | `batch-events` | 10 | O (500) | 배치 처리, Auto Commit |
| Optimized | `optimized-events` | 10 | O (500) | 배치 + 수동 Ack + 병렬 처리 + DLT |
| Parallel | `parallel-events` | 100 | X | Confluent Parallel Consumer, KEY 기반 순서 보장 |

## Confluent Parallel Consumer

### 개요

Confluent Parallel Consumer는 일반 Kafka Consumer의 한계를 극복하기 위해 만들어진 라이브러리입니다.
파티션 수와 관계없이 높은 병렬성을 제공하면서도 KEY 기반 순서 보장이 가능합니다.

### Optimized Consumer (전략 C) vs Parallel Consumer

| 특성 | Optimized (ThreadPool) | Parallel Consumer |
|------|----------------------|-------------------|
| 병렬 처리 방식 | 배치를 청크로 나눠 ThreadPool에서 처리 | 메시지별 병렬 처리 (내부 관리) |
| 순서 보장 | 보장하지 않음 | KEY 기반 순서 보장 가능 |
| 오프셋 커밋 | 배치 단위 (coarse-grained) | 메시지 단위 (fine-grained) |
| 실패 시 재처리 | 배치 전체 재처리 | 실패 메시지만 재처리 |
| 구현 복잡도 | Spring Kafka + ExecutorService | Confluent 라이브러리 사용 |
| 최대 병렬성 | ThreadPool 크기에 종속 | maxConcurrency 설정 (파티션 무관) |

### ProcessingOrder 옵션

```kotlin
ParallelConsumerOptions.builder<String, String>()
    .consumer(consumer)
    .ordering(ProcessingOrder.KEY)  // 같은 키는 순서 보장
    .maxConcurrency(100)            // 최대 동시 처리 수
    .build()
```

- **UNORDERED**: 최대 병렬성, 순서 보장 없음
- **KEY**: 같은 키를 가진 메시지는 순서 보장 (권장)
- **PARTITION**: 파티션 내 순서 보장 (일반 Consumer와 동일)

### 핵심 장점

1. **파티션 독립적 병렬성**: 파티션 수와 관계없이 `maxConcurrency`로 병렬성 제어
2. **KEY 기반 순서 보장**: 같은 키의 메시지는 순차 처리, 다른 키는 병렬 처리
3. **Fine-grained 오프셋 관리**: 개별 메시지 단위로 오프셋 관리
4. **자동 재시도**: 실패한 메시지만 자동 재시도 (배치 전체 X)
5. **EOS 지원**: Exactly-Once Semantics 지원

### 사용 시나리오

- 파티션을 추가하지 않고 처리량을 늘리고 싶을 때
- 사용자별/주문별 등 KEY 기반 순서가 중요할 때
- 실패한 메시지만 재처리하고 싶을 때
- 기존 Consumer 대비 더 세밀한 오프셋 관리가 필요할 때

## 환경 설정

### Docker 실행

```bash
# 전체 인프라 실행 (MySQL, Kafka, Prometheus, Grafana)
docker-compose up -d

# Kafka만 실행
docker-compose up -d zookeeper kafka kafka-ui
```

### 서비스 URL

| 서비스 | URL |
|--------|-----|
| Application | http://localhost:8080 |
| Kafka UI | http://localhost:8090 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |

### 애플리케이션 실행

```bash
./gradlew bootRun
```

## API 사용법

### 기본 예제 (User Events)

```bash
# 단건 메시지 발송
curl -X POST http://localhost:8080/api/kafka/users/send \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "name": "John", "email": "john@example.com"}'

# 대량 메시지 발송
curl -X POST http://localhost:8080/api/kafka/users/send/bulk \
  -H "Content-Type: application/json" \
  -d '{"count": 100, "startId": 1}'
```

### 부하 테스트

```bash
# Normal Consumer 테스트 (10,000건)
curl -X POST "http://localhost:8080/api/kafka/load-test/send/normal?count=10000"

# Batch Consumer 테스트 (10,000건)
curl -X POST "http://localhost:8080/api/kafka/load-test/send/batch?count=10000"

# Optimized Consumer 테스트 (10,000건)
curl -X POST "http://localhost:8080/api/kafka/load-test/send/optimized?count=10000"

# Parallel Consumer 테스트 (10,000건)
curl -X POST "http://localhost:8080/api/kafka/load-test/send/parallel?count=10000"

# 모든 Consumer에 동시 발송
curl -X POST "http://localhost:8080/api/kafka/load-test/send/all?count=10000"

# 비동기 발송 (대량 데이터용)
curl -X POST "http://localhost:8080/api/kafka/load-test/send/all/async?count=100000"
```

### Parallel Consumer 전용 API

```bash
# 메시지 발송 (keyCount로 순서 보장 그룹 설정)
curl -X POST "http://localhost:8080/api/kafka/parallel/send?count=10000&keyCount=10"

# 비동기 대량 발송
curl -X POST "http://localhost:8080/api/kafka/parallel/send/async?count=100000&keyCount=100"

# Parallel Consumer 통계 조회
curl http://localhost:8080/api/kafka/parallel/stats

# 통계 초기화
curl -X POST http://localhost:8080/api/kafka/parallel/stats/reset
```

### 성능 통계 조회

```bash
# 통계 조회
curl http://localhost:8080/api/kafka/load-test/stats

# 통계 초기화
curl -X POST http://localhost:8080/api/kafka/load-test/stats/reset

# 발송 건수 조회
curl http://localhost:8080/api/kafka/load-test/sent-count
```

### 관리자 API

```bash
# 리스너 목록 조회
curl http://localhost:8080/api/kafka/admin/listeners

# 특정 리스너 일시정지
curl -X POST http://localhost:8080/api/kafka/admin/listeners/{listenerId}/pause

# 특정 리스너 재개
curl -X POST http://localhost:8080/api/kafka/admin/listeners/{listenerId}/resume

# 모든 리스너 일시정지
curl -X POST http://localhost:8080/api/kafka/admin/listeners/pause-all

# 모든 리스너 재개
curl -X POST http://localhost:8080/api/kafka/admin/listeners/resume-all

# Kafka 헬스체크
curl http://localhost:8080/api/kafka/admin/health
```

### Actuator 엔드포인트

```bash
# 헬스 체크
curl http://localhost:8080/actuator/health

# Prometheus 메트릭
curl http://localhost:8080/actuator/prometheus

# 메트릭 조회
curl http://localhost:8080/actuator/metrics
```

## 성능 테스트 시나리오

### 1만 건 테스트

```bash
# 통계 초기화
curl -X POST http://localhost:8080/api/kafka/load-test/stats/reset

# 모든 Consumer에 발송
curl -X POST "http://localhost:8080/api/kafka/load-test/send/all?count=10000"

# 결과 확인 (10초 후)
curl http://localhost:8080/api/kafka/load-test/stats
```

### 10만 건 테스트

```bash
curl -X POST http://localhost:8080/api/kafka/load-test/stats/reset
curl -X POST "http://localhost:8080/api/kafka/load-test/send/all/async?count=100000"
# Grafana 대시보드에서 실시간 모니터링
```

## 권장 설정값

### 고처리량 환경

```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 500
      fetch-min-size: 1048576  # 1MB
      fetch-max-wait: 500ms
    producer:
      batch-size: 16384
      linger-ms: 5
      compression-type: lz4
```

### Consumer 튜닝 포인트

1. **Concurrency**: 파티션 수에 맞춰 설정 (최대 = 파티션 수)
2. **Batch Size**: 500~1000 권장
3. **Fetch Size**: 네트워크 대역폭에 따라 조절
4. **Thread Pool**: CPU 코어 수 * 2 권장

## 모니터링

### Grafana 대시보드

1. Grafana 접속: http://localhost:3000
2. 로그인: admin / admin
3. Dashboards → Kafka Consumer Performance

### 주요 메트릭

- `kafka_consumer_normal_count`: Normal Consumer 처리 건수
- `kafka_consumer_batch_count`: Batch Consumer 처리 건수
- `kafka_consumer_optimized_count`: Optimized Consumer 처리 건수
- `kafka_consumer_parallel_count`: Parallel Consumer 처리 건수
- `kafka_consumer_*_tps`: 각 Consumer TPS
- `kafka_consumer_*_processing_time`: 처리 시간

## 트러블슈팅

### Kafka 연결 실패

```bash
# Kafka 상태 확인
docker-compose ps
docker-compose logs kafka

# 토픽 목록 확인
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Consumer Lag 확인

```bash
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group normal-events-consumer
```

### 토픽 삭제 및 재생성

```bash
docker exec -it kafka kafka-topics --delete \
  --topic normal-events \
  --bootstrap-server localhost:9092

docker exec -it kafka kafka-topics --create \
  --topic normal-events \
  --partitions 10 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092
```
