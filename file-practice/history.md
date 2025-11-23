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
