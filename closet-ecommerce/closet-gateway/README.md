# closet-gateway

> Spring Cloud Gateway 기반 API Gateway (인증, 라우팅, CORS, Rate Limit)

## 역할

closet-gateway는 클라이언트 요청을 각 백엔드 서비스로 라우팅하는 API Gateway이다.
JWT 토큰 검증을 통한 인증, IP 기반 Rate Limiting, CORS 설정, 요청/응답 로깅을 담당한다.
인증 성공 시 `X-Member-Id` 헤더를 추가하여 다운스트림 서비스에 회원 식별자를 전달한다.

## 기술 스택

| 기술 | 용도 |
|------|------|
| Spring Cloud Gateway (WebFlux) | 리액티브 API 라우팅 |
| JJWT 0.12.3 | JWT 토큰 검증 |
| Spring Boot Actuator | 헬스체크, 메트릭 |
| Micrometer Prometheus | Prometheus 메트릭 수집 |
| kotlin-logging | 로깅 |

## 라우팅 규칙

| Route ID | URI | Path Predicates |
|----------|-----|-----------------|
| member-service | http://localhost:8081 | `/api/v1/members/**` |
| product-service | http://localhost:8082 | `/api/v1/products/**`, `/api/v1/categories/**`, `/api/v1/brands/**` |
| order-service | http://localhost:8083 | `/api/v1/orders/**`, `/api/v1/carts/**` |
| payment-service | http://localhost:8084 | `/api/v1/payments/**` |
| bff-service | http://localhost:8085 | `/api/v1/bff/**` |

## 인증 정책

인증 없이 접근 가능한 Public 엔드포인트:
- `POST /api/v1/members/register` -- 회원가입
- `POST /api/v1/members/login` -- 로그인
- `POST /api/v1/members/auth/refresh` -- 토큰 갱신
- `GET /api/v1/products/**` -- 상품 조회
- `GET /api/v1/categories/**` -- 카테고리 조회
- `GET /api/v1/brands/**` -- 브랜드 조회
- `GET /api/v1/bff/products/**` -- BFF 상품 조회
- `POST /api/v1/bff/auth/**` -- BFF 인증

상품/카테고리/브랜드/BFF 상품 경로는 GET 요청만 Public이다.

## 필터 체인

| 필터 | Order | 역할 |
|------|-------|------|
| RequestLoggingFilter | -2 | 요청/응답 로깅, 응답 시간 측정 |
| JwtAuthenticationFilter | -1 | JWT 검증, X-Member-Id 헤더 주입 |

## CORS 설정

- 허용 Origin: `http://localhost:3000`, `http://localhost:19006`
- 허용 Method: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Credentials: 허용
- Max Age: 3600초

## 패키지 구조

```
src/main/kotlin/com/closet/gateway/
├── config/     # CorsConfig, RateLimiterConfig
├── filter/     # JwtAuthenticationFilter, RequestLoggingFilter
└── ClosetGatewayApplication.kt
```

## 포트

- 서버 포트: 8080

## 의존 서비스

- closet-member (8081)
- closet-product (8082)
- closet-order (8083)
- closet-payment (8084)
- closet-bff (8085)
