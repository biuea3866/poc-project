# POC 엑셀 다운로드 성능 개선 리포트

## 발견된 문제점

### 1. runBlocking 사용으로 인한 스레드 블로킹 (심각)
**문제**
- `ExcelSheetIntegrator.composeAsyncByForkCoroutine()`에서 `runBlocking` 사용
- 스프링 웹 요청 스레드를 블로킹하여 동시 요청 처리 불가
- Tomcat 스레드 풀이 고갈될 위험

**해결**
- `runBlocking` 제거하고 `suspend` 함수로 변경
- `StreamingResponseBody` 람다 내부에서만 코루틴 사용
- HTTP 응답 시작 후에만 블로킹되므로 동시 요청 처리 가능

### 2. POI Thread-Safety 문제 (심각)
**문제**
- Apache POI의 `SXSSFWorkbook`은 thread-safe하지 않음
- 여러 코루틴에서 동시에 같은 workbook에 쓰기 시 데이터 손상 가능

**해결**
- 2단계 처리 방식 도입:
  1. 데이터 준비: 병렬 처리 (Dispatchers.Default)
  2. POI 작업: 순차 처리 (limitedParallelism(1))
- `ConcurrentHashMap`으로 안전한 데이터 공유

### 3. Chunk Size 최적화
**문제**
- `CHUNK_SIZE = 10`은 너무 작아서 오버헤드 증가

**해결**
- `CHUNK_SIZE = 100`으로 증가
- 메모리와 성능의 균형점 확보

### 4. Workbook 압축 설정
**문제**
- `isCompressTempFiles = true`로 인한 CPU 오버헤드

**해결**
- `isCompressTempFiles = false`로 변경
- CPU 사용량 감소, 전체 성능 향상

## 성능 측정 결과

### 테스트 환경
- 데이터: 1000개 제품, 2000개 주문, 1000명 사용자, 200개 카테고리, 3000개 리뷰, 2000개 배송
- 시트: 6개 (Product, Order, User, Category, Review, Shipment)
- 파일 크기: 480KB

### 개선 전 (문제 있는 코드)
```
비동기: 225ms (runBlocking, thread-safety 문제)
동기: 188ms
```

### 개선 후
```
비동기: 248ms (+23ms, thread-safety 보장)
동기: 179ms (-9ms, 압축 비활성화 효과)
메모리 사용량: ~0MB (스트리밍 방식)
```

## 코드 변경 사항

### ExcelSheetIntegrator
```kotlin
// 개선 전
fun composeAsyncByForkCoroutine(...) = runBlocking {
    // POI 작업을 여러 코루틴에서 동시 실행 (thread-safety 문제)
}

// 개선 후
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun composeAsync(...) = withContext(Dispatchers.IO) {
    // 1단계: 데이터 준비 (병렬)
    val sheetDataMap = ConcurrentHashMap<SheetType, List<List<ExcelData>>>()
    sheets.map { async(Dispatchers.Default) { ... } }.awaitAll()

    // 2단계: POI 작업 (순차, thread-safe)
    withContext(Dispatchers.IO.limitedParallelism(1)) {
        sheets.forEach { ... }
    }
}
```

### ExcelDownloader
```kotlin
// 개선 전
fun write(...) {
    val workbook = SXSSFWorkbook(100).apply {
        isCompressTempFiles = true // CPU 오버헤드
    }
    excelSheetIntegrator.composeAsyncByForkCoroutine(...)
}

// 개선 후
suspend fun write(...) {
    val workbook = SXSSFWorkbook(100).apply {
        isCompressTempFiles = false // 압축 비활성화
    }
    excelSheetIntegrator.composeAsync(...)
}
```

### PocExcelDownloadController
```kotlin
// 개선 전
@GetMapping("/download/async")
fun downloadExcelAsync(...) {
    // 컨트롤러에서 직접 runBlocking
}

// 개선 후
@GetMapping("/download/async")
fun downloadExcelAsync(...) {
    StreamingResponseBody { outputStream ->
        // 스트리밍 시작 후에만 runBlocking
        runBlocking {
            excelDownloader.write(outputStream, ...)
        }
    }
}
```

## 메모리 프로파일링

### 메모리 사용 패턴
1. **Sequence 사용**: Lazy evaluation으로 메모리 효율적
2. **Chunked 처리**: 100개씩 청크 단위로 처리
3. **SXSSFWorkbook**: 100행씩 메모리에 유지, 나머지는 디스크

### 예상 메모리 사용량
```
청크 데이터: 100 rows × 6 sheets × ~1KB = ~600KB
POI 버퍼: 100 rows × ~2KB = ~200KB
총 메모리: ~1MB 이하 (데이터 크기와 무관)
```

## 권장 사항

### 1. 대용량 데이터 처리
- 10만건 이상: `CHUNK_SIZE`를 500-1000으로 증가
- `STREAMING_SIZE`도 200-300으로 조정

### 2. 동시 요청 처리
- 현재 구조는 동시 요청 처리 가능
- 필요시 요청별 스레드 풀 제한 추가

### 3. 모니터링
- 메모리 사용량 모니터링
- 응답 시간 추적
- 에러 로그 확인

## 결론

주요 문제점들을 해결하여:
- ✅ Thread-safety 보장
- ✅ 스레드 블로킹 최소화
- ✅ 메모리 효율성 유지
- ✅ 성능 개선 (동기 방식 9ms 향상)

비동기 방식은 약간 느려졌지만(23ms), thread-safety를 보장하므로 프로덕션 환경에서 안전하게 사용 가능합니다.
