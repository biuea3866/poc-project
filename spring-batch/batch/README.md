# Spring Batch 6전략 성능 비교 예제

동일한 예약 알림 데이터셋(100만건)을 6가지 Spring Batch 전략으로 처리하고, 각 5회 평균으로
**메모리·CPU·처리속도**를 비교합니다.

- Spring Boot 4.0.1 / Kotlin 2.2.21 / JDK 21 / Spring Batch 6.0.1
- 헥사고날 아키텍처 + Rich Domain Model + QueryDSL + Testcontainers
- 트랜잭션 경계 등 설계 트레이드오프는 [docs/adr/0001](docs/adr/0001-batch-transaction-boundary.md) 참고
- 전략별 특성·컴포넌트 역할: [docs/strategies.md](docs/strategies.md)
- 측정 결과: [docs/benchmark-result.md](docs/benchmark-result.md) · 원인 분석: [docs/benchmark-analysis.md](docs/benchmark-analysis.md)

## 도메인

예약 알림(`reserved_notification`)을 유저의 알림 유형별로 묶어 발송합니다.

| 유형 | group_key | 묶음 규칙 |
|---|---|---|
| POST_COMMENT | postId | 게시글 단위로 댓글 알림을 묶어 1건 발송 |
| KEYWORD_INTEREST | keyword | 키워드 단위로 신규 물건 알림을 묶어 1건 발송 |
| TICKET_REMINDER | ticketId | 예매 단위 리마인드 |

- 발송 단위 = `GROUP BY (user_id, notification_type, group_key)` 한 행
- 같은 그룹이 N건이면 묶음 메시지, 1건이면 단건 메시지(도메인 `NotificationGroup.toMessage()`)
- 발송 시뮬레이션 = CPU 작업(해싱) + 약간의 latency(sleep) — 병렬화 효과의 부하원

## 6개 전략

| # | 전략 | Job 이름 | 핵심 |
|---|---|---|---|
| 1 | 단일스레드 Chunk | `singleChunkJob` | chunk=1000, 스레드 1 (기준선) |
| 2 | 단일스레드 Tasklet | `singleTaskletJob` | 스트리밍 + 1000건 내부 수동 chunking |
| 3 | Async Processor/Writer | `asyncJob` | Processor만 스레드풀 병렬, Writer는 단일 |
| 4 | Multi-thread Step | `multiThreadStepJob` | 단일 step 멀티스레드, SynchronizedItemStreamReader |
| 5 | Parallel Step (Split) | `parallelStepJob` | 유형 3종 독립 flow 병렬, 데이터 자연 분리 |
| 6 | Partitioning | `partitionJob` | user_id 범위 분할, 파티션별 독립 Reader |

## 실행 방법

### 1) MySQL 기동

```bash
docker compose up -d        # localhost:3307, db=batch_perf
```

### 2) 데이터 시딩 (1회)

```bash
./gradlew bootRun --args='--benchmark.mode=seed'
# 기본 100만건 / 유저 1만명. 조정:
# --benchmark.mode=seed --benchmark.total-count=1000000 --benchmark.user-count=10000 --benchmark.key-pool=15
```

### 3) 벤치마크 실행

```bash
./gradlew bootRun --args='--benchmark.mode=benchmark'
# 6전략 × 5회. 결과 → docs/benchmark-result.md, docs/benchmark-result.csv
```

`benchmark.mode` 미지정(none)이면 아무 Job도 실행하지 않습니다(컨텍스트만 기동).

## 측정 지표

| 지표 | 수집 방법 |
|---|---|
| 처리속도 | `System.nanoTime()` 소요시간 + StepExecution write count → 묶음/s |
| 메모리 | `MemoryMXBean` heap used (peak / avg), 100ms 폴링 |
| CPU | `OperatingSystemMXBean.processCpuLoad`, 100ms 폴링 |

reset·GC는 측정 구간 밖에서 수행해 매 회차 동일 작업량을 보장합니다.

## 테스트

```bash
./gradlew test
```

| 계층 | 대상 | 도구 |
|---|---|---|
| 단위 | domain(묶음/단건 규칙), application(UseCase) | Kotest + MockK |
| 통합 | GROUP BY 쿼리·bulk update | Testcontainers MySQL |
| 시나리오 | 6전략 동일출력 수렴(`StrategyEquivalenceTest`) | Testcontainers + JobLauncher |

## 패키지 구조

```
presentation/   BenchmarkRunner, MetricsSampler, BenchmarkReporter, seed/
application/    SendNotificationGroupUseCase, ResetBatchStateUseCase
domain/         NotificationGroup, NotificationMessage, NotificationType,
                NotificationDispatchService, Repository·Gateway interface
infrastructure/ gateway/, persistence/(QueryDSL·GROUP BY·bulk update), batch/(6전략 Config)
```
