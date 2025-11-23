# Claude Package - 주문 내역 파일 다운로드 시스템

## 개요

주문 내역을 엑셀(XLSX) 또는 PDF 파일로 다운로드할 수 있는 시스템입니다.
- **동기 방식**: 즉시 파일을 생성하여 브라우저로 다운로드
- **비동기 방식**: 백그라운드에서 파일을 생성하고 이메일로 발송

## 주요 기능

### 1. 동적 컬럼 설정
사용자가 원하는 정보만 선택하여 다운로드할 수 있습니다.
- 주문 정보 (주문 ID, 주문 번호, 금액, 날짜 등)
- 상품 정보 (상품명, 가격, 수량 등)
- 쿠폰 정보 (쿠폰 코드, 할인율, 할인 금액 등)

### 2. 엑셀 다운로드
- Apache POI의 `SXSSFWorkbook` 사용으로 대용량 데이터 처리 시 OOM 방지
- 2개의 시트로 구성: "주문 내역" 시트, "상품" 시트
- 스타일이 적용된 헤더와 데이터

### 3. PDF 다운로드
- iText7 라이브러리 사용
- 한글 폰트 지원 (HYSMyeongJo-Medium)
- 주문별로 상세 정보를 표 형식으로 표시

### 4. 비동기 처리
- 코루틴을 사용한 비동기 파일 생성
- 완료 후 이메일로 자동 발송

## 패키지 구조

```
com.example.filepractice.claude/
├── domain/              # 도메인 모델
│   ├── Order.kt        # 주문
│   ├── Product.kt      # 상품
│   └── Coupon.kt       # 쿠폰
├── dto/                # DTO
│   ├── ColumnConfig.kt # 동적 컬럼 설정
│   └── DownloadRequest.kt
├── service/            # 서비스
│   ├── OrderService.kt
│   ├── ExcelGenerationService.kt
│   ├── PdfGenerationService.kt
│   ├── EmailService.kt
│   └── AsyncFileDownloadService.kt
└── controller/         # 컨트롤러
    └── FileDownloadController.kt
```

## API 엔드포인트

### 헬스 체크
```
GET /api/claude/download/health
```

### 동기 엑셀 다운로드
```
GET /api/claude/download/excel/sync?userId=100&includePrice=true
```
- `userId`: 사용자 ID (필수)
- `includePrice`: 가격 정보 포함 여부 (기본값: true)

### 비동기 엑셀 다운로드
```
POST /api/claude/download/excel/async
Content-Type: application/json

{
  "userId": 100,
  "email": "user@example.com",
  "columnConfig": {
    "orderColumns": ["ID", "ORDER_NUMBER", "TOTAL_AMOUNT"],
    "productColumns": ["NAME", "QUANTITY"],
    "couponColumns": ["CODE", "DISCOUNT_AMOUNT"]
  }
}
```

### 동기 PDF 다운로드
```
GET /api/claude/download/pdf/sync?userId=100
```

### 비동기 PDF 다운로드
```
POST /api/claude/download/pdf/async
Content-Type: application/json

{
  "userId": 100,
  "email": "user@example.com"
}
```

## 테스트

모든 컴포넌트에 대한 단위 테스트가 포함되어 있습니다:

```bash
./gradlew test --tests "com.example.filepractice.claude.*"
```

### 테스트 커버리지
- ✅ ExcelGenerationServiceTest: 엑셀 생성 기능 테스트
- ✅ PdfGenerationServiceTest: PDF 생성 기능 테스트
- ✅ OrderServiceTest: 주문 데이터 조회 테스트
- ✅ FileDownloadControllerTest: API 엔드포인트 테스트

## 사용 예제

### 1. 모든 정보를 포함한 엑셀 다운로드
```bash
curl -O -J "http://localhost:8080/api/claude/download/excel/sync?userId=100"
```

### 2. 가격 정보를 제외한 엑셀 다운로드
```bash
curl -O -J "http://localhost:8080/api/claude/download/excel/sync?userId=100&includePrice=false"
```

### 3. 비동기 이메일 발송
```bash
curl -X POST http://localhost:8080/api/claude/download/excel/async \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 100,
    "email": "user@example.com"
  }'
```

## 주요 기술 스택

- **언어**: Kotlin 1.9.21
- **프레임워크**: Spring Boot 3.2.0
- **엑셀 라이브러리**: Apache POI 5.2.5 (SXSSFWorkbook)
- **PDF 라이브러리**: iText7 7.2.5
- **비동기 처리**: Kotlin Coroutines

## 주요 특징

### OOM 방지
`SXSSFWorkbook`을 사용하여 메모리에 100개 행만 유지하고 나머지는 디스크에 임시 저장합니다.

```kotlin
val workbook = SXSSFWorkbook(100)  // 메모리에 100개 행만 유지
```

### 동적 컬럼 선택
사용자가 원하는 컬럼만 선택하여 다운로드할 수 있습니다.

```kotlin
val columnConfig = ColumnConfig.withoutPriceInfo()  // 가격 정보 제외
```

### 한글 폰트 지원
iText7의 font-asian 패키지를 사용하여 PDF에 한글을 정상적으로 표시합니다.

```kotlin
val koreanFont = PdfFontFactory.createFont("HYSMyeongJo-Medium", "UniKS-UCS2-H")
```

## 참고사항

- 이메일 서비스는 현재 모킹 구현으로, 실제 이메일을 발송하지 않고 로그만 출력합니다.
- 실제 환경에서는 JavaMailSender, AWS SES, SendGrid 등을 연동하여 사용하세요.
- 비동기 처리는 코루틴을 사용하며, 프로덕션 환경에서는 메시지 큐(RabbitMQ, Kafka 등) 사용을 권장합니다.
