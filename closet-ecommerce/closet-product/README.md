# closet-product

> 상품 카탈로그, 카테고리, 브랜드, 옵션(사이즈/색상), 사이즈 가이드 관리 서비스

## 역할

closet-product는 상품 도메인을 담당하는 서비스이다.
상품 CRUD, 카테고리(3depth 계층 구조), 브랜드, 상품 옵션(사이즈/색상/SKU), 상품 이미지, 사이즈 가이드를 관리한다.
의류 특화 속성으로 시즌(SS/FW/ALL), 핏 타입(OVERSIZED/REGULAR/SLIM), 성별(MALE/FEMALE/UNISEX)을 지원한다.

## 기술 스택

| 기술 | 용도 |
|------|------|
| Spring Boot Starter Web | REST API |
| Spring Data JPA | 엔티티 매핑, Repository |
| QueryDSL | 동적 상품 검색 쿼리 |
| MySQL 8.0 (Flyway) | 데이터 저장 |
| Spring Data Redis | 캐싱 |
| Virtual Threads | 가상 스레드 활성화 |

## 도메인 모델

### Product (Aggregate Root)
상품 엔티티. `name`, `description`, `brandId`, `categoryId`, `basePrice`/`salePrice`(Money VO), `discountRate`, `status`, `season`, `fitType`, `gender` 필드를 가진다.
`options`, `images`, `sizeGuides`를 OneToMany (CascadeType.ALL, orphanRemoval)로 관리한다.
`activate()`, `deactivate()`, `markSoldOut()`, `changeStatus()`, `updatePrice()`, `addOption()`, `removeOption()` 등 비즈니스 로직을 캡슐화한다.

### ProductOption
상품 옵션 엔티티. 사이즈(XS~FREE)와 색상(colorName, colorHex)의 조합으로 SKU를 생성한다. `additionalPrice`(Money)를 가진다.

### ProductImage
상품 이미지 엔티티. `imageUrl`, `type`(MAIN/DETAIL), `sortOrder` 필드.

### SizeGuide
사이즈 가이드 엔티티. 실측 정보를 관리한다: `shoulderWidth`, `chestWidth`, `totalLength`, `sleeveLength` (단위: cm).

### Category
카테고리 엔티티. 최대 3depth 계층 구조(`parentId`, `depth`). `sortOrder`로 노출 순서를 관리한다.

### Brand
브랜드 엔티티. `name`, `logoUrl`, `description`, `sellerId`, `status` 필드.

### ProductStatus
상품 상태 enum: `DRAFT -> ACTIVE <-> SOLD_OUT / INACTIVE`. `canTransitionTo()` / `validateTransitionTo()`로 전이 규칙을 관리한다.

## API

| Method | Path | 설명 |
|--------|------|------|
| POST | /api/v1/products | 상품 등록 |
| PUT | /api/v1/products/{id} | 상품 수정 |
| GET | /api/v1/products/{id} | 상품 상세 조회 |
| GET | /api/v1/products | 상품 목록 조회 (카테고리/브랜드/가격/상태 필터, 페이징) |
| PATCH | /api/v1/products/{id}/status | 상품 상태 변경 |
| POST | /api/v1/products/{id}/options | 상품 옵션 추가 |
| DELETE | /api/v1/products/{id}/options/{optionId} | 상품 옵션 삭제 |
| GET | /api/v1/categories | 카테고리 목록 조회 |
| POST | /api/v1/categories | 카테고리 등록 |
| GET | /api/v1/brands | 브랜드 목록 조회 |
| POST | /api/v1/brands | 브랜드 등록 |

## 패키지 구조

```
src/main/kotlin/com/closet/product/
├── application/
│   ├── dto/            # ProductDto (Request/Response)
│   └── service/        # ProductService, BrandService, CategoryService
├── domain/
│   ├── entity/         # Product, ProductOption, ProductImage, SizeGuide, Brand, Category
│   ├── enums/          # ProductStatus, FitType, Gender, ImageType, Season, Size
│   └── repository/     # ProductRepository, ProductRepositoryCustom, ProductOptionRepository, BrandRepository, CategoryRepository
├── infrastructure/
│   ├── config/         # QueryDslConfig
│   └── repository/     # ProductRepositoryImpl (QueryDSL)
└── presentation/       # ProductController, BrandController, CategoryController
```

## DB 테이블

| 테이블 | 설명 |
|--------|------|
| product | 상품 기본 정보 (name, brand_id, category_id, base_price, sale_price, status, season, fit_type, gender) |
| product_option | 상품 옵션 - 사이즈/색상 조합 (size, color_name, color_hex, sku_code, additional_price) |
| product_image | 상품 이미지 (image_url, type, sort_order) |
| size_guide | 사이즈 가이드 실측 정보 (shoulder_width, chest_width, total_length, sleeve_length) |
| category | 카테고리 (parent_id, depth, sort_order) - 최대 3depth 계층 |
| brand | 브랜드 정보 (name, logo_url, seller_id) |

## 포트

- 서버 포트: 8082

## 의존 서비스

- closet-common (공통 라이브러리)
- Redis (캐싱)
