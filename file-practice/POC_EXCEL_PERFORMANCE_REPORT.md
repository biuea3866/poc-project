# POC 엑셀 다운로드 성능 분석 보고서

## 요약

본 보고서는 동기 및 비동기 엑셀 다운로드 구현에 대한 종합적인 성능 비교 결과를 제시합니다. 테스트는 k6 부하 테스트 도구를 사용하여 7분 동안 10~100명의 동시 사용자 부하로 수행되었습니다.

**주요 발견사항:**
- 두 API 모두 5,890개 이상의 요청을 0% 오류율로 처리
- 서버 측 처리에서 비동기 API가 더 높은 CPU 효율성 확보 (CPU 시간 33% 감소)
- 클라이언트 측 레이턴시는 동기 API가 약간 더 빠름
- 두 API 모두 스트리밍을 통해 우수한 메모리 효율성 유지

---

## 테스트 설정

### 테스트 환경
- **테스트 시간**: 7분
- **총 요청 수**: 11,780건 (동기 5,890 + 비동기 5,890)
- **부하 패턴**: 6단계에 걸쳐 10 VUs에서 100 VUs로 증가
- **데이터 크기**: 요청당 100개 레코드
- **워크북 타입**: FULL (6개 시트: Product, Order, User, Category, Review, Shipment)
- **요청 간 대기 시간**: 2초

### 테스트 단계
1. **워밍업** (30초): 10 VUs
2. **50명으로 증가** (1분): 10 → 50 VUs
3. **50명 유지** (2분): 50 VUs
4. **100명으로 증가** (1분): 50 → 100 VUs
5. **100명 유지** (2분): 100 VUs
6. **감소** (30초): 100 → 0 VUs

---

## 성능 지표 비교

### 1. 클라이언트 측 레이턴시 (k6 측정)

**동기 API:**
| 지표 | 값 |
|--------|-------|
| 평균 | 206.90 ms |
| 최소 | 36.05 ms |
| 최대 | 1,701.13 ms |
| P50 (중앙값) | 187.75 ms |
| P95 | 379.88 ms |
| P99 | 521.60 ms |
| **총 요청 수** | **5,890** |

**비동기 API:**
| 지표 | 값 |
|--------|-------|
| 평균 | 224.58 ms |
| 최소 | 37.70 ms |
| 최대 | 1,558.62 ms |
| P50 (중앙값) | 207.90 ms |
| P95 | 401.84 ms |
| P99 | 562.36 ms |
| **총 요청 수** | **5,890** |

**분석:**
- 동기 API가 평균 **8.5% 더 빠름** (206.90ms vs 224.58ms)
- 두 API 모두 우수한 P95 성능 (<400ms)
- 두 API 모두 임계값 요구사항 충족 (동기 <5s, 비동기 <3s at P95)

---

### 2. 서버 측 처리 시간

**동기 API:**
| 지표 | 값 |
|--------|-------|
| 평균 응답 시간 | 151.90 ms |
| 최소 | 36 ms |
| 최대 | 754 ms |
| 총 요청 수 | 5,891 |

**비동기 API:**
| 지표 | 값 |
|--------|-------|
| 평균 응답 시간 | 160.64 ms |
| 최소 | 28 ms |
| 최대 | 835 ms |
| 총 요청 수 | 5,890 |

**분석:**
- 서버 측 처리 시간은 매우 유사 (5.8% 차이)
- 비동기는 최대 부하에서 약간 더 높은 분산 표시
- 두 API 모두 100 VU 부하에서도 1초 미만 응답 시간 달성

---

### 3. CPU 사용량 비교

**동기 API:**
| 지표 | 값 |
|--------|-------|
| 평균 CPU 시간 | 26.47 ms |
| 최소 CPU 시간 | 20 ms |
| 최대 CPU 시간 | 547 ms |
| **평균 CPU 사용률** | **20.92%** |
| CPU 사용률 범위 | 4.8% - 72.55% |

**비동기 API:**
| 지표 | 값 |
|--------|-------|
| 평균 CPU 시간 | 17.75 ms |
| 최소 CPU 시간 | 14 ms |
| 최대 CPU 시간 | 27 ms |
| **평균 CPU 사용률** | **13.34%** |
| CPU 사용률 범위 | 2.75% - 53.57% |

**분석:**
- 비동기 API가 요청당 **CPU 시간 33% 감소** (17.75ms vs 26.47ms)
- 비동기 API가 **평균 CPU 사용률 36% 감소** (13.34% vs 20.92%)
- 비동기가 더 일관된 CPU 시간 표시 (14-27ms) vs 동기 (20-547ms)
- **멀티코어 효율성**: 비동기가 CPU 코어 전반에 걸쳐 작업 부하를 더 잘 분산

**핵심 인사이트:** 비동기 구현의 코루틴과 Mutex 기반 병렬화는 스레드 안정성을 유지하면서 우수한 CPU 효율성을 제공합니다.

---

### 4. 메모리 사용량

**동기 API:**
- 요청당 평균 메모리 사용량: ~4.17 MB
- 시트 작성 중 피크 메모리: 23-26 MB
- GC 후 메모리: 효율적으로 회수됨 (기준선으로 복귀)

**비동기 API:**
- 요청당 평균 메모리 사용량: ~5.60 MB
- 시트 작성 중 피크 메모리: 16-28 MB
- GC 후 메모리: 효율적으로 회수됨 (기준선으로 복귀)

**분석:**
- 두 구현 모두 **SXSSFWorkbook 스트리밍**을 효과적으로 사용
- 부하와 관계없이 메모리 사용량은 **매우 낮은 수준** 유지
- 요청 간 가비지 컬렉션이 메모리를 효율적으로 회수
- 프로덕션 사용에 적합한 피크 메모리 사용량 (<30MB)

**참고:** 메모리 지표는 JVM 가비지 컬렉션 주기로 인해 큰 변동을 보입니다. 음수 값은 GC가 요청 시작 시 측정된 기준선 이하로 메모리를 감소시켰음을 나타냅니다. 중요한 지표는 피크 메모리가 30MB 미만으로 유지되고 GC 후 기준선으로 돌아온다는 점입니다.

---

### 5. 동시 부하 시 처리량

**전체 테스트 통계:**
| 지표 | 동기 API | 비동기 API |
|--------|----------|-----------|
| 총 요청 수 | 5,891 | 5,890 |
| 성공률 | 100% | 100% |
| 오류율 | 0% | 0% |
| 평균 요청/초 | ~14 | ~14 |

**최대 부하 시 (100 VUs):**
- 두 API 모두 안정적인 성능 유지
- 최대 동시성에서도 요청 실패 없음
- 두 API 모두 정의된 모든 임계값 통과

---

## 상세 분석

### 동기 API의 장점

1. **부하 시 낮은 레이턴시**
   - 8.5% 빠른 평균 응답 시간 (206.90ms vs 224.58ms)
   - 단순한 실행 경로로 오버헤드 감소

2. **예측 가능한 동작**
   - 직관적인 순차 처리
   - 디버깅 및 추론이 더 쉬움

### 비동기 API의 장점

1. **우수한 CPU 효율성**
   - 요청당 CPU 시간 33% 감소
   - 평균 CPU 사용률 36% 감소
   - 코루틴을 통한 멀티코어 활용 개선

2. **향상된 리소스 활용**
   - 논블로킹 작업으로 스레드 해제
   - 동일한 리소스로 더 많은 동시 요청 처리 가능
   - 더 일관된 CPU 시간 (낮은 분산)

3. **확장성**
   - Mutex 기반 병렬화로 안전한 동시 시트 생성 가능
   - 높은 동시성 시나리오에 더 적합
   - 낮은 CPU 사용량은 추가 부하를 위한 여유 공간 확보

---

## 적용된 성능 최적화 기법

### 1. 스트리밍 아키텍처
- **SXSSFWorkbook** 100행 윈도우 사용
- 청크 기반 처리 (청크당 100개 레코드)
- 메모리 효율적인 워크북 생성

### 2. 병렬화 전략 (비동기)
- **2단계 접근법**:
  1. 데이터 준비: `Dispatchers.Default`로 병렬 처리
  2. POI 작업: 스레드 안정성 보장을 위한 Mutex 동기화
- Apache POI 스레드 안정성을 유지하면서 병렬성 달성

### 3. 성능 튜닝
- 워크북 압축 비활성화 (`isCompressTempFiles = false`)
- 최적화된 청크 크기 (100개 레코드)
- 효율적인 코루틴 디스패처 사용

---

## 권장사항

### 동기 API 사용 권장 상황:
- 레이턴시가 주요 관심사인 경우
- 단순한 순차 처리 선호
- 낮은 개발 복잡도 요구
- 예측 가능하고 적당한 부하 (<50명 동시 사용자)

### 비동기 API 사용 권장 상황:
- CPU 효율성이 중요한 경우 (비용 최적화)
- 높은 동시성 예상 (>50명 동시 사용자)
- 시스템 확장성이 우선순위
- 여러 엑셀 다운로드가 병렬로 실행되는 경우
- 리소스 제약 환경에서 실행

### 프로덕션 권장사항:
**비동기 API를 프로덕션에 배포할 것을 권장합니다:**
1. **향상된 리소스 활용**: CPU 사용량 33% 감소로 더 많은 요청 처리 가능
2. **비용 효율성**: 낮은 CPU 소비로 인프라 비용 절감
3. **확장성**: 트래픽 급증 처리에 더 유리
4. **현대적 아키텍처**: 논블로킹 I/O는 반응형 프로그래밍 원칙과 부합

파일 다운로드 작업에서 8.5%의 레이턴시 차이(평균 18ms)는 무시할 수 있는 수준이며, 33%의 CPU 절감은 장기적으로 상당한 이점을 제공합니다.

---

## 임계값 준수

### 정의된 임계값:
- 동기 API: `http_req_duration{api:sync} p(95)<5000ms` ✅ **통과** (379.88ms)
- 비동기 API: `http_req_duration{api:async} p(95)<3000ms` ✅ **통과** (401.84ms)
- 오류율: `http_req_failed rate<0.1` ✅ **통과** (0%)

모든 임계값 성공적으로 통과.

---

## 기술 구현 상세

### 동기 구현
```kotlin
fun writeSync(outputStream: OutputStream, downloadableData: DownloadableData, workbookType: WorkbookType) {
    val workbook = SXSSFWorkbook(100).apply { isCompressTempFiles = false }
    val excelDataMap = excelDataFetcher.fetchMapBy(downloadableData, sheetTypes)

    excelSheetIntegrator.composeSync(workbook, excelDataMap, sheetTypes)
    workbook.write(outputStream)
    workbook.close()
}
```

### 비동기 구현
```kotlin
suspend fun write(outputStream: OutputStream, downloadableData: DownloadableData, workbookType: WorkbookType) {
    val workbook = SXSSFWorkbook(100).apply { isCompressTempFiles = false }
    val excelDataMap = excelDataFetcher.fetchMapBy(downloadableData, sheetTypes)

    excelSheetIntegrator.composeAsync(workbook, excelDataMap, sheetTypes) // Coroutines + Mutex
    workbook.write(outputStream)
    workbook.close()
}
```

### 주요 차이점: 시트 통합

**비동기는 스레드 안전 병렬화를 위해 Mutex 사용:**
```kotlin
suspend fun composeAsync(workbook: SXSSFWorkbook, excelDataMap: Map<SheetType, Sequence<ExcelData>>, sheetTypes: List<SheetType>) {
    withContext(Dispatchers.IO) {
        // 1단계: 병렬 데이터 준비
        val sheetDataMap = ConcurrentHashMap<SheetType, List<List<ExcelData>>>()
        sheets.map { async(Dispatchers.Default) { /* 데이터 준비 */ } }.awaitAll()

        // 2단계: Mutex 동기화 POI 작업 + 병렬 데이터 쓰기
        sheets.map { sheetData ->
            async(Dispatchers.IO) {
                val sheet = workbookMutex.withLock { sheetData.createSheet(workbook) }
                sheetData.writeData(sheet, chunkedData, currentRow) // 병렬 쓰기
            }
        }.awaitAll()
    }
}
```

---

## 결론

두 구현 모두 우수한 성능 특성을 보여줍니다:

- 11,780개 요청에 걸쳐 **100% 성공률**
- 100 VU 부하에서도 **1초 미만 응답 시간**
- 스트리밍 아키텍처를 통한 **효율적인 메모리 사용**
- 전체 테스트 기간 동안 **오류 없음**

**비동기 구현을 프로덕션에 권장합니다.** 우수한 CPU 효율성(CPU 시간 33% 감소), 향상된 확장성, 그리고 현대적인 반응형 아키텍처가 그 이유입니다. 약간의 레이턴시 증가(8.5%)는 상당한 리소스 절감과 개선된 동시성 처리를 고려하면 수용 가능한 수준입니다.

---

## 테스트 산출물

- **부하 테스트 스크립트**: `k6-scripts/simple-comparison-test.js`
- **테스트 결과**: `k6-results/simple-test-results.json`
- **애플리케이션 로그**: `/tmp/spring-boot.log`
- **보고서 생성일**: 2025-11-30

---

## 부록: 샘플 로그 출력

### 동기 API
```
[동기] 시작 - 메모리: 30MB, CPU: 6.18%
[동기] 데이터 준비 완료 - 메모리: 30MB (+0MB), CPU: 10.96%
[동기] 시트 작성 완료 - 메모리: 36MB (+6MB), CPU: 9.99%
[동기] 파일 쓰기 완료 - 메모리: 41MB (+5MB), CPU: 9.76%
[동기] 완료 - 소요시간: 63ms, CPU 시간: 21ms, 평균 CPU: 33.33%, 총 메모리 사용: 0MB
```

### 비동기 API
```
[비동기] 시작 - 메모리: 30MB, CPU: 2.00%
[비동기] 데이터 준비 완료 - 메모리: 30MB (+0MB), CPU: 10.54%
[비동기] 시트 작성 완료 - 메모리: 36MB (+6MB), CPU: 16.92%
[비동기] 파일 쓰기 완료 - 메모리: 41MB (+5MB), CPU: 9.68%
[비동기] 완료 - 소요시간: 62ms, CPU 시간: 15ms, 평균 CPU: 24.19%, 총 메모리 사용: 0MB
```
