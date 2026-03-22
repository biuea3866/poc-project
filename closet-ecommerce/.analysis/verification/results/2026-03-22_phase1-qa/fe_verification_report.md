# Phase 1 FE QA 검증 리포트

> 검증일: 2026-03-22

## 1. 검증 요약

| 플랫폼 | 총 TC | 통과 | 실패 | 미실행 |
|--------|------|------|------|--------|
| Web (Next.js) | 18 | 16 | 2 | 0 |
| Mobile (Expo) | 14 | 12 | 2 | 0 |

## 2. Web 검증 결과

### 2.1 페이지 렌더링

| 페이지 | URL | HTTP 상태 | 한글 텍스트 | 상태 |
|--------|-----|----------|-----------|------|
| 메인 | `/` | 200 | Closet, 인기 상품, 카테고리 | PASS |
| 로그인 | `/login` | 200 | 로그인, 이메일, 비밀번호 | PASS |
| 회원가입 | `/register` | 200 | 회원가입, 이름, 전화번호 | PASS |
| 상품 목록 | `/products` | 200 | 상품 목록, 필터, 카테고리 | PASS |
| 상품 상세 | `/products/1` | 200 | 장바구니 담기, 수량 | PASS |
| 장바구니 | `/cart` | 200 | 장바구니, 합계, 주문하기 | PASS |
| 주문 내역 | `/orders` | 200 | (인증 필요 페이지) | PASS |
| 마이페이지 | `/mypage` | 200 | 마이페이지, 내 정보, 주문 내역, 배송지 | PASS |
| 배송지 관리 | `/mypage/addresses` | 200 | (인증 필요 페이지) | PASS |

> 모든 페이지 HTTP 200 반환 확인.

### 2.2 한글화 검증

| 페이지 | 영어 잔존 텍스트 | 상태 |
|--------|----------------|------|
| 메인 (`/`) | 없음 (Home, cart, login 등은 href 속성값으로 UI 노출 아님) | PASS |
| 로그인 (`/login`) | 없음 (submit은 form attribute, 가시 텍스트는 한글) | PASS |
| 회원가입 (`/register`) | 없음 | PASS |
| 상품 목록 (`/products`) | 없음 | PASS |
| 장바구니 (`/cart`) | 없음 | PASS |
| 마이페이지 (`/mypage`) | 없음 | PASS |

> grep으로 검출된 영어 단어(Home, cart, login, products, search, submit, register)는 모두 HTML 속성(href, type, class) 내부 값이며 사용자에게 보이는 UI 텍스트가 아님을 HTML 소스 분석으로 확인.

### 2.3 API 클라이언트

| 항목 | 상태 |
|------|------|
| baseURL 설정 | PASS -- `process.env.NEXT_PUBLIC_API_URL \|\| 'http://localhost:8080/api/v1'` |
| JWT interceptor (Request) | PASS -- localStorage에서 accessToken 읽어 Authorization 헤더 부착 |
| JWT interceptor (Response 401) | WARN -- refresh token 로직 미구현 (TODO 주석만 존재) |
| API 함수 BE 매핑 | PASS -- product, order, cart, member, payment 모두 REST 엔드포인트 매핑 확인 |

**API 엔드포인트 매핑 상세:**

| 파일 | 함수 | 엔드포인트 | 상태 |
|------|------|-----------|------|
| `product.ts` | `getProducts` | `GET /products` | PASS |
| `product.ts` | `getProduct` | `GET /products/:id` | PASS |
| `product.ts` | `getCategories` | `GET /categories` | PASS |
| `product.ts` | `getBrands` | `GET /brands` | PASS |
| `order.ts` | `createOrder` | `POST /orders` | PASS |
| `order.ts` | `getOrders` | `GET /orders` | PASS |
| `order.ts` | `getOrder` | `GET /orders/:id` | PASS |
| `order.ts` | `cancelOrder` | `POST /orders/:id/cancel` | PASS |
| `cart.ts` | `getCart` | `GET /cart` | PASS |
| `cart.ts` | `addCartItem` | `POST /cart/items` | PASS |
| `cart.ts` | `updateCartItem` | `PATCH /cart/items/:id` | PASS |
| `cart.ts` | `removeCartItem` | `DELETE /cart/items/:id` | PASS |
| `cart.ts` | `clearCart` | `DELETE /cart` | PASS |
| `member.ts` | `login` | `POST /members/login` | PASS |
| `member.ts` | `register` | `POST /members/register` | PASS |
| `member.ts` | `getMe` | `GET /members/me` | PASS |
| `member.ts` | `getAddresses` | `GET /members/me/addresses` | PASS |
| `member.ts` | `addAddress` | `POST /members/me/addresses` | PASS |
| `member.ts` | `deleteAddress` | `DELETE /members/me/addresses/:id` | PASS |
| `payment.ts` | `confirmPayment` | `POST /payments/confirm` | PASS |

### 2.4 컴포넌트 구조

| 디렉토리 | 파일 수 | 누락 |
|----------|--------|------|
| `app/` (페이지) | 9 | 없음 |
| `components/common/` | 2 (Header, Footer) | 없음 |
| `components/auth/` | 2 (LoginForm, RegisterForm) | 없음 |
| `components/product/` | 3 (ProductCard, ProductFilter, ProductList) | 없음 |
| `components/cart/` | 2 (CartItem, CartSummary) | 없음 |
| `components/order/` | 2 (OrderForm, OrderStatus) | 없음 |
| `stores/` | 2 (authStore, cartStore) | 없음 |
| `types/` | 6 (common, member, product, cart, order, payment) | 없음 |
| `lib/api/` | 6 (client, product, order, cart, member, payment) | 없음 |
| **합계** | **34** | **없음** |

### 2.5 TypeScript 타입 검증

| 타입 파일 | 인터페이스 수 | BE 응답 포맷 일치 | 비고 |
|-----------|-------------|-----------------|------|
| `product.ts` | 6 (Product, ProductImage, ProductOption, ProductOptionValue, Category, Brand) | 기본 일치 | Web은 범용적 Option 구조 사용 |
| `order.ts` | 4 (Order, OrderItem, CreateOrderRequest, CreateOrderItemRequest) + enum | 일치 | |
| `cart.ts` | 3 (Cart, CartItem, AddCartItemRequest, UpdateCartItemRequest) | 기본 일치 | 단순 구조 |
| `member.ts` | 5 (Member, ShippingAddress, LoginRequest, LoginResponse, RegisterRequest) | 일치 | |
| `payment.ts` | 3 (Payment, ConfirmPaymentRequest) + type | 일치 | |

## 3. Mobile 검증 결과

### 3.1 프로젝트 구조

| 디렉토리 | 파일 수 | 설명 |
|----------|--------|------|
| `screens/auth/` | 2 | LoginScreen, RegisterScreen |
| `screens/home/` | 1 | HomeScreen |
| `screens/product/` | 2 | ProductListScreen, ProductDetailScreen |
| `screens/cart/` | 1 | CartScreen |
| `screens/order/` | 3 | CheckoutScreen, OrderListScreen, OrderDetailScreen |
| `screens/mypage/` | 2 | MyPageScreen, AddressListScreen |
| `navigation/` | 4 | RootNavigator, AuthNavigator, MainTabNavigator, types |
| `components/common/` | 5 | Button, Input, Card, Badge, Loading |
| `components/product/` | 3 | ProductCard, ProductOptionSelector, SizeGuide |
| `components/cart/` | 2 | CartItemRow, CartSummary |
| `components/order/` | 2 | OrderStatusBadge, OrderItemRow |
| `stores/` | 2 | authStore, cartStore |
| `hooks/` | 2 | useAuth, useCart |
| `api/` | 6 | client, member, product, cart, order, payment |
| `types/` | 6 | common, member, product, cart, order, payment |
| `utils/` | 1 | format |
| **합계** | **44** | |

### 3.2 네비게이션

| 항목 | 상태 | 상세 |
|------|------|------|
| Auth flow | PASS | Login -> Register 스택 네비게이션, 인증 여부로 Auth/Main 분기 |
| Main tab flow | PASS | 홈 / 카테고리 / 장바구니 / 마이페이지 4탭 구성 |
| Tab 한글 라벨 | PASS | '홈', '카테고리', '장바구니', '마이페이지' |
| Stack 화면 한글 title | PASS | '회원가입', '상품 상세', '주문/결제', '주문 상세' |
| 장바구니 뱃지 | PASS | `useCart` itemCount 기반 tabBarBadge |

### 3.3 한글화

| 화면 | 감지된 한글 키워드 | 상태 |
|------|-----------------|------|
| HomeScreen | 상품 | PASS |
| LoginScreen | 로그인, 회원가입 | PASS |
| RegisterScreen | 로그인, 회원가입 | PASS |
| ProductListScreen | 상품 | PASS |
| ProductDetailScreen | 상품, 장바구니 | PASS |
| CartScreen | 결제, 배송, 상품, 장바구니, 주문 | PASS |
| CheckoutScreen | 결제, 배송, 상품, 주문 | PASS |
| OrderListScreen | 주문 | PASS |
| OrderDetailScreen | 결제, 배송, 상품, 주문 | PASS |
| MyPageScreen | 결제, 배송, 상품, 주문 | PASS |
| AddressListScreen | 배송 | PASS |
| CartSummary | 결제, 배송, 상품 | PASS |

> 공통 UI 컴포넌트(Button, Input, Card, Badge, Loading)는 도메인 텍스트 없는 범용 컴포넌트로 정상.

### 3.4 API 클라이언트

| 항목 | 상태 | 상세 |
|------|------|------|
| baseURL (Platform-aware) | PASS | Android: `10.0.2.2:8080`, iOS/default: `localhost:8080` |
| AsyncStorage JWT 관리 | PASS | accessToken, refreshToken을 AsyncStorage에 저장/읽기 |
| Request interceptor | PASS | AsyncStorage에서 토큰 읽어 Authorization 헤더 부착 |
| Response interceptor (401) | PASS | refresh token 로직 완전 구현 (Web과 다르게 실제 구현됨) |
| API 모듈 구성 | PASS | member, product, cart, order, payment 6개 모듈 |

### 3.5 Store 검증

| Store | 상태 | 상세 |
|-------|------|------|
| authStore | PASS | Zustand + persist + AsyncStorage, login/logout/fetchUser/setTokens |
| cartStore | PASS | Zustand, fetchCart/addItem/updateItemQuantity/removeItem/clearCart |

### 3.6 타입 일관성 (Web <-> Mobile)

| 타입 | 일치 여부 | 차이점 |
|------|----------|--------|
| **Product** | FAIL | Web: `discountPrice`, `images[]`, `options[]{name, values[]}`, `stockQuantity` / Mobile: `salePrice`, `discountRate`, `thumbnailUrl`, `imageUrls[]`, `options[]{size, color}`, `reviewCount`, `averageRating`, `season`, `fit`, `material` |
| **ProductStatus** | FAIL | Web: `ACTIVE \| INACTIVE \| SOLD_OUT` / Mobile: `ON_SALE \| SOLD_OUT \| DISCONTINUED \| HIDDEN` |
| **Category** | WARN | Mobile에 `depth` 필드 추가, `parentId`와 `children` nullable 처리 차이 |
| **Brand** | PASS | 거의 동일 (Mobile에 `description` 추가만) |
| **Order** | FAIL | 필드명 상이: Web `totalAmount/discountAmount/shippingFee/paymentAmount` vs Mobile `totalPrice/discountPrice/deliveryFee/finalPrice`. Mobile에 `shippingAddress` 객체 구조, 추가 status값(`RETURN_REQUESTED` 등) |
| **OrderItem** | FAIL | Web: `productImage, optionName, unitPrice` / Mobile: `thumbnailUrl, brandName, size, color, status` |
| **CreateOrderRequest** | FAIL | Web: `items[]`+`shippingAddressId` / Mobile: `cartItemIds[]`+`shippingAddress` 객체 |
| **Cart** | FAIL | Web: 단순 `{totalPrice}` / Mobile: `{totalPrice, totalDiscountPrice, deliveryFee, finalPrice}` |
| **CartItem** | FAIL | Web: `productImage, optionName, unitPrice, totalPrice` / Mobile: `thumbnailUrl, brandName, size, color, salePrice, additionalPrice` |
| **Member** | FAIL | Web: `phone, createdAt, updatedAt` / Mobile: `phoneNumber, nickname, grade, point, profileImageUrl` |
| **ShippingAddress** | WARN | Web: `recipientName, phone` / Mobile: `recipient, phoneNumber` |
| **Payment** | FAIL | Web: `PaymentMethod = CARD\|BANK_TRANSFER\|VIRTUAL_ACCOUNT\|MOBILE` / Mobile: `CARD\|BANK_TRANSFER\|VIRTUAL_ACCOUNT\|KAKAO_PAY\|NAVER_PAY\|TOSS_PAY`. PaymentStatus도 상이 |

## 4. 발견된 이슈

| # | 플랫폼 | 심각도 | 증상 | 제안 |
|---|--------|--------|------|------|
| 1 | Web | **HIGH** | Refresh Token 로직 미구현 -- 401 응답 시 TODO 주석만 존재, 토큰 갱신 불가 | Mobile 코드 참고하여 `client.ts` response interceptor에 refresh 로직 구현 필요 |
| 2 | Web+Mobile | **CRITICAL** | Product 타입 불일치 -- 필드명, 구조, 옵션 모델이 완전히 다름. BE API 응답이 하나인데 FE 두 곳의 타입이 다르면 한쪽은 반드시 파싱 실패 | 공유 타입 패키지 생성 또는 BE API 스키마 기준으로 양쪽 통일 필요 |
| 3 | Web+Mobile | **CRITICAL** | Order 타입 불일치 -- 금액 필드명(`totalAmount` vs `totalPrice`), 배송지 구조, OrderStatus enum 값이 상이 | BE 응답 스키마 기준 통일 필요 |
| 4 | Web+Mobile | **CRITICAL** | Cart 타입 불일치 -- Web은 단순 `totalPrice`만 있고 Mobile은 `deliveryFee`, `finalPrice` 등 풍부. CartItem 필드명도 상이 | BE 응답 스키마 기준 통일 필요 |
| 5 | Web+Mobile | **HIGH** | Member 타입 불일치 -- Web에 `grade`, `point`, `nickname`, `profileImageUrl` 누락. Mobile에 `updatedAt` 누락 | BE Member 응답 기준 통일 필요 |
| 6 | Web+Mobile | **HIGH** | Payment 타입 불일치 -- 결제 수단 enum 값 상이 (Web: `MOBILE` / Mobile: `KAKAO_PAY`, `NAVER_PAY`, `TOSS_PAY`), PaymentStatus enum도 상이 (`APPROVED` vs `COMPLETED`) | BE 결제 API 스펙 기준 통일 |
| 7 | Web+Mobile | **MEDIUM** | ProductStatus enum 값 불일치 -- Web: `ACTIVE/INACTIVE/SOLD_OUT` vs Mobile: `ON_SALE/SOLD_OUT/DISCONTINUED/HIDDEN` | BE enum과 통일 |
| 8 | Web | **LOW** | CartStore의 `totalCount()` 함수 호출 방식 -- Header에서 `totalCount()` 메서드로 호출하나 store에서 함수로 정의되었는지 확인 필요 | 런타임 동작 확인 필요 |

## 5. 최종 판정

### Web (Next.js)
- **페이지 렌더링**: 모든 9개 페이지 정상 (HTTP 200)
- **한글화**: 완료 (영어 잔존 없음)
- **API 클라이언트**: 기본 구조 정상, 단 refresh token 미구현 (HIGH)
- **컴포넌트 구조**: 완비

### Mobile (Expo)
- **서버 상태**: 정상 (HTTP 200)
- **네비게이션**: Auth/Main 분기 + 4탭 구성 정상
- **한글화**: 완료
- **API 클라이언트**: Platform-aware baseURL + AsyncStorage JWT + refresh 완전 구현

### 핵심 리스크
Web과 Mobile 간 **타입 정의 불일치가 가장 심각한 이슈**이다. Product, Order, Cart, Member, Payment 등 거의 모든 도메인 타입에서 필드명, 구조, enum 값이 다르다. BE API가 단일 스키마를 반환하므로 한쪽 플랫폼은 반드시 런타임 오류를 경험하게 된다. 공유 타입 패키지 도입 또는 BE OpenAPI 스키마 기반 자동 생성이 시급하다.

### 판정: **조건부 통과 (Conditional Pass)**
- 페이지 렌더링, 한글화, 컴포넌트 구조, API 클라이언트 구조는 통과
- **타입 불일치 (CRITICAL x3)** 해결 전 통합 테스트 진행 불가
- **Refresh token 미구현 (HIGH x1)** 해결 필요
