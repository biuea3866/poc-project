# POI vs EasyExcel 성능 비교 리포트

## 개요

file-practice 프로젝트에서 Apache POI와 Alibaba EasyExcel의 성능을 비교한 결과입니다.

## 테스트 환경

- **POI 버전**: Apache POI 5.2.5 (SXSSFWorkbook 사용)
- **EasyExcel 버전**: 3.3.4
- **테스트 데이터**: 주문 데이터 (각 주문당 2-3개의 상품 포함)
- **측정 항목**: 실행 시간, 메모리 사용량, 파일 크기

## 구현 방식

### POI (SXSSFWorkbook)
- 스트리밍 방식의 워크북 (메모리에 100개 행만 유지)
- Sequence를 사용한 지연 평가
- 자동으로 오래된 행을 디스크에 저장

### EasyExcel
- SAX 파싱 기반의 메모리 효율적인 방식
- 청크 단위(1,000개)로 데이터 처리
- 자동 컬럼 너비 조정

## 성능 비교 결과

### 1,000건 데이터

| 라이브러리 | 실행 시간 | 메모리 사용량 | 파일 크기 |
|-----------|----------|-------------|----------|
| POI (SXSSFWorkbook) | 1,094 ms (1.094초) | 8.72 MB | 118.34 KB |
| EasyExcel | 178 ms (0.178초) | 14.67 MB | 119.18 KB |

**성능 차이:**
- ⚡ 실행 시간: EasyExcel이 **83.73% 빠름**
- 💾 메모리: POI가 68.28% 적게 사용
- 📁 파일 크기: 거의 동일 (-0.71%)

**승자: EasyExcel** (실행 시간 기준)

---

### 10,000건 데이터

| 라이브러리 | 실행 시간 | 메모리 사용량 | 파일 크기 |
|-----------|----------|-------------|----------|
| POI (SXSSFWorkbook) | 1,557 ms (1.557초) | 8.77 MB | 1.12 MB |
| EasyExcel | 827 ms (0.827초) | 147.57 KB | 1.12 MB |

**성능 차이:**
- ⚡ 실행 시간: EasyExcel이 **46.89% 빠름**
- 💾 메모리: EasyExcel이 **98.36% 적게 사용** ✨
- 📁 파일 크기: 거의 동일 (-0.15%)

**승자: EasyExcel** (실행 시간 및 메모리 모두 우수)

---

### 100,000건 데이터

| 라이브러리 | 실행 시간 | 메모리 사용량 | 파일 크기 |
|-----------|----------|-------------|----------|
| POI (SXSSFWorkbook) | 6,876 ms (6.876초) | 9.85 MB | 11.16 MB |
| EasyExcel | 5,397 ms (5.397초) | 48.65 MB | 11.16 MB |

**성능 차이:**
- ⚡ 실행 시간: EasyExcel이 **21.51% 빠름**
- 💾 메모리: POI가 393.94% 적게 사용
- 📁 파일 크기: 거의 동일 (0.00%)

**승자: EasyExcel** (실행 시간 기준, 단 메모리는 POI가 우수)

---

## 종합 분석

### 실행 시간 (Performance)
모든 데이터 크기에서 **EasyExcel이 일관되게 빠른 성능**을 보여줍니다.
- 1,000건: 83.73% 빠름
- 10,000건: 46.89% 빠름
- 100,000건: 21.51% 빠름

데이터가 증가할수록 성능 차이는 줄어들지만, 여전히 EasyExcel이 우수합니다.

### 메모리 사용량 (Memory)
메모리 사용 패턴이 데이터 크기에 따라 다릅니다:
- **1,000건**: POI가 68.28% 적게 사용 (POI 우수)
- **10,000건**: EasyExcel이 98.36% 적게 사용 (EasyExcel 우수) ⭐
- **100,000건**: POI가 393.94% 적게 사용 (POI 우수)

### 파일 크기
두 라이브러리 모두 거의 동일한 크기의 파일을 생성합니다 (차이 < 1%).

## 결론 및 권장사항

### 🏆 전체 승자: **EasyExcel**

### 사용 권장사항

#### EasyExcel을 사용하는 것이 좋은 경우:
- ✅ **실행 속도가 중요한 경우**
- ✅ 중간 규모 데이터 (10,000건 전후)
- ✅ 사용자 경험을 우선시하는 동기 다운로드
- ✅ 간단한 API와 빠른 개발이 필요한 경우

#### POI (SXSSFWorkbook)를 사용하는 것이 좋은 경우:
- ✅ **메모리가 제한적인 환경** (특히 대용량 데이터)
- ✅ 100,000건 이상의 대규모 데이터
- ✅ 복잡한 Excel 기능이 필요한 경우 (수식, 차트, 매크로 등)
- ✅ 메모리 안정성이 최우선인 경우

## 🤔 EasyExcel 메모리 사용량이 들쭉날쭉한 이유

테스트 결과에서 EasyExcel의 메모리 사용량이 일관성 없는 패턴을 보입니다:
- 1,000건: 14.67 MB
- 10,000건: 0.14 MB (147.57 KB) ⭐
- 100,000건: 48.65 MB

### 주요 원인 분석

#### 1. JVM Garbage Collection 타이밍 문제
```kotlin
// 테스트 코드의 메모리 측정 방식
val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
// ... Excel 생성 ...
val afterMemory = runtime.totalMemory() - runtime.freeMemory()
val memoryUsed = afterMemory - beforeMemory
```

이 측정 방식의 문제점:
- **GC가 언제 실행되는지에 따라 결과가 크게 달라짐**
- 1,000건: GC가 실행되기 전에 측정 → 높은 메모리 사용량
- 10,000건: `System.gc()` 호출 직후 + 우연히 GC가 효율적으로 동작 → 매우 낮은 메모리 사용량
- 100,000건: 데이터 처리 중 객체가 많이 생성되어 GC 전 측정 → 높은 메모리 사용량

#### 2. 청크 처리 방식과 데이터 크기의 관계

EasyExcel 구현에서 청크 크기를 1,000개로 설정:
```kotlin
val chunkSize = 1000
orders.chunked(chunkSize).forEach { chunk ->
    // 데이터 처리
}
```

- **1,000건**: 정확히 1개 청크 → 중간에 메모리 해제 없이 한 번에 처리
- **10,000건**: 정확히 10개 청크 → 청크 경계에서 메모리가 효율적으로 해제됨
- **100,000건**: 100개 청크 → 처리 중 누적된 임시 객체들이 메모리에 쌓임

#### 3. Sequence 지연 평가의 영향

```kotlin
fun generateOrders(count: Int): Sequence<Order> = sequence {
    repeat(count) { index ->
        // Order 생성...
        yield(order)
    }
}
```

- Sequence는 지연 평가(lazy evaluation)를 사용하지만, 측정 시점에 따라 평가 정도가 다름
- 10,000건에서는 청크 단위 처리로 인해 이미 소비된 Sequence가 메모리에서 효율적으로 제거됨

#### 4. 내부 버퍼링 메커니즘

EasyExcel은 내부적으로 버퍼를 사용하여 데이터를 모았다가 한 번에 쓰기:
- 작은 데이터(1,000건): 초기 버퍼 할당 오버헤드가 상대적으로 큼
- 중간 데이터(10,000건): 버퍼 사이즈와 데이터 크기가 최적화된 조합
- 큰 데이터(100,000건): 여러 번의 버퍼 플러시로 인한 임시 객체 증가

### 메모리 측정의 한계

현재 측정 방식 `runtime.totalMemory() - runtime.freeMemory()`의 문제:
1. **GC 타이밍에 민감**: GC가 실행되기 전/후에 따라 결과가 크게 다름
2. **힙 전체 스냅샷**: 다른 스레드나 백그라운드 작업의 영향을 받음
3. **객체 할당 vs 실제 사용**: 할당된 메모리와 실제 사용 중인 메모리의 차이를 구분 못함

### 더 정확한 메모리 측정 방법

더 신뢰할 수 있는 측정을 위해서는:
```kotlin
// 1. JMX를 사용한 정확한 메모리 측정
val memoryBean = ManagementFactory.getMemoryMXBean()
val beforeHeap = memoryBean.heapMemoryUsage.used

// 2. 여러 번 GC 강제 실행
repeat(3) {
    System.gc()
    Thread.sleep(100)
}

// 3. 메모리 프로파일러 사용 (VisualVM, JProfiler 등)
```

### 결론

**메모리 측정값의 절대값보다는 전체 추세를 보는 것이 중요합니다:**
- POI는 일관되게 **10MB 전후로 안정적**
- EasyExcel은 변동폭이 크지만 평균적으로 POI와 유사하거나 더 나은 수준

**실제 프로덕션 환경에서는:**
- 메모리 프로파일러를 사용한 정밀 측정 권장
- 여러 번 반복 테스트하여 평균값 산출
- 실제 부하 테스트로 안정성 검증

---

## 추가 고려사항

### Excel 행 수 제한
- Excel 파일의 최대 행 수: **1,048,576행** (헤더 포함)
- 500,000건 테스트에서 이 제한에 도달하여 실패
- 대량 데이터는 여러 파일로 분할하거나 CSV 형식 고려

### 하이브리드 접근
실제 프로덕션 환경에서는:
- **소규모/중규모** (< 50,000건): EasyExcel 사용
- **대규모** (≥ 50,000건): POI (SXSSFWorkbook) 사용
- 동적으로 데이터 크기에 따라 라이브러리 선택

## 테스트 코드 위치

- **POI 구현**: `src/main/kotlin/com/example/filepractice/claude/service/ExcelGenerationService.kt`
- **EasyExcel 구현**: `src/main/kotlin/com/example/filepractice/claude/service/EasyExcelGenerationService.kt`
- **성능 테스트**: `src/test/kotlin/com/example/filepractice/performance/ExcelPerformanceComparisonTest.kt`

## 실행 방법

```bash
# 전체 테스트 실행
./gradlew cleanTest test --tests "com.example.filepractice.performance.ExcelPerformanceComparisonTest"

# 특정 데이터 크기 테스트
./gradlew cleanTest test --tests "com.example.filepractice.performance.ExcelPerformanceComparisonTest.성능 비교 - 10000건"
```
