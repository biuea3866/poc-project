# 전략별 특성 · 컴포넌트 역할

6개 전략은 모두 같은 데이터 흐름을 따릅니다. 차이는 **읽기·처리·쓰기 3단계를 어떻게 병렬화하느냐**뿐입니다.

```
[Reader] 발송 묶음 1건 읽기  →  [Processor] 발송(시뮬)  →  [Writer] sent=1 일괄 갱신
                                    └ chunk(N건) 모이면 트랜잭션 커밋 ┘
```

- 발송 단위 = `GROUP BY (user_id, notification_type, group_key)` 한 행 = `NotificationGroup` 1개
- 트랜잭션 경계 = chunk (ADR-0001). 발송 후 `sent` 갱신은 Writer가 chunk 단위 bulk update

성능 분석은 [benchmark-analysis.md](./benchmark-analysis.md) 참고.

---

## 1. 공통 컴포넌트 (6전략이 공유)

### 도메인 (`domain/`)

| 컴포넌트 | 역할 |
|---|---|
| `NotificationGroup` | 발송 단위(묶음) Rich Entity. `toMessage()`가 **N건→묶음 / 1건→단건** 메시지를 결정. 불변·stateless |
| `NotificationType` | 유형 enum(POST_COMMENT/KEYWORD_INTEREST/TICKET_REMINDER) + group_key 의미 + 메시지 템플릿 |
| `NotificationMessage` | 실제 발송 페이로드 value |
| `NotificationDispatchService` | `group.toMessage()` → `gateway.send()` 오케스트레이션(도메인 서비스) |
| `NotificationSendGateway` | 발송 외부 시스템 추상 interface |
| `ReservedNotificationRepository` | reset/count/userIdRange 등 고수준 영속 연산 interface |

### 애플리케이션 (`application/`)

| 컴포넌트 | 역할 |
|---|---|
| `SendNotificationGroupUseCase` | 묶음 1건 발송. **트랜잭션 없음**(chunk가 소유, ADR-0001). Processor가 호출 |
| `ResetBatchStateUseCase` | 벤치마크 반복 사이 `sent` 플래그 리셋(측정 구간 밖) |

### 인프라 (`infrastructure/`)

| 컴포넌트 | 역할 | 비고 |
|---|---|---|
| `NotificationSendGatewayImpl` | 발송 시뮬레이션 — CPU 해싱 + latency. **stateless**(멀티스레드 공유 안전) | gateway |
| `GroupedNotificationSql` | GROUP BY·type 필터·markSent SQL 상수 | persistence |
| `NotificationGroupRowMapper` | 집계 행 → `NotificationGroup` 매핑 | persistence |
| `GroupedNotificationReaders` | `JdbcCursorItemReader` 팩토리. **MySQL 스트리밍**(`fetchSize=MIN_VALUE`)으로 메모리 절약. `fullRange`/`type`/`userRange` 3종 | persistence |
| `NotificationSentBulkUpdater` | chunk 단위 `sent=1` bulk update. **stateless** | persistence |
| `ReservedNotificationRepositoryImpl` | Repository QueryDSL 구현(reset/count/range) | persistence |
| `NotificationGroupItemProcessor` | **공용 Processor** — `ItemProcessor`, UseCase 호출. 전략 1·3·4·5·6 공유. stateless | batch/common |
| `NotificationGroupItemWriter` | **공용 Writer** — `ItemWriter`, `bulkUpdater.markSent(chunk)`. 전략 1·4·5·6 공유(3은 Async가 래핑) | batch/common |
| `BatchConstants` | Job 이름·chunk 크기·`CPU_CORES` 상수 | batch/common |

### 벤치마크 (`presentation/`)

| 컴포넌트 | 역할 |
|---|---|
| `BenchmarkRunner` | `benchmark.mode=benchmark`일 때 6전략×N회 실행·측정·리포트. `JobOperator.start()` 호출 |
| `MetricsSampler` | 측정 중 별도 데몬 스레드로 heap·CPU 100ms 폴링 |
| `BenchmarkReporter` | 결과를 markdown + CSV 산출 |
| `ReservedNotificationSeeder` / `SeedRunner` | `mode=seed`일 때 JDBC batch insert로 대량 시딩 |

---

## 2. 전략별 특성과 전용 컴포넌트

### 전략 1 — 단일 스레드 Chunk (기준선)

| 특성 | 값 |
|---|---|
| 병렬화 | 없음 (단일 스레드) |
| chunk | 1000 |
| 스레드 안전 | 불필요 |
| 장점 | 가장 단순. `saveState=true`로 **restart 지원** |
| 단점 | 처리량 최저(기준선) |
| 용도 | 재처리 안정성 우선, 소·중량 배치 |

```
스레드1: read→process→write→read→process→write ...  (직렬)
```

| 전용 컴포넌트 | 역할 |
|---|---|
| `SingleChunkJobConfig` | `singleChunkStep`(chunk 1000) 구성. reader=`fullRangeReader`, processor·writer=공용 |

---

### 전략 2 — 단일 스레드 Tasklet

| 특성 | 값 |
|---|---|
| 병렬화 | 없음 (단일 스레드) |
| 처리 단위 | tasklet이 직접 스트리밍하며 1000건마다 수동 커밋 |
| 트랜잭션 | step은 `ResourcelessTransactionManager`(DB 무관), 내부에서 `TransactionTemplate`로 1000건씩 독립 커밋 |
| 장점 | 청크 프레임워크 부기 오버헤드가 적음. 복잡한 제어 흐름에 유연 |
| 단점 | reader/processor/writer 분리 이점 없음. 단일 거대 트랜잭션 위험(→ 내부 수동 chunking으로 회피) |
| 용도 | 비청크형 처리, 단발 작업, 커스텀 루프 |

```
스레드1(Tasklet 1회 호출): 커서 스트리밍 → 건건 발송 → 1000건마다 markSent 커밋 ... → FINISHED
```

| 전용 컴포넌트 | 역할 |
|---|---|
| `SingleTaskletJobConfig` | `singleTaskletStep`을 `ResourcelessTransactionManager`로 구성 |
| `SendAllGroupsTasklet` | `JdbcTemplate` 스트리밍 read → `UseCase` 발송 → 1000건 버퍼 차면 `TransactionTemplate`로 `bulkUpdater.markSent` 커밋. **공용 Processor/Writer 미사용**(직접 구현) |

---

### 전략 3 — Async Processor / Writer

| 특성 | 값 |
|---|---|
| 병렬화 | **Processor만** 스레드풀 병렬 (Reader·Writer는 단일 스레드) |
| chunk | 500 |
| 스레드풀 | core=코어수, max=코어수×2, queue 200 + CallerRuns(백프레셔) |
| 장점 | Reader/Writer 변경 없이 무거운 처리만 병렬화 |
| 단점 | Reader·Writer가 직렬 구간으로 남아 **암달의 법칙**에 걸림. 처리가 가벼우면 이득 작음 |
| 용도 | processor가 압도적 병목(무거운 변환·외부 호출) |

```
Reader(스레드1) ─┬─ process(풀 스레드A) ─┐
                 ├─ process(풀 스레드B) ─┼─→ Writer(스레드1, Future unwrap)
                 └─ process(풀 스레드C) ─┘
```

| 전용 컴포넌트 | 역할 |
|---|---|
| `AsyncJobConfig` | `asyncStep`(chunk 500) 구성 |
| `asyncItemProcessor` | `AsyncItemProcessor` — 공용 Processor를 감싸 `asyncTaskExecutor`로 병렬 실행, `Future` 반환 |
| `asyncItemWriter` | `AsyncItemWriter` — `Future`를 unwrap해 공용 Writer로 위임 |
| `asyncTaskExecutor` | core=코어수, max=코어수×2 스레드풀 |

---

### 전략 4 — Multi-thread Step

| 특성 | 값 |
|---|---|
| 병렬화 | **읽기·처리·쓰기 전부** N스레드 (단일 step 멀티스레드) |
| chunk | 500 |
| 스레드 | 코어수 |
| 스레드 안전 | **`SynchronizedItemStreamReader`로 read() 직렬화 필수**. cursor reader `saveState=false`(→ **restart 미지원**) |
| 장점 | 스캔 1번 + 최대 다운스트림 병렬 → **처리량 최고** |
| 단점 | restart 미지원. 공유 reader가 잠재적 병목 |
| 용도 | 단일 DB 대량 처리에서 처리량 우선 |

```
공유 Reader(동기화) ─┬─ 스레드A: read→process→write(자기 chunk)
                     ├─ 스레드B: read→process→write
                     └─ 스레드C: read→process→write   ... 코어수만큼
```

| 전용 컴포넌트 | 역할 |
|---|---|
| `MultiThreadStepJobConfig` | `multiThreadStep`(chunk 500, `taskExecutor`) 구성 |
| `multiThreadReader` | `SynchronizedItemStreamReader`로 `fullRangeReader`를 감싸 멀티스레드 read 직렬화 |
| `multiThreadTaskExecutor` | core=max=코어수 스레드풀 |

---

### 전략 5 — Parallel Step (Split flow)

| 특성 | 값 |
|---|---|
| 병렬화 | 유형 3종을 독립 flow로 **동시 실행**(고정 3-way) |
| chunk | 500 |
| 스레드 | 3 (유형 수) |
| 스레드 안전 | **불필요** — flow마다 전용 reader가 자기 유형만 읽음(데이터 자연 분리) |
| 장점 | 공유 reader 병목 없음. 3스레드로 10스레드급 처리량 근접 |
| 단점 | 팬아웃이 분할 축 개수(3)로 고정 |
| 용도 | 작업이 자연 분할되는 축이 있을 때(유형·테넌트·지역) |

```
split ─┬─ postCommentFlow:  read(POST)→process→write   (스레드1)
       ├─ keywordFlow:      read(KEYWORD)→process→write (스레드2)
       └─ ticketFlow:       read(TICKET)→process→write  (스레드3)
            → 가장 느린 flow가 끝나면 Job 완료
```

| 전용 컴포넌트 | 역할 |
|---|---|
| `ParallelStepJobConfig` | 유형별 `typeStep`(chunk 500, `typeReader`) 3개 → `typeFlow` 3개 → `split`으로 묶어 Job 구성 |
| `parallelTaskExecutor` | 스레드 3개(유형 수) 풀 |

---

### 전략 6 — Partitioning

| 특성 | 값 |
|---|---|
| 병렬화 | user_id 범위를 gridSize 등분해 **파티션별 독립 실행**(N-way) |
| chunk | 500 |
| gridSize / 스레드 | 코어수 |
| 스레드 안전 | **불필요** — 파티션마다 `@StepScope` reader가 서로 다른 범위를 읽음(범위 분리) |
| 장점 | 진짜 N-way 병렬. 데이터가 물리 분리(샤딩)되면 선형 확장 |
| 단점 | 단일 테이블에선 N개 동시 스캔 경합 + **메모리 N배**(스트리밍 커서 N개). restart 안정성 트레이드오프 |
| 용도 | 샤딩된 DB·파일 샤드 등 파티션이 독립 자원을 쓸 때 |

```
Manager step ─ Partitioner가 user_id [min,max]를 gridSize 등분
   ├─ partition0: read(1~1000)     →process→write  (스레드1)
   ├─ partition1: read(1001~2000)  →process→write  (스레드2)
   └─ ...                                            ... gridSize만큼
        → 모든 파티션 종료 시 Manager 완료
```

| 전용 컴포넌트 | 역할 |
|---|---|
| `PartitionJobConfig` | `partitionManagerStep`(partitioner + gridSize + `taskExecutor`) + `partitionWorkerStep`(chunk 500) 구성 |
| `UserIdRangePartitioner` | `userIdRange()`를 gridSize 등분해 각 파티션 ExecutionContext에 `[userIdFrom, userIdTo]` 주입 |
| `partitionReader` | `@StepScope` — 파티션별 범위를 `@Value`로 받아 `userRangeReader` 생성 |
| `partitionTaskExecutor` | gridSize(코어수) 스레드풀 |

---

## 3. 한눈에 보기

| 전략 | 병렬 단계 | 스레드 | reader 동기화 | restart | 메모리 | 처리량(실측) |
|---|---|---|---|---|---|---|
| 1 Chunk | 없음 | 1 | - | ✅ | 낮음 | 2,651/s |
| 2 Tasklet | 없음 | 1 | - | (수동) | 낮음 | 2,978/s |
| 3 Async | 처리 | 1+풀 | 불필요 | ✅ | 낮음 | 3,333/s |
| 4 Multi-thread | 읽기·처리·쓰기 | 코어수 | **필요** | ❌ | 낮음 | **7,119/s** |
| 5 Parallel | 유형별 flow | 3 | 불필요 | ✅ | 낮음 | 6,091/s |
| 6 Partitioning | 범위별 파티션 | 코어수 | 불필요 | ❌ | **높음** | 5,503/s |

> 멀티스레드 전략(4·6)은 reader `saveState=false`라 restart 미지원입니다. 처리량과 재시작성은 트레이드오프입니다.
