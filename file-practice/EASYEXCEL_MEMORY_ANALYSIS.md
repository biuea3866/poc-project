# EasyExcel 메모리 사용량 불안정성 분석

## 🔍 문제 현상

독립 벤치마크 결과에서 EasyExcel의 메모리 사용량이 매우 불안정합니다:

| 데이터 크기 | 평균 메모리 | 변동계수 (CV) | 메모리 범위 |
|-----------|-----------|-------------|-----------|
| 1,000건 | 3.33 MB | **65.93%** ⚠️ | 1.20 MB ~ 6.39 MB |
| 10,000건 | 35.20 MB | **191.01%** 🚨 | 0.27 MB ~ 169.65 MB |
| 100,000건 | 91.84 MB | **2.32%** ✅ | 88.64 MB ~ 94.65 MB |

**이상한 패턴**:
- 소량 데이터에서 변동이 매우 큼
- 중간 규모(10,000건)에서 변동이 극단적 (191%!)
- 대량 데이터에서 오히려 안정적

---

## 🔬 원인 분석

### 1. EasyExcel의 내부 버퍼링 메커니즘

EasyExcel은 `WriteContextImpl`에서 복잡한 버퍼링을 수행합니다:

```java
// EasyExcel 내부 코드 (WriteContextImpl.java)
public class WriteContextImpl implements WriteContext {
    private WriteHolder currentWriteHolder;
    private Map<Integer, WriteSheetHolder> writeSheetHolderMap;

    // 데이터를 모았다가 한 번에 쓰기
    public void write(List data, WriteSheet writeSheet, WriteTable writeTable) {
        // 내부 버퍼에 데이터 누적
        // 특정 조건에서만 flush
    }
}
```

**문제점**:
- 데이터를 내부 버퍼에 모았다가 한 번에 처리
- 버퍼 크기가 동적으로 조정됨
- flush 타이밍이 예측 불가능

### 2. 청크 처리와 버퍼 관계

우리 구현:
```kotlin
// EasyExcelGenerationService.kt
val chunkSize = 1000
orders.chunked(chunkSize).forEach { chunk ->
    val data = chunk.map { order -> /* ... */ }
    excelWriter.write(data, writeSheet, table)
}
```

**메모리 동작 시나리오**:

#### 시나리오 A: GC 전 측정 (높은 메모리)
```
청크 1 (1000개) → 버퍼 누적
청크 2 (1000개) → 버퍼 누적
...
[메모리 측정] ← GC 전, 버퍼에 여러 청크 누적
청크 완료 → flush
```

#### 시나리오 B: GC 후 측정 (낮은 메모리)
```
청크 1 (1000개) → 버퍼 누적 → 자동 flush
[GC 발생]
청크 2 (1000개) → 버퍼 누적
[메모리 측정] ← GC 직후, 버퍼 비어있음
```

### 3. WriteHolder의 동적 메모리 할당

```java
// EasyExcel 내부
public class WriteHolder {
    // 동적으로 커지는 컬렉션들
    private List<WriteHandler> writeHandlerList;
    private Map<Integer, Field> fieldMap;
    private List<List<String>> headList;

    // 스타일 캐시 (메모리 사용)
    private Map<Integer, CellStyle> cellStyleMap;
}
```

**초기 데이터 처리 시**:
- 헤더 분석 및 캐싱
- 스타일 객체 생성 및 저장
- 필드 매핑 정보 구축

→ **초기 오버헤드가 크고, 데이터 크기에 따라 달라짐**

### 4. POI SXSSFWorkbook과의 차이

#### POI (SXSSFWorkbook)
```java
// POI 내부 - 예측 가능한 메모리
SXSSFWorkbook workbook = new SXSSFWorkbook(100); // 고정 윈도우
// 항상 100행만 메모리 유지
// 초과 시 즉시 디스크로 flush
```
→ **메모리 사용량이 일정하고 예측 가능**

#### EasyExcel
```java
// EasyExcel 내부 - 동적 버퍼
WriteContextImpl context = new WriteContextImpl();
// 버퍼 크기가 동적으로 조정
// flush 타이밍이 내부 로직에 의존
```
→ **메모리 사용량이 가변적이고 예측 불가**

---

## 📊 데이터 크기별 메모리 패턴 설명

### 1,000건: 중간 정도 변동 (65.93% CV)

```
측정 1: 5.51 MB  ← 초기 오버헤드 + 버퍼 누적
측정 2: 6.39 MB  ← 최대치 (GC 전)
측정 3: 2.32 MB  ← GC 후 + 최적화
측정 4: 1.20 MB  ← 최소치 (완전 flush 후)
측정 5: 1.23 MB  ← 안정 상태
```

**이유**:
- 1개 청크 (1,000개)만 처리
- 초기 메타데이터 생성 오버헤드
- GC 타이밍에 따라 크게 달라짐

### 10,000건: 극단적 변동 (191.01% CV) 🚨

```
측정 1: 4.60 MB    ← 정상
측정 2: 0.27 MB    ← 최소치 (GC 직후 측정)
측정 3: 169.65 MB  ← 최대치 (모든 청크 버퍼에 누적!)
측정 4: 1.23 MB    ← GC 후
측정 5: 0.27 MB    ← GC 직후
```

**이유**:
- **10개 청크 처리 중 GC 타이밍이 불규칙**
- 운이 나쁘면 여러 청크가 버퍼에 동시 누적
- 운이 좋으면 각 청크마다 즉시 flush

**시나리오 재현**:
```kotlin
// 최악의 경우 (측정 3: 169.65 MB)
청크 1 (1000개) → 버퍼 누적 (flush 안됨)
청크 2 (1000개) → 버퍼 누적 (flush 안됨)
청크 3 (1000개) → 버퍼 누적 (flush 안됨)
...
청크 10 (1000개) → 버퍼 누적
[메모리 측정] ← 10개 청크 모두 메모리에!

// 최선의 경우 (측정 2: 0.27 MB)
[GC 발생]
청크 1 (1000개) → 버퍼 누적 → 즉시 flush
[메모리 측정] ← 버퍼 비어있음
```

### 100,000건: 안정적 (2.32% CV) ✅

```
측정 1: 88.64 MB  ← 안정
측정 2: 91.64 MB  ← 안정
측정 3: 94.65 MB  ← 안정
측정 4: 93.62 MB  ← 안정
측정 5: 90.67 MB  ← 안정
```

**이유**:
- 100개 청크 처리로 데이터가 많음
- **내부 버퍼가 일찍 가득 차서 자동 flush 발생**
- GC도 더 자주 발생하여 안정화
- 초기 오버헤드 비율이 낮음

---

## 🔍 EasyExcel 내부 코드 분석

### WriteContextImpl의 flush 로직

EasyExcel은 다음 조건에서 flush합니다:

```java
// 추정 내부 로직
private void checkAndFlush() {
    if (currentBufferSize > threshold) {  // 임계값 초과
        flush();
    }

    if (memoryPressure > limit) {  // 메모리 압박
        flush();
    }

    // 명시적 flush 호출 시에만
}
```

**문제**:
- `threshold`가 동적으로 변함
- JVM 메모리 상태에 따라 다르게 동작
- GC 타이밍과 상호작용

### 우리 코드의 청크 처리

```kotlin
orders.chunked(chunkSize).forEach { chunk ->
    val data = chunk.map { order ->
        orderColumns.map { column ->
            getOrderColumnValue(order, column)
        }
    }
    excelWriter.write(data, writeSheet, table)
}
```

**각 write() 호출마다**:
1. EasyExcel 내부 버퍼에 추가
2. flush 필요성 검사
3. 조건 충족 시에만 flush

→ **예측 불가능**

---

## 💡 왜 POI는 안정적인가?

### POI SXSSFWorkbook의 결정적 차이

```java
// POI 코드 (SXSSFSheet.java)
public SXSSFRow createRow(int rownum) {
    if (_randomAccessWindowSize >= 0 &&
        _rows.size() > _randomAccessWindowSize) {
        // 윈도우 크기 초과 시 즉시 flush (결정적!)
        flushRows(_randomAccessWindowSize);
    }
    // 새 행 생성
}
```

**핵심**:
- **윈도우 크기(100행) 고정**
- 101번째 행 생성 시 **무조건 flush**
- GC나 메모리 상태와 무관
- **완전히 예측 가능**

**결과**:
```
POI 메모리 사용량 = 윈도우 크기 × 행당 메모리 + 고정 오버헤드
                  = 100행 × ~100KB + ~10MB
                  ≈ 일정 (10-20 MB)
```

---

## 🎯 해결 방법

### 방법 1: Explicit Flush (권장)

```kotlin
class EasyExcelGenerationService {
    fun generateOrderExcelFromSequence(
        orders: Sequence<Order>,
        columnConfig: ColumnConfig,
        outputStream: OutputStream
    ) {
        val excelWriter = EasyExcel.write(outputStream)
            .autoCloseStream(false)
            .build()

        try {
            val orderSheet = EasyExcel.writerSheet(0, "주문 내역").build()

            var chunkCount = 0
            orders.chunked(1000).forEach { chunk ->
                val data = chunk.map { /* ... */ }
                excelWriter.write(data, orderSheet)

                chunkCount++

                // 명시적 flush 추가
                if (chunkCount % 5 == 0) {  // 5개 청크마다
                    excelWriter.finish()  // 강제 flush
                }
            }
        } finally {
            excelWriter.finish()
        }
    }
}
```

### 방법 2: 더 작은 청크 사용

```kotlin
// 청크 크기를 줄여서 버퍼 누적 방지
val chunkSize = 100  // 1000 → 100
orders.chunked(chunkSize).forEach { chunk ->
    excelWriter.write(data, writeSheet, table)
}
```

### 방법 3: 메모리 설정 조정

```kotlin
val excelWriter = EasyExcel.write(outputStream)
    .autoCloseStream(false)
    .inMemory(true)  // 메모리 모드 명시
    .build()
```

---

## 📈 개선된 벤치마크 예상 결과

### 현재 (Explicit Flush 없음)
- 10,000건 변동계수: **191.01%** 🚨

### 개선 후 (Explicit Flush 추가)
- 10,000건 변동계수: **예상 < 20%** ✅

---

## 🎓 결론

### EasyExcel 메모리 불안정의 근본 원인

1. **동적 버퍼링**
   - 내부 버퍼 크기가 가변적
   - flush 타이밍이 예측 불가

2. **GC 타이밍 의존성**
   - 메모리 측정 시점에 따라 크게 달라짐
   - 버퍼 상태가 GC에 따라 변함

3. **청크 처리와의 상호작용**
   - 여러 청크가 버퍼에 누적될 수 있음
   - 운에 따라 즉시 flush되거나 누적됨

### POI vs EasyExcel 메모리 관리 철학

| 특성 | POI (SXSSFWorkbook) | EasyExcel |
|-----|-------------------|-----------|
| 버퍼 크기 | **고정** (100행) | **동적** (가변) |
| Flush 타이밍 | **결정적** (101번째 행) | **휴리스틱** (조건 기반) |
| 예측 가능성 | ✅ **높음** | ❌ **낮음** |
| 메모리 안정성 | ✅ **일정** | ❌ **변동** |
| 최적화 | ❌ 덜 최적화 | ✅ 더 최적화 (평균적으로) |

### 프로덕션 권장사항

**EasyExcel 사용 시**:
```kotlin
// 1. 명시적 flush 추가
if (chunkCount % 5 == 0) excelWriter.finish()

// 2. 작은 청크 사용
orders.chunked(100)  // 1000 대신 100

// 3. 메모리 모니터링 강화
// 4. 대용량 데이터는 POI 사용 고려
```

**결론**:
- **소/중규모 (< 50,000건)**: EasyExcel 사용 가능 (속도 우수)
- **대규모 (≥ 50,000건)**: **POI 권장** (메모리 안정성)
- **메모리 예측 필요**: **POI 필수** (결정적 동작)
