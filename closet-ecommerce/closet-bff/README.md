# closet-bff

> Backend For Frontend -- 프론트엔드 화면 단위로 여러 마이크로서비스를 오케스트레이션하는 BFF 서비스

## 역할

closet-bff는 프론트엔드(웹/모바일) 화면에 최적화된 API를 제공하는 BFF(Backend For Frontend) 서비스이다.
OpenFeign 클라이언트로 member, product, order, payment 서비스를 호출하여 화면 단위로 데이터를 조합한다.
홈 화면, 상품 상세, 장바구니, 체크아웃, 주문/결제, 마이페이지, 배송지 관리 등의 Facade를 제공한다.
DB에 직접 연결하지 않으며(DataSource/JPA/Flyway AutoConfiguration 제외), 순수 오케스트레이션 계층이다.

## 기술 스택

| 기술 | 용도 |
|------|------|
| Spring Boot Starter Web | REST API |
| Spring Cloud OpenFeign | 마이크로서비스 간 HTTP 통신 (Feign Client) |
| Spring Boot Actuator | 헬스체크, 메트릭 |
| Jackson Module Kotlin | JSON 직렬화/역직렬화 |
| Kotest 5.8.0 | 테스트 프레임워크 |
| MockK 1.13.8 | Mocking 라이브러리 |
| Virtual Threads | 가상 스레드 활성화 |

## Feign Client 연결

| Client | 대상 서비스 | Base URL |
|--------|-----------|----------|
| MemberServiceClient | closet-member | http://localhost:8081/api/v1 |
| ProductServiceClient | closet-product | http://localhost:8082/api/v1 |
| OrderServiceClient | closet-order | http://localhost:8083/api/v1 |
| PaymentServiceClient | closet-payment | http://localhost:8084/api/v1 |

Feign 기본 설정: connect-timeout 5000ms, read-timeout 10000ms.

## API

| Method | Path | 설명 |
|--------|------|------|
| POST | /api/v1/bff/auth/register | 회원가입 (-> member-service) |
| POST | /api/v1/bff/auth/login | 로그인 (-> member-service) |
| GET | /api/v1/bff/home | 홈 화면 데이터 (상품 목록 + 카테고리 + 브랜드) |
| GET | /api/v1/bff/products/{id} | 상품 상세 (-> product-service) |
| POST | /api/v1/bff/cart/items | 장바구니 항목 추가 (-> order-service) |
| PUT | /api/v1/bff/cart/items/{itemId} | 장바구니 수량 변경 (-> order-service) |
| DELETE | /api/v1/bff/cart/items/{itemId} | 장바구니 항목 삭제 (-> order-service) |
| GET | /api/v1/bff/checkout | 체크아웃 화면 (장바구니 + 배송지 + 회원 정보 조합) |
| POST | /api/v1/bff/orders | 주문 생성 (-> order-service) |
| GET | /api/v1/bff/orders/{id} | 주문 상세 (-> order-service + payment-service) |
| POST | /api/v1/bff/orders/{orderId}/pay | 결제 승인 (-> payment-service) |
| POST | /api/v1/bff/orders/{orderId}/cancel | 주문 취소 (-> order-service) |
| GET | /api/v1/bff/mypage | 마이페이지 (회원 정보 + 최근 주문) |
| POST | /api/v1/bff/addresses | 배송지 추가 (-> member-service) |
| PUT | /api/v1/bff/addresses/{id} | 배송지 수정 (-> member-service) |
| DELETE | /api/v1/bff/addresses/{id} | 배송지 삭제 (-> member-service) |
| PATCH | /api/v1/bff/addresses/{id}/default | 기본 배송지 설정 (-> member-service) |

## 패키지 구조

```
src/main/kotlin/com/closet/bff/
├── client/            # MemberServiceClient, ProductServiceClient, OrderServiceClient, PaymentServiceClient
├── config/            # FeignConfig
├── dto/               # BffResponses, ProductSearchParams, 각종 Request/Response DTO
├── facade/            # AuthBffFacade, HomeBffFacade, ProductBffFacade, CartBffFacade, OrderBffFacade, MyPageBffFacade, AddressBffFacade
└── presentation/      # BffAuthController, BffProductController, BffCartController, BffOrderController, BffMyPageController, BffAddressController
```

## 포트

- 서버 포트: 8085

## 의존 서비스

- closet-common (공통 라이브러리)
- closet-member (8081) -- Feign
- closet-product (8082) -- Feign
- closet-order (8083) -- Feign
- closet-payment (8084) -- Feign
