# Phase 1 QA 검증 리포트

> 검증일: 2026-03-22 (2차 실행)
> 검증 환경: localhost (Member:8081, Product:8082, Order:8083, Payment:8084, Gateway:8080, BFF:8085)

## 1. 검증 요약

| 서비스 | TC 수 | 통과 | 실패 |
|--------|------|------|------|
| Member | 8 | 8 | 0 |
| Product | 8 | 8 | 0 |
| Order | 4 | 1 | 3 |
| Payment | 3 | 0 | 3 |
| Gateway | 3 | 2 | 1 |
| BFF | 2 | 0 | 2 |
| **합계** | **28** | **19** | **9** |

**통과율: 67.9% (19/28)**

---

## 2. 상세 결과

### 2.1 Member Service (8/8 통과)

| # | TC | 기대 결과 | 실제 응답 | 판정 |
|---|-----|----------|----------|------|
| 1 | 회원가입 | 201 / success:true | 201 / `{"success":true,"data":{"id":3,"email":"qa2@closet.com","name":"QA2","grade":"NORMAL","status":"ACTIVE"}}` | PASS |
| 2 | 중복 회원가입 | 409 / success:false | 409 / `{"success":false,"error":{"code":"C006","message":"이미 사용 중인 이메일입니다"}}` | PASS |
| 3 | 로그인 | 200 / success:true + accessToken | 200 / `{"success":true,"data":{"accessToken":"eyJ...","refreshToken":"eyJ...","memberId":3}}` | PASS |
| 4 | 잘못된 비밀번호 | 401 / success:false | 401 / `{"success":false,"error":{"code":"C004","message":"이메일 또는 비밀번호가 올바르지 않습니다"}}` | PASS |
| 5 | 내 정보 (인증) | 200 / success:true | 200 / `{"success":true,"data":{"id":3,"email":"qa2@closet.com","name":"QA2","grade":"NORMAL","pointBalance":0,"status":"ACTIVE"}}` | PASS |
| 6 | 내 정보 (토큰 없음) | 401 / success:false | 401 / `{"success":false,"error":{"code":"C004","message":"인증이 필요합니다"}}` | PASS |
| 7 | 배송지 등록 | 201 / success:true | 201 / `{"success":true,"data":{"id":2,"name":"QA","zipCode":"06035","isDefault":true}}` | PASS |
| 8 | 배송지 목록 | 200 / success:true + 배열 | 200 / `{"success":true,"data":[{"id":2,"name":"QA","isDefault":true}]}` | PASS |

### 2.2 Product Service (8/8 통과)

| # | TC | 기대 결과 | 실제 응답 | 판정 |
|---|-----|----------|----------|------|
| 9 | 상품 목록 | 200 / success:true + 페이징 | 200 / `{"success":true,"data":{"content":[...],"totalElements":26,"totalPages":6}}` (5건 반환) | PASS |
| 10 | 상품 상세 | 200 / success:true + options | 200 / `{"success":true,"data":{"id":1,"name":"무신사 스탠다드 릴렉스핏 반팔 티셔츠","options":[3개],"images":[]}}` | PASS |
| 11 | 카테고리 | 200 / success:true + 목록 | 200 / 6개 카테고리 (상의/하의/아우터/신발/가방/액세서리), children 배열 포함 | PASS |
| 12 | 브랜드 | 200 / success:true + 목록 | 200 / 10개 브랜드 (무신사 스탠다드, 커버낫, 나이키, 아디다스 등) | PASS |
| 13 | 카테고리 필터 | 200 / success:true + 필터 | 200 / `{"success":true,"data":{"content":[...],"totalElements":11}}` (categoryId=1 상의만) | PASS |
| 14 | 상품 등록 | 201 / success:true + DRAFT | 201 / `{"success":true,"data":{"id":27,"name":"QA상품","status":"DRAFT"}}` | PASS |
| 15 | DRAFT→ACTIVE | 200 / success:true + ACTIVE | 200 / `{"success":true,"data":{"id":28,"status":"ACTIVE"}}` | PASS |
| 16 | ACTIVE→DRAFT (실패 기대) | 400 / success:false | 400 / `{"success":false,"error":{"code":"C001","message":"Cannot transition from ACTIVE to DRAFT"}}` | PASS |

### 2.3 Order Service (1/4 통과)

| # | TC | 기대 결과 | 실제 응답 | 판정 |
|---|-----|----------|----------|------|
| 17 | 장바구니 담기 | 201 / success:true | 201 / `{"success":true,"data":{"id":1,"items":[{"id":1,"productId":1,"quantity":2,"unitPrice":12900},{"id":2,...}]}}` | PASS |
| 18 | 장바구니 조회 | 200 / success:true | 500 / `{"success":false,"error":{"code":"C003","message":"서버 오류가 발생했습니다"}}` | **FAIL** |
| 19 | 주문 생성 | 201 / success:true | 500 / `{"success":false,"error":{"code":"C003","message":"서버 오류가 발생했습니다"}}` | **FAIL** |
| 20 | 주문 조회 | 200 / success:true | 404 / `{"success":false,"error":{"code":"C002","message":"주문을 찾을 수 없습니다. id=1"}}` (주문 생성 실패로 조회 불가) | **FAIL** |

### 2.4 Payment Service (0/3 통과)

| # | TC | 기대 결과 | 실제 응답 | 판정 |
|---|-----|----------|----------|------|
| 21 | 결제 승인 | 200 / success:true | 500 / `{"success":false,"error":{"code":"C003","message":"서버 오류가 발생했습니다"}}` | **FAIL** |
| 22 | 결제 조회 | 200 / success:true | 500 / `{"success":false,"error":{"code":"C003","message":"서버 오류가 발생했습니다"}}` | **FAIL** |
| 23 | 중복 결제 (멱등성) | 409 or 200 | 500 / `{"success":false,"error":{"code":"C003","message":"서버 오류가 발생했습니다"}}` | **FAIL** |

### 2.5 Gateway Service (2/3 통과)

| # | TC | 기대 결과 | 실제 응답 | 판정 |
|---|-----|----------|----------|------|
| 24 | Gateway 상품 조회 (공개) | 200 / success:true | 200 / `{"success":true,"data":{"content":[...],"totalElements":28}}` | PASS |
| 25 | Gateway 인증 없이 (401) | 401 | 401 / `{"success":false,"error":{"code":"AUTH001","message":"Authorization header missing"}}` | PASS |
| 26 | Gateway 인증 있음 | 200 / success:true | 401 / `{"success":false,"error":{"code":"AUTH001","message":"Invalid token"}}` | **FAIL** |

### 2.6 BFF Service (0/2 통과)

| # | TC | 기대 결과 | 실제 응답 | 판정 |
|---|-----|----------|----------|------|
| 27 | BFF 홈 | 200 / success:true | 타임아웃 (HTTP_CODE:000, 10초 내 응답 없음) | **FAIL** |
| 28 | BFF 상품 상세 | 200 / success:true | 타임아웃 (HTTP_CODE:000, 10초 내 응답 없음) | **FAIL** |

---

## 3. 발견된 버그

| # | 서비스 | 심각도 | 증상 | 기대 | 실제 |
|---|--------|--------|------|------|------|
| BUG-01 | Order | **Critical** | 장바구니 조회 500 에러 | GET /carts -> 200 + 장바구니 목록 | 500 서버 오류 (C003). POST로 담기는 성공하지만 GET 조회에서 서버 에러. X-Member-Id 헤더 vs 쿼리 파라미터 불일치 또는 직렬화 오류 추정 |
| BUG-02 | Order | **Critical** | 주문 생성 500 에러 | POST /orders -> 201 + PENDING 주문 | 500 서버 오류 (C003). `OrderStatusHistory` 엔티티의 `@CreatedDate`/`@EntityListeners` 누락 또는 DB 제약 조건 위반 추정 |
| BUG-03 | Payment | **Critical** | 결제 서비스 전체 API 500 에러 | 결제 승인/조회 정상 동작 | 모든 API 엔드포인트에서 500 반환. 서비스 미구현 또는 DB 설정 오류 |
| BUG-04 | Gateway | **Major** | 인증된 요청 토큰 검증 실패 | Bearer 토큰으로 인증 통과 -> Member 프록시 | 401 "Invalid token". Member 서비스가 발급한 JWT를 Gateway가 검증하지 못함. **JWT secret 불일치** 추정 |
| BUG-05 | BFF | **Critical** | BFF 전체 API 무한 대기 | 200 + 통합 응답 | 커넥션 타임아웃 (응답 없음). BFF가 하위 서비스(Order/Payment) 호출 시 무한 대기. WebClient 타임아웃 미설정 또는 하위 서비스 장애 전파 |
| BUG-06 | Product | **Minor** | DRAFT 상품 목록 노출 | ACTIVE 상품만 목록 반환 | Gateway 경유 상품 목록(TC-24)에서 status=DRAFT 상품(id:27)이 포함되어 반환됨. 상품 목록 쿼리에 상태 필터 누락 |

---

## 4. E2E Happy Path 결과

**회원가입 → 로그인 → 상품조회 → 장바구니 → 주문 → 결제** 전체 구매 플로우:

| 단계 | 작업 | 결과 |
|------|------|------|
| 1 | 회원가입 | 성공 (201) |
| 2 | 로그인 | 성공 (200, 토큰 발급) |
| 3 | 상품 조회 | 성공 (200) |
| 4 | 장바구니 담기 | 성공 (201) |
| 5 | 장바구니 조회 | **실패** (500) |
| 6 | 주문 생성 | **실패** (500) |
| 7 | 결제 승인 | **실패** (500) |

**E2E Happy Path: 실패** -- 4단계(장바구니 담기)까지 성공, 5단계(장바구니 조회)부터 차단됨

---

## 5. 최종 판정

### **불합격 (FAIL)** -- 핵심 구매 플로우 미완성

#### 서비스별 판정

| 서비스 | 판정 | 비고 |
|--------|------|------|
| Member (8081) | **합격** | 8/8 TC 통과. 인증/회원/배송지 전체 정상 |
| Product (8082) | **합격** | 8/8 TC 통과. CRUD, 상태 전이, 필터 전체 정상 |
| Order (8083) | **불합격** | 1/4 TC 통과. 장바구니 담기만 성공, 조회/주문 생성 500 에러 |
| Payment (8084) | **불합격** | 0/3 TC 통과. 전체 API 500 에러 |
| Gateway (8080) | **조건부 합격** | 2/3 TC 통과. 공개 API 프록시 정상, JWT 인증 프록시 실패 |
| BFF (8085) | **불합격** | 0/2 TC 통과. 전체 API 타임아웃 |

#### 우선 조치 사항

| 우선순위 | 대상 | 조치 내용 |
|----------|------|----------|
| **P0** | Order 서비스 | 장바구니 조회 및 주문 생성 500 에러 수정. `OrderStatusHistory` 엔티티 감사(Auditing) 설정, 장바구니 조회 API 파라미터 바인딩 확인 |
| **P0** | Payment 서비스 | 전체 API 500 에러 원인 조사. Controller/Service/Repository 구현 상태 및 DB 테이블 생성 확인 |
| **P0** | BFF 서비스 | WebClient 타임아웃 설정(connect/read), 서킷브레이커 적용으로 하위 서비스 장애 전파 차단 |
| **P1** | Gateway | Member 서비스와 JWT signing secret 동기화. 동일한 secret key를 사용하도록 application.yml 설정 통일 |
| **P2** | Product 서비스 | 상품 목록 API에서 DRAFT 상태 상품 필터링 추가 (ACTIVE 상품만 반환) |

#### 1차 QA 대비 개선점

- **Gateway**: 이전 검증에서 전체 타임아웃이었으나, 현재 공개 API 프록시(상품 조회)와 인증 거부(401) 응답이 정상 동작. 라우팅 기능 복구 확인
- **Member/Product**: 이전과 동일하게 전체 TC 통과로 안정적

#### 재검증 조건

위 P0 항목 3건 수정 후 28개 TC 전체 재실행 필요
