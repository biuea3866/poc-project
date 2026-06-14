# Spring Batch 6전략 성능 결과 분석

측정 원본: [benchmark-result.md](./benchmark-result.md) · [benchmark-result.csv](./benchmark-result.csv)
설계 배경: [adr/0001-batch-transaction-boundary.md](./adr/0001-batch-transaction-boundary.md)

## 1. 요약 (결론부터)

| 순위 | 전략 | 평균 소요 | 처리량(묶음/s) | 기준선 대비 | 평균 CPU | peak heap |
|---|---|---|---|---|---|---|
| 1 | Multi-thread Step | 21,275ms | 7,119 | **2.69×** | 8.7% | 78MB |
| 2 | Parallel Step (Split) | 24,695ms | 6,091 | 2.32× | 7.6% | 79MB |
| 3 | Partitioning | 28,608ms | 5,503 | 2.00× | 6.9% | **120MB** |
| 4 | Async Processor/Writer | 46,144ms | 3,333 | 1.24× | 4.8% | 78MB |
| 5 | 단일 Tasklet | 50,480ms | 2,978 | 1.13× | 3.7% | 78MB |
| 6 | 단일 Chunk(기준선) | 57,191ms | 2,651 | 1.00× | 3.4% | 82MB |

**핵심 한 줄**: 10코어인데도 최고 가속이 2.69×에 그치고 CPU 사용률이 3~9%에 머뭅니다. 이 워크로드는 **CPU-bound 가 아니라 DB·조정(coordination)-bound** 이며, 병렬화 이득은 "코어를 채워서"가 아니라 "DB 대기 시간을 겹쳐서" 나옵니다.

## 2. 측정 조건

| 항목 | 값 |
|---|---|
| 데이터 | raw 예약 알림 1,000,000건 → 발송 묶음 149,793건 (평균 6.7행/묶음) |
| 발송 시뮬 | CPU 해싱 40,000 iters + latency 0 (parkNanos 의 OS 타이머 granularity≈1ms 때문에 sub-ms latency 비활성) |
| 반복 | 전략당 5회 평균 (매 회차 reset + GC 후 측정) |
| 하드웨어 | 10 logical cores |
| DB | MySQL 8.0 (동일 호스트 Docker 컨테이너) |
| 측정 | 소요=nanoTime, 처리량=write count/소요, CPU=`OperatingSystemMXBean.processCpuLoad`(전 코어 정규화), heap=`MemoryMXBean`(100ms 폴링) |

### 데이터 분포 (편향 없음 — 분석의 전제)

병렬 전략 비교가 공정하려면 데이터가 균등해야 합니다. 실측 결과 균등합니다.

| 분포 | 결과 |
|---|---|
| 유형별 묶음 (parallel 3-flow) | POST_COMMENT 49,935 / KEYWORD 49,925 / TICKET 49,933 → **균형** |
| user_id 1,000명 구간별 묶음 (partition 10-way) | 14,972 ~ 14,984 (편차 **<0.1%**) → **균형** |
| 묶음 크기 | 단건 0.8%, 2~5행 33%, 6~15행 66%, 16+행 0.1% |

→ **partition 의 데이터 스큐는 없습니다.** 따라서 partition 이 multi-thread 보다 느린 것은 분할 불균형이 아니라 다른 원인(아래 3-6)입니다.

## 3. 왜 이런 순위가 나왔나 — 전략별 원인

각 전략을 "읽기(GROUP BY 스캔) / 처리(발송) / 쓰기(sent bulk update)" 3단계의 병렬화 정도로 보면 결과가 설명됩니다.

### 1·2위 Multi-thread Step (2.69×) vs Parallel Split (2.32×)

- **Multi-thread Step**: 커서 1개를 `SynchronizedItemStreamReader` 로 공유하고 10스레드가 처리+쓰기를 병렬 수행합니다.
  - GROUP BY **스캔은 단 1번**(공유 커서). 스트리밍 커서에서 다음 행을 꺼내는 `read()` 는 매우 싸므로 동기화 병목이 작습니다.
  - 처리(CPU)와 쓰기(chunk별 bulk update)는 10스레드로 병렬 → **단일 효율 스캔 + 최대 다운스트림 병렬**.
- **Parallel Split**: 유형 3종을 독립 flow 3개로 나눠 각자 전용 커서로 읽습니다.
  - 스레드는 3개뿐인데 multi-thread(10스레드)와 거의 맞먹습니다(24.7s vs 21.3s).
  - 이유: **병목이 스레드 수가 아니라 DB**입니다. 전용 커서 3개로 이미 DB 처리량 한계에 근접합니다. "공유 reader 병목을 없앤 것"이 "스레드를 늘린 것"만큼 효과적임을 보여줍니다.
  - 한계: 팬아웃이 3으로 고정(유형 수)이라 그 이상 확장 불가.

### 3위 Partitioning (2.00×) — 스레드는 가장 많은데 왜 3위인가

10파티션이 각자 독립 커서를 가지므로 이론상 가장 빨라야 하지만 실제로는 multi-thread 보다 느립니다. 데이터는 균등(2절)하므로 스큐가 아니라 다음이 원인입니다.

| 원인 | 근거 |
|---|---|
| **10개 동시 GROUP BY 스캔의 DB 경합** | multi-thread 는 스캔 1번, partition 은 user_id 범위별 GROUP BY 10번 동시 실행 → MySQL buffer pool·임시테이블(그룹핑)·I/O 경합 |
| **10배 메모리 → GC 압박** | 스트리밍 커서 10개 + chunk 버퍼 10개 동시 점유 → peak heap 78MB→최대 198MB. 단일 스캔 전략(78MB)의 ~2.5배 |
| **파티션별 프레임워크 오버헤드** | StepExecution 10개, 트랜잭션·메타데이터 기록 10벌 |
| **높은 분산** | 회차별 22s/79MB ~ 40s/198MB. GC 타이밍에 따라 출렁임 → 평균을 끌어내림 |

→ partition 은 **물리적으로 분리된 데이터(샤딩된 DB, 파일 샤드)** 에서 빛납니다. 단일 테이블을 범위로 쪼개면 동시 스캔이 같은 자원을 두고 경합해 이점이 반감됩니다.

### 4위 Async Processor/Writer (1.24×) — "멀티스레드"인데 왜 이득이 작은가

AsyncItemProcessor/Writer 는 **처리(processor)만** 스레드풀로 병렬화합니다.

- **읽기는 단일 스레드**(커서 1개를 한 스레드가 순차 read).
- **쓰기도 단일 스레드**(Writer 가 Future 를 unwrap 해 메인 스텝 스레드에서 bulk update).
- 즉 읽기·쓰기가 직렬 구간으로 남아 **암달의 법칙**에 걸립니다. 발송(CPU)이 전체 비용의 일부일 뿐이라 그 부분만 병렬화해서는 이득이 제한적입니다.
- CPU 4.8% 로 단일(3.4%)보다 약간만 높음 = 부분 병렬화의 증거.

→ async 는 **processor 가 압도적 병목(무거운 외부 호출·변환)** 이고 읽기·쓰기가 쌀 때 적합합니다. 이 워크로드처럼 발송이 가볍고 DB 왕복이 비중 있으면 이득이 작습니다.

### 5·6위 단일 Tasklet (1.13×) ≈ 단일 Chunk (기준선)

- 둘 다 단일 스레드라 기준선 수준(2,600~3,000 묶음/s)입니다.
- Tasklet 이 Chunk 보다 근소하게 빠른 이유: Chunk 는 청크마다 Spring Batch 의 청크 트랜잭션 + `BATCH_STEP_EXECUTION` 메타데이터 갱신 등 프레임워크 부기(bookkeeping)가 더 많습니다. Tasklet 은 자체 `TransactionTemplate` 으로 1,000건씩 단순 커밋하므로 청크 프레임워크 오버헤드가 적습니다.
- 차이는 ~13%로 측정 분산(예: Chunk 4회차 70s) 안에 들 수 있습니다. **둘은 사실상 동급 기준선**으로 보는 게 맞습니다.

## 4. 관통하는 인사이트

1. **이 워크로드는 DB-bound 다.** CPU 가 3~9%(전 코어 정규화)에 머뭅니다. 순수 CPU-bound 라면 multi-thread 가 10×에 근접하고 CPU 가 ~100% 여야 합니다. 실제 가속 2.69×, 병렬 효율 27% → 병목은 MySQL 처리량(커서 스캔·bulk update)과 전략별 직렬화 지점입니다.
2. **DB 가 같은 호스트에 co-located 다.** 앱 JVM 과 MySQL 컨테이너가 같은 10코어를 나눠 씁니다. `processCpuLoad` 는 앱만 측정하므로 MySQL 의 CPU 는 빠져 있습니다. 즉 "남는 코어"처럼 보여도 실제로는 DB 가 코어를 쓰고 있어 확장 천장이 낮습니다.
3. **"공유 reader 1개 + 다운스트림 병렬"이 "reader N개 동시 스캔"을 이긴다.** 단일 테이블에서는 스캔을 한 번만 하고(=multi-thread) 처리/쓰기를 병렬화하는 편이, 스캔을 N개로 쪼개(=partition) 같은 자원을 경합시키는 것보다 유리했습니다.
4. **병렬화 이득은 빠르게 포화한다.** 3-way(parallel)와 10-way(multi-thread)의 처리량 차이가 17%뿐(6,091 vs 7,119)입니다. DB 가 병목이라 일정 동시성 이후 스레드를 늘려도 효용이 급감합니다.
5. **메모리는 동시 스트림 수에 비례한다.** 단일 스캔 전략 ~78MB, partition(10 스트림) 최대 198MB. 처리량을 위해 동시성을 올리면 메모리·GC 비용을 같이 냅니다.

## 5. 측정 타당성·한계 (Validity threats)

- **DB co-location**: 앱과 MySQL 이 코어를 공유해 절대 수치가 낙관/비관 양방향으로 왜곡될 수 있습니다. 분리 호스트면 병렬 전략 이득이 더 커질 여지가 있습니다.
- **latency 0**: OS 타이머 한계로 sub-ms latency 를 끄고 CPU-bound 로 측정했습니다. 실제 외부 발송(수십 ms I/O 대기)이 있으면 async/multi-thread 의 상대 이득이 더 커집니다(대기 시간 겹치기 효과).
- **분산**: 단일 머신이라 GC·OS 스케줄링으로 회차 편차 ±20%(예: async 4회차 62s vs 평균 46s). 5회 평균으로 완화했으나 partition 의 분산은 큽니다.
- **단일 측정 환경**: 1대 기준이라 절대값보다 **상대 경향**으로 해석해야 합니다.

## 6. 권장 선택 가이드

| 상황 | 권장 전략 | 이유 |
|---|---|---|
| 단일 DB, 단순 대량 처리 | **Multi-thread Step** | 단일 스캔 + 최대 병렬. 구현 단순, 처리량 최고 |
| 처리(변환/외부 호출)가 압도적 병목 | **Async** | 읽기·쓰기 싸고 처리만 무거우면 적합. 단 이 예제처럼 처리 가벼우면 이득 작음 |
| 작업이 자연 분할되는 축이 있음(유형·테넌트) | **Parallel Split** | 전용 reader 로 공유 병목 제거. 팬아웃은 분할 축 개수로 고정 |
| 데이터가 물리적으로 샤딩됨(샤드 DB·파일) | **Partitioning** | 파티션이 독립 자원을 쓰면 진짜 선형 확장. 단일 테이블에선 동시 스캔 경합·메모리 비용 주의 |
| 복잡 제어 흐름·비청크 처리 | **Tasklet** | 단일 트랜잭션 폭증만 주의(내부 수동 chunking 필요) |
| 재시작·재처리 안정성 최우선 | **단일 Chunk** | saveState 기반 restart 지원. 멀티스레드 전략은 saveState=false 로 restart 미지원 트레이드오프 |

> 주의: multi-thread / partition 전략은 멀티스레드 안전을 위해 `saveState=false`(restart 미지원) 입니다. 처리량과 재시작성은 트레이드오프입니다.
