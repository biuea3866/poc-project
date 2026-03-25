# [Ticket #7] Product 도메인 (상품 카탈로그 + 가격 정책)

## 개요
- TDD 참조: tdd.md 섹션 4.1.1, 4.2 (domain/product/)
- 선행 티켓: #2
- 크기: M

## 작업 내용

### 변경 사항

1. **ProductType enum 정의**
   - `SUBSCRIPTION`: 구독형 (월/연 자동 갱신)
   - `CONSUMABLE`: 소진형 (포인트 충전 후 사용)
   - `ONE_TIME`: 건별 결제형 (1회 결제 1회 사용)

2. **Product entity 구현**
   - `code` (UNIQUE): 상품 식별 코드 (예: `PLAN_BASIC`, `SMS_PACK_1000`, `AI_CREDIT_100`)
   - `name`: 상품명
   - `productType`: ProductType enum
   - `description`: 상품 설명
   - `isActive`: 활성 여부 (기본 true)
   - BaseEntity 상속 (created_at, updated_at, deleted_at)

3. **ProductMetadata entity 구현**
   - Product와 1:N 관계 (product_id FK)
   - `metaKey` + `metaValue` key-value 쌍
   - 예: `plan_level=STANDARD`, `credit_amount=100`, `sms_count=1000`
   - UNIQUE 제약: (product_id, meta_key)

4. **ProductPrice entity 구현**
   - Product와 1:N 관계 (product_id FK)
   - `price`: 원가 (VAT 별도, INT)
   - `currency`: 통화 코드 (기본 `KRW`)
   - `billingIntervalMonths`: 구독 상품만 사용 (1=월, 12=연). 비구독은 NULL
   - `validFrom` / `validTo`: 유효 기간. `validTo=NULL`이면 현재 유효 가격
   - 가격 이력 관리: 새 가격 등록 시 기존 가격의 validTo 설정

5. **ProductRepository 구현**
   - `findByCode(code: String): Product?`
   - `findByProductType(type: ProductType): List<Product>`
   - `findByIsActiveTrue(): List<Product>`

6. **ProductService 구현**
   - `findByCode(code: String): Product` — 없으면 `ProductNotFoundException`
   - `findActiveProducts(): List<Product>` — isActive=true만
   - `findActiveProductsByType(type: ProductType): List<Product>`
   - `getCurrentPrice(productId: Long, billingIntervalMonths: Int?): ProductPrice` — validFrom ≤ now AND (validTo IS NULL OR validTo > now) 조건으로 현재 유효 가격 조회. 없으면 `PriceNotFoundException`

7. **상품 카탈로그 하이브리드 전략**
   - ProductType은 앱 레벨 enum (코드에서 분기)
   - 가격/메타데이터는 DB 관리 (코드 배포 없이 변경 가능)
   - Product.code를 통해 도메인 로직에서 상품 식별

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | domain | domain/product/ProductType.kt | 신규 |
| greeting_payment-server | domain | domain/product/Product.kt | 신규 |
| greeting_payment-server | domain | domain/product/ProductMetadata.kt | 신규 |
| greeting_payment-server | domain | domain/product/ProductPrice.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/ProductRepository.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/ProductPriceRepository.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/ProductMetadataRepository.kt | 신규 |
| greeting_payment-server | application | application/ProductService.kt | 신규 |
| greeting_payment-server | domain | domain/product/exception/ProductNotFoundException.kt | 신규 |
| greeting_payment-server | domain | domain/product/exception/PriceNotFoundException.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T7-01 | 상품코드로 조회 성공 | Product(code=PLAN_BASIC) 존재 | findByCode("PLAN_BASIC") | Product 반환 |
| T7-02 | 활성 상품 목록 조회 | isActive=true 3건, false 1건 | findActiveProducts() | 3건 반환 |
| T7-03 | 타입별 활성 상품 조회 | SUBSCRIPTION 2건, CONSUMABLE 1건 | findActiveProductsByType(SUBSCRIPTION) | 2건 반환 |
| T7-04 | 현재 유효 가격 조회 | validFrom=어제, validTo=NULL | getCurrentPrice(productId, 1) | 해당 가격 반환 |
| T7-05 | 월간/연간 가격 구분 | 동일 상품에 billingInterval=1, 12 각각 존재 | getCurrentPrice(productId, 12) | 연간 가격 반환 |
| T7-06 | 메타데이터 조회 | Product에 plan_level=STANDARD 메타 존재 | product.metadata 조회 | key-value 쌍 반환 |
| T7-07 | 비구독 상품 가격 조회 | CONSUMABLE 상품, billingInterval=NULL | getCurrentPrice(productId, null) | 가격 반환 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T7-E01 | 존재하지 않는 코드 | 해당 코드 상품 없음 | findByCode("INVALID") | ProductNotFoundException |
| T7-E02 | 유효한 가격 없음 | validTo가 모두 과거 | getCurrentPrice(productId, 1) | PriceNotFoundException |
| T7-E03 | 삭제된 상품 제외 | deleted_at 설정된 상품 | findActiveProducts() | 해당 상품 미포함 |
| T7-E04 | 비활성 상품 제외 | isActive=false 상품 | findActiveProducts() | 해당 상품 미포함 |
| T7-E05 | 가격 기간 겹침 방지 | 동일 상품/interval에 validTo=NULL 두 건 | 데이터 정합성 검증 | 비즈니스 로직에서 최신 1건만 반환 |

## 기대 결과 (AC)
- [ ] ProductType enum이 SUBSCRIPTION, CONSUMABLE, ONE_TIME 3개 값을 가진다
- [ ] Product entity가 code, name, productType, isActive, description 필드를 가지며 BaseEntity를 상속한다
- [ ] ProductMetadata가 product_id + meta_key UNIQUE 제약으로 key-value 저장을 지원한다
- [ ] ProductPrice가 validFrom/validTo 기반 가격 이력 관리를 지원한다
- [ ] ProductService.findByCode()가 존재하지 않는 상품에 대해 ProductNotFoundException을 던진다
- [ ] ProductService.getCurrentPrice()가 현재 시점 유효 가격을 정확히 반환한다
- [ ] 구독 상품은 billingIntervalMonths로 월간/연간 가격을 구분할 수 있다
- [ ] 단위 테스트 커버리지 80% 이상
