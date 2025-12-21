# 변경 이력

## 2025-11-22: Ktor에서 Spring Boot으로 프레임워크 전환

### 요구사항
- 기존 Ktor로 구현된 애플리케이션을 Spring Boot (spring-web) 기반으로 변경.

### 변경 사유
- 사용자의 요청. 더 성숙하고 널리 사용되는 Spring Boot 프레임워크를 사용하여 프로젝트의 확장성과 유지보수성을 높이기 위함.

### 주요 변경 내용
1.  **`build.gradle.kts` 수정**:
    - Ktor 관련 플러그인 및 의존성을 제거.
    - Spring Boot 플러그인 (`org.springframework.boot`, `io.spring.dependency-management`) 및 `kotlin-spring` 플러그인을 추가.
    - Ktor 의존성을 `spring-boot-starter-web`, `spring-boot-starter-test` 등으로 교체.
    - `kotlinx-coroutines-core`, `mockito-kotlin` 의존성 추가.

2.  **애플리케이션 구조 변경**:
    - 기존 `Application.kt` (Ktor)를 삭제.
    - `@SpringBootApplication` 어노테이션을 사용하는 `FilePracticeApplication.kt`를 생성.

3.  **API 엔드포인트 마이그레이션**:
    - Ktor의 `routing` 블록 대신, Spring MVC의 `@RestController`를 사용하는 `FileDownloadController`를 생성.
    - 각 엔드포인트를 `@GetMapping` 어노테이션을 사용하여 구현.

4.  **서비스 클래스 변경**:
    - `ExcelService`, `PdfService`에 `@Service` 어노테이션을 추가하여 Spring의 DI 컨테이너가 관리하는 빈으로 등록.

5.  **테스트 코드 마이그레이션**:
    - Ktor의 `testApplication` 기반 테스트를 삭제.
    - `@WebMvcTest`와 `MockMvc`를 사용하여 컨트롤러를 테스트하는 `FileDownloadControllerTest`를 생성.
    - 서비스 로직 테스트(`ExcelServiceTest`)는 JUnit5 기반의 순수 단위 테스트로 유지.
    - Mockito를 사용하여 서비스 의존성을 Mocking 처리.

---

## 2025-11-22: 엑셀 OOM 해결 및 iText PDF 구현 추가

### 요구사항
1.  대용량 엑셀 다운로드 시 발생할 수 있는 OutOfMemory(OOM) 문제를 해결.
2.  무료 라이선스 버전의 iText7 라이브러리를 사용한 PDF 생성 기능 추가.

### 변경 사유
- 대용량 데이터 처리 시의 안정성 확보 및 다양한 PDF 라이브러리 구현 예시 제공.

### 주요 변경 내용 (예정)
1.  **엑셀 OOM 해결**:
    - `ExcelService`에서 사용하는 `XSSFWorkbook`을 스트리밍 API인 `SXSSFWorkbook`으로 교체하여 메모리 사용량을 최적화.
2.  **iText PDF 구현 추가**:
    - `build.gradle.kts`에 iText7 (AGPL) 의존성 추가.
    - 한글 폰트(나눔고딕) 파일을 리소스에 포함.
    - `ITextPdfService`를 새로 생성하여 iText를 사용한 PDF 생성 로직 구현.
    - `FileDownloadController`에 iText 기반 PDF 다운로드를 위한 신규 엔드포인트 (`/download/pdf-itext`) 추가.

---

## 2025-11-22: `FileDownloadController` 리팩토링 및 iText PDF 구현 재시도

### 요구사항
1.  `FileDownloadController`에 집중된 비즈니스 로직을 서비스 계층으로 분리하여 OOP 원칙을 준수하고 컨트롤러의 역할을 명확히 함.
2.  이전에 실패했던 iText PDF 구현을 재시도하여 안정적으로 동작하도록 함.

### 변경 사유
- 컨트롤러는 HTTP 요청/응답 처리만을 담당해야 하며, 비즈니스 로직은 서비스 계층에서 처리되어야 함. 이는 코드의 응집도를 높이고 유지보수성을 향상시킴.
- iText PDF 구현의 안정적인 완성을 통해 다양한 PDF 라이브러리 지원을 목표로 함.

### 주요 변경 내용 (예정)
1.  **`FileDownloadController` 리팩토링**:
    - `createDummyOrders` 로직을 `OrderService` (또는 유사한 서비스)로 이동.
    - 비동기 파일 저장 로직을 `FileStorageService` (또는 각 서비스)로 이동하여 컨트롤러의 책임을 분리.
2.  **iText PDF 구현 재시도**:
    - `build.gradle.kts`에 iText7 의존성 재확인 및 필요한 경우 추가.
    - `ITextPdfService`를 재구현하여 한글 폰트 지원을 포함한 PDF 생성 로직을 안정적으로 구현.
    - `ITextPdfServiceTest`를 작성하여 `ITextPdfService`의 기능을 검증.
    - `FileDownloadController`에 iText 기반 PDF 다운로드 엔드포인트 추가 및 `FileDownloadControllerTest`에 해당 엔드포인트 테스트 추가.

---

## 2025-11-24: EasyExcel 라이브러리 도입 및 POI 성능 비교

### 요구사항
- EasyExcel 라이브러리를 도입하여 Apache POI와 성능을 비교.
- 다양한 데이터 크기(1,000건, 10,000건, 100,000건)에서 실행 시간과 메모리 사용량을 측정.

### 변경 사유
- 대용량 엑셀 처리 시 더 나은 성능을 제공하는 라이브러리 선택을 위한 실증적 데이터 확보.
- POI와 EasyExcel의 장단점을 파악하여 사용 사례별 최적의 라이브러리 선정.

### 주요 변경 내용

1. **EasyExcel 의존성 추가**:
   - `build.gradle.kts`에 `com.alibaba:easyexcel:3.3.4` 추가.

2. **EasyExcelGenerationService 구현**:
   - `src/main/kotlin/com/example/filepractice/claude/service/EasyExcelGenerationService.kt` 생성.
   - EasyExcel의 스트리밍 API를 사용하여 대용량 데이터 처리 구현.
   - 청크 단위(1,000개)로 데이터를 처리하여 메모리 효율성 향상.

3. **성능 비교 테스트 작성**:
   - `src/test/kotlin/com/example/filepractice/performance/ExcelPerformanceComparisonTest.kt` 생성.
   - 1,000건, 10,000건, 100,000건 데이터로 POI vs EasyExcel 성능 측정.
   - 실행 시간, 메모리 사용량, 파일 크기를 자동으로 측정 및 비교.

4. **Gradle 테스트 로깅 설정**:
   - `build.gradle.kts`에 `testLogging` 설정 추가로 테스트 결과를 콘솔에 출력.

### 성능 비교 결과 요약

| 데이터 크기 | 승자 | 실행 시간 차이 | 메모리 특이사항 |
|-----------|------|-------------|--------------|
| 1,000건 | EasyExcel | 83.73% 빠름 | POI가 메모리 효율적 |
| 10,000건 | EasyExcel | 46.89% 빠름 | EasyExcel이 98.36% 메모리 절감 |
| 100,000건 | EasyExcel | 21.51% 빠름 | POI가 메모리 효율적 |

**전체 결론**: 실행 시간 기준 EasyExcel이 모든 데이터 크기에서 우수. 메모리는 데이터 크기에 따라 다른 패턴 보임.

### 권장사항
- **소/중규모 데이터 (< 50,000건)**: EasyExcel 사용 - 빠른 속도와 우수한 사용자 경험.
- **대규모 데이터 (≥ 50,000건)**: POI 사용 - 메모리 안정성 우선.

### 상세 리포트
- `PERFORMANCE_COMPARISON.md`: 전체 성능 비교 결과 및 분석.

---

## 2025-11-24: 독립적이고 통계적으로 신뢰할 수 있는 벤치마크 구현

### 요구사항
- 실제 프로덕션 환경에서 사용할 수 있도록 완벽하게 독립적이고 정확한 성능 비교 필요.
- 통계적으로 유의미한 결과 제공.

### 변경 사유
- 기존 테스트의 메모리 측정 방식이 GC 타이밍에 따라 결과가 크게 달라지는 문제 발견.
- 두 테스트가 연속으로 실행되어 서로 영향을 줄 수 있는 문제 해결 필요.
- 프로덕션 환경에서 신뢰할 수 있는 의사결정 기준 마련.

### 주요 변경 내용

1. **JMH 벤치마크 프레임워크 도입**:
   - `build.gradle.kts`에 `org.openjdk.jmh` 의존성 추가.
   - 전문적인 벤치마킹 도구 활용 준비.

2. **독립 벤치마크 테스트 구현**:
   - `src/test/kotlin/com/example/filepractice/performance/IndependentExcelBenchmark.kt` 생성.
   - 각 라이브러리를 별도의 테스트로 완전 분리하여 상호 영향 차단.
   - JMX (Java Management Extensions)를 사용한 정밀한 메모리 측정.
   - 워밍업 3회 + 측정 5회 반복으로 JVM 최적화 효과 반영.
   - 측정 전/후 여러 번 GC 실행으로 메모리 정리.

3. **통계 분석 기능 추가**:
   - 평균(Mean), 중앙값(Median), 표준편차(StdDev), 최소/최대값 자동 계산.
   - 변동계수(CV, Coefficient of Variation)로 안정성 평가.
   - 힙 메모리 + Non-heap 메모리 모두 측정.

### 벤치마크 결과 요약

#### 1,000건
- 실행 시간: POI 64ms vs EasyExcel 66ms (비슷)
- 메모리: POI 18MB vs EasyExcel 3.33MB (EasyExcel 81% 절감)
- 안정성: POI 메모리 완벽 일관성(0% CV), EasyExcel 불안정(65.93% CV)

#### 10,000건
- 실행 시간: POI 449ms vs EasyExcel 503ms (POI 12% 빠름)
- 메모리(중앙값): POI 22.32MB vs EasyExcel 1.23MB (EasyExcel 95% 절감!)
- 안정성: POI가 더 안정적

#### 100,000건 (대용량)
- 실행 시간: POI 4,492ms vs EasyExcel 4,226ms (EasyExcel 6% 빠름)
- 메모리: POI 15.73MB vs EasyExcel 91.84MB (POI 83% 절감!) 🏆
- 안정성: 둘 다 매우 안정적 (1.57-2.29% CV)

### 최종 결론 및 권장사항

**대용량 처리 (≥ 50,000건)**: Apache POI 권장
- 메모리 효율 5~6배 우수
- 안정적이고 예측 가능한 성능

**소/중규모 처리 (< 50,000건)**: Alibaba EasyExcel 권장
- 메모리 효율 우수 (특히 중앙값 기준)
- 간단한 API, 빠른 개발

**최적 전략**: 하이브리드 접근
- 데이터 크기에 따라 동적으로 라이브러리 선택
- 각 상황에서 최고의 성능 확보

### 상세 리포트
- `INDEPENDENT_BENCHMARK_RESULTS.md`: 프로덕션용 독립 벤치마크 결과 및 권장사항.
