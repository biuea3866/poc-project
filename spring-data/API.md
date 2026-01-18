# Partition Test API

## 개요
MySQL 파티셔닝 성능을 테스트하기 위한 REST API

Base URL: `http://localhost:8080`

---

## API 엔드포인트

### 1. 파티셔닝 미적용 상품 조회

**목적**: 파티셔닝이 적용되지 않은 테이블 조회 성능 측정

```http
GET /api/partition-test/products/non-partitioned
```

**Query Parameters**:
- `page` (optional, default: 0) - 페이지 번호
- `size` (optional, default: 100) - 페이지 크기
- `startDate` (optional) - 시작 날짜 (yyyy-MM-dd)
- `endDate` (optional) - 종료 날짜 (yyyy-MM-dd)

**Example**:
```bash
curl "http://localhost:8080/api/partition-test/products/non-partitioned?page=0&size=100&startDate=2024-01-01&endDate=2024-12-31"
```

---

### 2. 파티셔닝 적용 상품 조회

**목적**: 파티셔닝이 적용된 테이블 조회 성능 측정

```http
GET /api/partition-test/products/partitioned
```

**Query Parameters**:
- `page` (optional, default: 0) - 페이지 번호
- `size` (optional, default: 100) - 페이지 크기
- `startDate` (optional) - 시작 날짜 (yyyy-MM-dd)
- `endDate` (optional) - 종료 날짜 (yyyy-MM-dd)

**Example**:
```bash
curl "http://localhost:8080/api/partition-test/products/partitioned?page=0&size=100&startDate=2024-01-01&endDate=2024-12-31"
```

---

### 3. JOIN 파티션 키 미포함

**목적**: 파티션 키 없이 JOIN할 때의 성능 측정 (전체 파티션 스캔)

```http
GET /api/partition-test/products-with-comments/without-partition-key
```

**Query Parameters**:
- `page` (optional, default: 0) - 페이지 번호
- `size` (optional, default: 10) - 페이지 크기
- `productStartDate` (optional) - 상품 시작 날짜 (yyyy-MM-dd)
- `productEndDate` (optional) - 상품 종료 날짜 (yyyy-MM-dd)

**특징**:
- 상품: 날짜 범위로 조회 (파티션 프루닝 적용)
- 댓글: productId만으로 조회 (파티션 프루닝 미적용, 전체 파티션 스캔)

**Example**:
```bash
curl "http://localhost:8080/api/partition-test/products-with-comments/without-partition-key?page=0&size=10&productStartDate=2024-01-01&productEndDate=2024-12-31"
```

---

### 4. JOIN 파티션 키 포함

**목적**: 파티션 키를 포함한 JOIN 성능 측정 (특정 파티션만 스캔)

```http
GET /api/partition-test/products-with-comments/with-partition-key
```

**Query Parameters** (필수):
- `page` (optional, default: 0) - 페이지 번호
- `size` (optional, default: 10) - 페이지 크기
- `startDate` (required) - 시작 날짜 (yyyy-MM-dd)
- `endDate` (required) - 종료 날짜 (yyyy-MM-dd)

**특징**:
- 상품: 날짜 범위로 조회 (파티션 프루닝 적용)
- 댓글: productId + 날짜 범위로 조회 (파티션 프루닝 적용)

**Example**:
```bash
curl "http://localhost:8080/api/partition-test/products-with-comments/with-partition-key?page=0&size=10&startDate=2024-01-01&endDate=2024-12-31"
```

---

### 5. Health Check

```http
GET /api/partition-test/health
```

**Example**:
```bash
curl "http://localhost:8080/api/partition-test/health"
```

---

## 응답 예시

### 상품 조회 (API 1, 2)
```json
{
  "content": [
    {
      "id": 1,
      "name": "Product-1",
      "price": 99.99,
      "category": "Electronics",
      "description": "Description for product 1",
      "stockQuantity": 100,
      "createdDate": "2024-01-15"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 100
  },
  "totalElements": 1000000,
  "totalPages": 10000
}
```

### 상품+댓글 조회 (API 3, 4)
```json
[
  {
    "productId": 1,
    "productName": "Product-P-1",
    "productPrice": 99.99,
    "productCategory": "Electronics",
    "productCreatedDate": "2024-01-15",
    "comments": [
      {
        "commentId": 1,
        "userName": "User-123",
        "content": "Great product!",
        "rating": 5,
        "createdDate": "2024-01-16"
      }
    ]
  }
]
```

---

## 애플리케이션 실행

### 개발 모드
```bash
./gradlew bootRun
```

### 성능 테스트 모드
```bash
./gradlew bootJar
java -jar build/libs/spring-data-0.0.1-SNAPSHOT.jar --spring.profiles.active=performance
```

---

## 주의사항

1. **날짜 형식**: yyyy-MM-dd (예: 2024-01-01)
2. **API 3 vs API 4**:
   - API 3은 파티션 프루닝이 부분적으로만 적용 (상품만)
   - API 4는 파티션 프루닝이 완전히 적용 (상품 + 댓글)
3. **페이지 크기**: JOIN API는 기본 10개, 일반 조회는 100개
