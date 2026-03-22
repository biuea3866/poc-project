# Phase 1 MVP QA 검증 리포트

> 검증일: 2026-03-22
> 대상: Closet E-Commerce Phase 1 MVP
> 서비스: Member(8081), Product(8082), Order(8083), Payment(8084), Gateway(8080), BFF(8085)
> Web(3000), Mobile(19006)

## 1. 검증 요약

| 서비스 | 총 TC | 통과 | 실패 | 미실행 |
|--------|------|------|------|--------|
| Member Service | 7 | 7 | 0 | 0 |
| Product Service | 10 | 10 | 0 | 0 |
| Order Service | 6 | 3 | 3 | 0 |
| Payment Service | 4 | 0 | 2 | 2 |
| Gateway | 4 | 0 | 4 | 0 |
| BFF | 3 | 0 | 3 | 0 |
| E2E Happy Path | 7 | 4 | 3 | 0 |
| **합계** | **41** | **24** | **15** | **2** |

**통과율: 58.5% (24/41)**

---

## 2. 서비스별 검증 결과

### 2.1 Member Service (7/7 통과)

| TC ID | 테스트명 | 기대 결과 | 실제 결과 | 상태 |
|-------|---------|----------|----------|------|
| TC-M001 | 회원가입 성공 | 201 + 회원 정보 반환 | 201, `{"id":8,"email":"qa-test@closet.com","name":"QA테스터","grade":"NORMAL","status":"ACTIVE"}` | PASS |
| TC-M002 | 중복 이메일 회원가입 | 409 Conflict | 409, `{"code":"C006","message":"이미 사용 중인 이메일입니다"}` | PASS |
| TC-M003 | 로그인 성공 | 200 + Access/Refresh Token | 200, accessToken + refreshToken + memberId 정상 반환 | PASS |
| TC-M004 | 잘못된 비밀번호 로그인 | 401 Unauthorized | 401, `{"code":"C004","message":"이메일 또는 비밀번호가 올바르지 않습니다"}` | PASS |
| TC-M005 | 내 정보 조회 | 200 + 회원 정보 | 200, 정상 반환 (id, email, name, phone, grade, pointBalance, status, createdAt) | PASS |
| TC-M006 | 배송지 등록 | 201 + 배송지 ID | 201, `{"id":1,"name":"QA테스터","isDefault":true}` - 첫 배송지 자동 기본 설정 | PASS |
| TC-M007 | 배송지 목록 조회 | 200 + 배송지 리스트 | 200, 1건 배송지 정상 반환 | PASS |

### 2.2 Product Service (10/10 통과)

| TC ID | 테스트명 | 기대 결과 | 실제 결과 | 상태 |
|-------|---------|----------|----------|------|
| TC-P001 | 상품 목록 조회 | 200 + 페이지네이션 | 200, 5건 반환, totalElements=24, totalPages=5, 페이지네이션 메타 정상 | PASS |
| TC-P002 | 상품 상세 조회 | 200 + 상품 상세 + 옵션 | 200, 상품 정보 + options 3개 (M/화이트, L/블랙, XL/네이비) + images 배열 반환 | PASS |
| TC-P003 | 카테고리 목록 조회 | 200 + 카테고리 리스트 | 200, 6개 카테고리 (상의/하의/아우터/신발/가방/액세서리), children 배열 포함 | PASS |
| TC-P004 | 브랜드 목록 조회 | 200 + 브랜드 리스트 | 200, 10개 브랜드 (무신사 스탠다드, 커버낫, 나이키 등), sellerId 매핑 정상 | PASS |
| TC-P005 | 상품 등록 | 201 + DRAFT 상태 | 201, `{"id":25,"status":"DRAFT"}` - DRAFT 상태로 생성 확인 | PASS |
| TC-P006 | 상태 변경 DRAFT->ACTIVE | 200 + ACTIVE 상태 | 200, `{"id":25,"status":"ACTIVE"}` | PASS |
| TC-P007 | 잘못된 상태 전이 ACTIVE->DRAFT | 400 에러 | 400, `{"code":"C001","message":"Cannot transition from ACTIVE to DRAFT"}` | PASS |
| TC-P008 | 옵션 추가 | 201 + 옵션 ID | 201, `{"id":52,"size":"M","colorName":"블랙","skuCode":"QA-TEST-BLK-M"}` | PASS |
| TC-P009 | 카테고리별 필터 | 200 + 해당 카테고리 상품만 | 200, categoryId=1 상품 6건만 반환 (상의 카테고리) | PASS |
| TC-P010 | 브랜드별 필터 | 200 + 해당 브랜드 상품만 | 200, brandId=1 상품 6건만 반환 (무신사 스탠다드) | PASS |

### 2.3 Order Service (3/6 통과)

| TC ID | 테스트명 | 기대 결과 | 실제 결과 | 상태 |
|-------|---------|----------|----------|------|
| TC-O001 | 장바구니 담기 | 201 + 장바구니 | 201, `{"id":1,"items":[{"id":1,"productId":1,"quantity":2,"unitPrice":12900}]}` | PASS |
| TC-O002 | 장바구니 조회 | 200 + 장바구니 | 200, 정상 반환 (주의: `memberId`는 헤더가 아닌 쿼리 파라미터로 전달 필요) | PASS |
| TC-O003 | 장바구니 수량 변경 | 200 + 수량 변경 | 200, quantity 3으로 변경 확인 | PASS |
| TC-O004 | 주문 생성 | 201 + PENDING 주문 | **500**, `{"code":"C003","message":"서버 오류가 발생했습니다"}` | **FAIL** |
| TC-O005 | 주문 조회 | 200 + 주문 상세 | 미실행 (주문 생성 실패로 인해 조회 대상 없음) | **FAIL** |
| TC-O006 | 주문 취소 | 200 + CANCELLED | 미실행 (주문 생성 실패로 인해 취소 대상 없음) | **FAIL** |

**장바구니 API 참고사항:**
- `GET /api/v1/carts`는 `X-Member-Id` 헤더가 아닌 `?memberId=` 쿼리 파라미터를 사용
- `POST /api/v1/carts/items`에서 `memberId`는 요청 body에 포함
- `DELETE /api/v1/carts/items/{itemId}` 정상 동작 확인 (204 No Content)

### 2.4 Payment Service (0/4 통과)

| TC ID | 테스트명 | 기대 결과 | 실제 결과 | 상태 |
|-------|---------|----------|----------|------|
| TC-PAY001 | 결제 승인 | 200 + APPROVED | **500**, `{"code":"C003","message":"서버 오류가 발생했습니다"}` | **FAIL** |
| TC-PAY002 | 결제 조회 | 200 + 결제 정보 | **500**, `{"code":"C003","message":"서버 오류가 발생했습니다"}` | **FAIL** |
| TC-PAY003 | 중복 결제 멱등성 | 기존 결제 반환 | 미실행 (결제 승인 실패) | **미실행** |
| TC-PAY004 | 결제 취소 | 200 + CANCELLED | 미실행 (결제 승인 실패) | **미실행** |

**근본 원인:** Payment 서비스에 API 엔드포인트가 구현되지 않음. `closet-payment/src/main/kotlin/`에 `ClosetPaymentApplication.kt` 1개 파일만 존재. Controller, Service, Domain 코드가 전혀 없음.

### 2.5 Gateway Service (0/4 통과)

| TC ID | 테스트명 | 기대 결과 | 실제 결과 | 상태 |
|-------|---------|----------|----------|------|
| TC-GW001 | Gateway 경유 상품 조회 | 200 + 상품 목록 | **연결 타임아웃 (HTTP 000)** — TCP 연결은 성립하나 HTTP 응답 없음 | **FAIL** |
| TC-GW002 | Gateway 인증 없이 접근 | 401 Unauthorized | **연결 타임아웃** | **FAIL** |
| TC-GW003 | Gateway 인증 포함 접근 | 200 + 회원 정보 | **연결 타임아웃** | **FAIL** |
| TC-GW004 | Gateway 경유 BFF | 200 + 메인 페이지 | **연결 타임아웃** | **FAIL** |

**근본 원인:** Gateway 프로세스(PID 59488)가 포트 8080에서 LISTEN 중이고 TCP 연결은 성공하지만, HTTP 요청에 대한 응답을 반환하지 않음. Spring Cloud Gateway(WebFlux/Netty 기반) 내부에서 라우팅 또는 필터 체인 초기화 오류로 추정.

### 2.6 BFF Service (0/3 통과)

| TC ID | 테스트명 | 기대 결과 | 실제 결과 | 상태 |
|-------|---------|----------|----------|------|
| TC-BFF001 | 상품 상세 BFF | 200 + 상품 + 관련 상품 | **연결 타임아웃 (HTTP 000)** | **FAIL** |
| TC-BFF002 | 체크아웃 BFF | 200 + 장바구니 + 배송지 | **연결 타임아웃** | **FAIL** |
| TC-BFF003 | 마이페이지 BFF | 200 + 회원 + 주문 + 배송지 | **연결 타임아웃** | **FAIL** |

**근본 원인:** BFF 프로세스(PID 59972)가 포트 8085에서 LISTEN 중이나 HTTP 응답 불가. Gateway와 동일한 증상.

### 2.7 Web / Mobile (정상 확인)

| 서비스 | 포트 | 상태 |
|--------|------|------|
| Web (Next.js) | 3000 | HTTP 200 정상 |
| Mobile (Expo) | 19006 | HTTP 200 정상 |

---

## 3. 발견된 버그

| # | 서비스 | 심각도 | 증상 | 기대 | 실제 | 원인 분석 |
|---|--------|--------|------|------|------|-----------|
| BUG-001 | Order | **Critical** | 주문 생성 API 500 에러 | 201 + PENDING 주문 | 500 Internal Server Error | `OrderStatusHistory` 엔티티에 `@EntityListeners(AuditingEntityListener.class)` 누락. `@CreatedDate` 어노테이션이 동작하지 않아 `created_at` NOT NULL 제약 조건 위반으로 추정 |
| BUG-002 | Payment | **Critical** | 결제 API 전체 미구현 | API 엔드포인트 동작 | 500 에러 (매핑 없음) | `closet-payment` 모듈에 Controller/Service/Domain 코드 미구현. Application 클래스만 존재 |
| BUG-003 | Gateway | **Critical** | HTTP 응답 불가 | API 라우팅 및 프록시 동작 | TCP 연결 성립 후 HTTP 응답 없음 (타임아웃) | Spring Cloud Gateway(Netty) 내부 필터 체인 또는 라우트 초기화 오류 추정. 로그 확인 필요 |
| BUG-004 | BFF | **Critical** | HTTP 응답 불가 | 통합 API 동작 | TCP 연결 성립 후 HTTP 응답 없음 (타임아웃) | BFF 서비스 Netty/Servlet 초기화 오류 추정. 의존 서비스(Gateway) 문제와 연관 가능 |
| BUG-005 | Order | **Minor** | 장바구니 조회 API 파라미터 불일치 | `X-Member-Id` 헤더 사용 | `memberId` 쿼리 파라미터 사용 | API 설계 문서와 실제 구현 불일치. `CartController.getCart()`가 `@RequestParam memberId`를 사용 |

---

## 4. E2E 시나리오 결과

### E2E-001: Happy Path 전체 구매

| Step | 작업 | 결과 | 상태 |
|------|------|------|------|
| 1 | 회원가입 | 201 - `{"id":9,"email":"e2e-test@closet.com","status":"ACTIVE"}` | PASS |
| 2 | 로그인 | 200 - accessToken + refreshToken 발급, memberId=9 | PASS |
| 3 | 상품 조회 | 200 - 상품 ID=1 상세 정보 + 옵션 3개 정상 반환 | PASS |
| 4 | 장바구니 담기 | 201 - `{"id":2,"memberId":9,"items":[{"productId":1,"quantity":1,"unitPrice":12900}]}` | PASS |
| 5 | 주문 생성 | **500** - `{"code":"C003","message":"서버 오류가 발생했습니다"}` | **FAIL** |
| 6 | 결제 승인 | 미실행 (주문 생성 실패) | **FAIL** |
| 7 | 주문 상태 확인 | 미실행 (결제 승인 실패) | **FAIL** |

**E2E 판정: FAIL** — Step 5 주문 생성에서 차단됨. 주문 -> 결제 플로우 전체 검증 불가.

---

## 5. 서비스 가용성 현황

| 서비스 | 포트 | 프로세스 | TCP | HTTP | 판정 |
|--------|------|---------|-----|------|------|
| Member | 8081 | java (PID 59059) | OK | OK | **정상** |
| Product | 8082 | ssh tunnel (PID 54258) | OK | OK | **정상** |
| Order | 8083 | java (PID 59061) | OK | 부분 동작 | **부분 장애** |
| Payment | 8084 | java (PID 59062) | OK | 500 | **장애** |
| Gateway | 8080 | java (PID 59488) | OK | 응답 없음 | **장애** |
| BFF | 8085 | java (PID 59972) | OK | 응답 없음 | **장애** |
| Web | 3000 | - | OK | OK | **정상** |
| Mobile | 19006 | - | OK | OK | **정상** |

---

## 6. 최종 판정

### **FAIL** — Phase 1 MVP 기본 기능 미완성, 다음 이슈 수정 후 재검증 필요

#### 즉시 수정 필요 (P0 - Blocker)

1. **BUG-001: Order 주문 생성 500 에러**
   - `OrderStatusHistory` 엔티티에 `@EntityListeners(AuditingEntityListener::class)` 추가
   - 파일: `closet-order/src/main/kotlin/com/closet/order/domain/order/OrderStatusHistory.kt`
   - 주문 생성 -> 결제 -> 주문 확인 전체 플로우의 시작점이므로 최우선 수정

2. **BUG-002: Payment 서비스 API 미구현**
   - Controller, Service, Domain, Repository 전체 구현 필요
   - 최소: 결제 승인(confirm), 결제 조회, 결제 취소 API

3. **BUG-003: Gateway 서비스 HTTP 응답 불가**
   - Spring Cloud Gateway Netty 초기화 로그 확인
   - 필터 체인(JWT 인증 필터 등) 디버깅 필요

4. **BUG-004: BFF 서비스 HTTP 응답 불가**
   - 서비스 시작 로그 확인 및 컨텍스트 로딩 오류 디버깅

#### 개선 권장 (P1)

5. **BUG-005: Cart API 파라미터 불일치**
   - API 설계 문서의 `X-Member-Id` 헤더 방식과 실제 구현의 `?memberId=` 쿼리 파라미터 방식 통일 필요
   - Gateway 인증 필터에서 주입하는 방식으로 통일 권장

#### 정상 동작 확인된 기능

- Member 서비스: 회원가입, 로그인, JWT 발급, 내 정보 조회, 배송지 CRUD — **전체 정상**
- Product 서비스: 상품 CRUD, 옵션 관리, 상태 전이(DRAFT->ACTIVE, 잘못된 전이 거부), 카테고리/브랜드 필터링 — **전체 정상**
- Order 서비스 장바구니: 추가, 조회, 수량 변경, 삭제 — **정상**
- Web/Mobile 프론트엔드: 정상 서빙 확인

---

## 7. 재검증 계획

1. BUG-001 ~ BUG-004 수정 후 전체 TC 재실행
2. 주문 생성 -> 결제 승인 -> 주문 상태 변경 E2E 플로우 재검증
3. Gateway 경유 전체 API 라우팅 검증
4. BFF 통합 API (상품 상세, 체크아웃, 마이페이지) 검증
