# Kafka Consumer 성능 비교 가이드

## 테스트 환경

- **하드웨어**: Apple M1/M2 또는 동급 (8GB+ RAM)
- **Kafka**: Confluent Platform 7.5.0
- **Partitions**: 10개
- **Replication Factor**: 1 (로컬 테스트)

## Consumer 전략별 특성

### 1. Normal Consumer (기준선)

```kotlin
@KafkaListener(
    topics = ["normal-events"],
    containerFactory = "normalKafkaListenerContainerFactory"
)
fun listen(record: ConsumerRecord<String, Event>)
```

**특징:**
- Concurrency: 1
- 단건 처리
- Auto Commit
- 간단한 구현

**예상 성능:**
- TPS: 100~500
- 지연: 낮음
- CPU 사용률: 낮음

### 2. Batch Consumer

```kotlin
@KafkaListener(
    topics = ["batch-events"],
    containerFactory = "batchKafkaListenerContainerFactory"
)
fun listen(records: List<ConsumerRecord<String, Event>>)
```

**특징:**
- Concurrency: 10
- 배치 크기: 500
- Auto Commit
- 네트워크 효율 향상

**예상 성능:**
- TPS: 5,000~15,000
- 지연: 중간 (배치 대기)
- CPU 사용률: 중간

### 3. Optimized Consumer

```kotlin
@KafkaListener(
    topics = ["optimized-events"],
    containerFactory = "optimizedKafkaListenerContainerFactory"
)
fun listen(records: List<ConsumerRecord<String, Event>>, ack: Acknowledgment)
```

**특징:**
- Concurrency: 10
- 배치 크기: 500
- Manual Acknowledgment
- 병렬 처리 (ThreadPoolTaskExecutor)
- DLT (Dead Letter Topic) 지원
- Non-blocking Retry

**예상 성능:**
- TPS: 10,000~30,000+
- 지연: 설정에 따라 조절 가능
- CPU 사용률: 높음

## 성능 비교표

| 메트릭 | Normal | Batch | Optimized |
|--------|--------|-------|-----------|
| 예상 TPS | 100~500 | 5K~15K | 10K~30K+ |
| Concurrency | 1 | 10 | 10 |
| Batch Size | 1 | 500 | 500 |
| Commit | Auto | Auto | Manual |
| 병렬 처리 | X | X | O |
| DLT | X | X | O |
| 복잡도 | 낮음 | 중간 | 높음 |

## 테스트 실행 방법

### 1. 환경 준비

```bash
# Docker 인프라 시작
docker-compose up -d

# 애플리케이션 시작
./gradlew bootRun --args='--spring.profiles.active=perf-test'
```

### 2. 워밍업

```bash
# 각 Consumer에 1,000건씩 워밍업
curl -X POST "http://localhost:8080/api/kafka/load-test/send/all?count=1000"
sleep 10
curl -X POST http://localhost:8080/api/kafka/load-test/stats/reset
```

### 3. 본 테스트

```bash
# 10,000건 테스트
curl -X POST "http://localhost:8080/api/kafka/load-test/send/all?count=10000"

# 결과 확인
sleep 30
curl http://localhost:8080/api/kafka/load-test/stats
```

## 튜닝 가이드

### Producer 튜닝

```yaml
spring:
  kafka:
    producer:
      # 배치 크기 (bytes)
      batch-size: 16384
      # 배치 대기 시간 (ms)
      linger-ms: 5
      # 버퍼 메모리 (bytes)
      buffer-memory: 33554432
      # 압축 타입
      compression-type: lz4
      # ACK 레벨
      acks: all
```

### Consumer 튜닝

```yaml
spring:
  kafka:
    consumer:
      # 한 번에 가져올 최대 레코드 수
      max-poll-records: 500
      # 최소 fetch 크기 (bytes)
      fetch-min-size: 1048576
      # fetch 최대 대기 시간
      fetch-max-wait: 500ms
```

### JVM 튜닝

```bash
# 고처리량을 위한 JVM 옵션
java -Xms2g -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar app.jar
```

## 상황별 권장 전략

### 1. 실시간 처리가 중요한 경우

**권장**: Normal Consumer

```kotlin
// 설정
ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 1
// Concurrency: 파티션 수와 동일
```

### 2. 처리량이 중요한 경우

**권장**: Batch Consumer 또는 Optimized Consumer

```kotlin
// 설정
ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 500
ConsumerConfig.FETCH_MIN_BYTES_CONFIG to 1048576
```

### 3. 안정성이 중요한 경우

**권장**: Optimized Consumer (Manual Ack + DLT)

```kotlin
// 설정
ContainerProperties.AckMode.MANUAL_IMMEDIATE
// DLT 설정으로 실패 메시지 보존
```

## 주의사항

### Partition 수와 Concurrency

- Concurrency > Partition 수: 일부 Consumer가 유휴 상태
- Concurrency < Partition 수: 최대 처리량 미달성
- **권장**: Concurrency = Partition 수

### Memory 관리

- 대량 배치 처리 시 Heap 메모리 주의
- `max.poll.records` × 메시지 크기 고려
- OOM 발생 시 배치 크기 축소

### Rebalancing

- Consumer 추가/제거 시 Rebalancing 발생
- Rebalancing 중 처리 중단
- `session.timeout.ms`, `heartbeat.interval.ms` 조절

## 모니터링 체크리스트

### 필수 메트릭

1. **Consumer Lag**: 처리 지연 여부
2. **TPS**: 초당 처리량
3. **Processing Time**: 메시지 처리 시간
4. **Error Rate**: 오류 발생률

### Grafana 알림 설정 권장값

```
- Consumer Lag > 10000: Warning
- Consumer Lag > 100000: Critical
- Error Rate > 1%: Warning
- Error Rate > 5%: Critical
```

## 추가 최적화 방안

### 1. Parallel Consumer (고급)

```kotlin
// Spring Kafka 3.0+ Parallel Consumer
@Bean
fun parallelContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Event> {
    return ConcurrentKafkaListenerContainerFactory<String, Event>().apply {
        // Partition 내 순서 보장하면서 병렬 처리
        containerProperties.isAsyncAcks = true
    }
}
```

### 2. Reactive Kafka

```kotlin
// WebFlux + Reactor Kafka
@Bean
fun reactiveKafkaConsumer(): ReceiverOptions<String, Event> {
    // Non-blocking I/O 기반 처리
}
```

### 3. 메시지 압축

```yaml
spring:
  kafka:
    producer:
      compression-type: lz4  # 또는 snappy, zstd
```

## 결론

| 사용 사례 | 권장 전략 |
|----------|----------|
| 간단한 이벤트 처리 | Normal |
| 로그 수집 | Batch |
| 주문/결제 처리 | Optimized |
| 실시간 알림 | Normal (낮은 지연) |
| 대량 데이터 마이그레이션 | Optimized |
