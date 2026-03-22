# Phase 1~4 구현 기록 총괄

> 기간: 2026-03-22 ~ 2026-03-23
> 서비스: 17개 (BE 15 + gateway + bff)
> 총 Kotlin 파일: 305개 (closet-common 11개 포함)
> 총 테스트 케이스: 258개 (Kotest BehaviorSpec)

## BE 구현 기록

### Phase 1: MVP (member, product, order, payment, gateway, bff)

| 서비스 | 구현 내용 | 테이블 수 | API 수 | 테스트 수 | 핵심 설계 결정 |
|--------|----------|----------|--------|----------|-------------|
| member | 회원 + JWT 인증 + 배송지 + 포인트 이력 | 3 | 10 | 4 | JWT Access/Refresh 이중 토큰, BCrypt 비밀번호 암호화, MemberGrade enum 등급 체계 |
| product | 상품 + 카테고리 + 브랜드 + 옵션(사이즈/색상) + SKU | 7 | 11 | 6 | QueryDSL 동적 필터, ProductStatus 상태 머신, 옵션-SKU 분리 모델 |
| order | 주문 + 주문항목 + 장바구니 | 10 | 8 | 27 | OrderStatus 9단계 상태 머신 (canTransitionTo), 주문번호 자동 생성, 부분 취소 지원 |
| payment | 결제 + 환불 | 2 | 3 | 0 | PaymentGateway ACL 패턴 (MockToss 구현체), 멱등성 키 기반 중복 방지 |
| gateway | API Gateway (라우팅 + 인증 + CORS + 레이트리밋) | 0 | 0 | 0 | Spring Cloud Gateway WebFlux, JWT 필터 체인, 서비스별 레이트리밋 설정 |
| bff | 서비스 오케스트레이션 (6개 컨트롤러) | 0 | 40 | 0 | OpenFeign + Virtual Thread, CompletableFuture 병렬 호출, Auth/Product/Order/Cart/Address/MyPage BFF |

**Phase 1 소계**: 테이블 22, API 72, 테스트 37

### Phase 2: 성장 (shipping, inventory, search, review)

| 서비스 | 구현 내용 | 테이블 수 | API 수 | 테스트 수 | 핵심 설계 결정 |
|--------|----------|----------|--------|----------|-------------|
| shipping | 배송 + 택배사 연동 + 반품/교환 물류 | 6 | 7 | 40 | ShippingStatus 상태 머신, 택배사 추상화 ACL, 묶음 배송 지원 |
| inventory | 재고 관리 + 입출고 + 안전재고 | 4 | 6 | 24 | 옵션별 재고 분리, 비관적 락 기반 재고 차감, 안전재고 알림 임계값 |
| search | Elasticsearch 기반 상품 검색 + 필터 + 자동완성 | 0 | 4 | 7 | OpenSearch 인덱스 설계, 카테고리/브랜드/가격/색상/사이즈 복합 필터 |
| review | 상품 리뷰 + 별점 + 포토/영상 리뷰 | 2 | 6 | 9 | ReviewStatus 상태 머신, 별점 집계 비정규화, 리뷰 보상 포인트 |

**Phase 2 소계**: 테이블 12, API 23, 테스트 80

### Phase 3: 확장 (promotion, display, seller, cs)

| 서비스 | 구현 내용 | 테이블 수 | API 수 | 테스트 수 | 핵심 설계 결정 |
|--------|----------|----------|--------|----------|-------------|
| promotion | 쿠폰 + 할인 + 적립금 + 타임세일 | 8 | 9 | 40 | CouponType/DiscountType enum 체계, 쿠폰 발급 한도 관리, 중복 적용 규칙 |
| display | 메인 페이지 + 기획전 + 배너 + 랭킹 | 4 | 12 | 13 | DisplayStatus 상태 머신, 노출 우선순위 정렬, 시즌별 기획전 관리 |
| seller | 브랜드 입점 + 셀러 관리 + 셀러 어드민 | 3 | 8 | 11 | SellerStatus 입점 심사 상태 머신, 수수료율 셀러별 관리 |
| cs | 1:1 문의 + FAQ + 반품/교환/환불 접수 | 3 | 9 | 14 | InquiryStatus/ClaimStatus 이중 상태 머신, FAQ 카테고리 분류 |

**Phase 3 소계**: 테이블 18, API 38, 테스트 78

### Phase 4: 고도화 (settlement, notification, content)

| 서비스 | 구현 내용 | 테이블 수 | API 수 | 테스트 수 | 핵심 설계 결정 |
|--------|----------|----------|--------|----------|-------------|
| settlement | 셀러 정산 + 수수료 계산 + 정산 주기 | 6 | 7 | 38 | SettlementStatus 4단계 상태 머신 (PENDING->CALCULATED->CONFIRMED->PAID), 수수료 자동 계산 |
| notification | 이메일/SMS/푸시 알림 + 재입고 알림 | 3 | 10 | 10 | NotificationType enum, 채널별 발송 추상화, 알림 템플릿 관리 |
| content | 스타일 매거진 + 코디 추천 + OOTD 스냅 | 5 | 13 | 15 | ContentStatus 상태 머신, 태그 기반 분류, 시즌 컬렉션 연동 |

**Phase 4 소계**: 테이블 14, API 30, 테스트 63

### BE 총합

| 항목 | 수량 |
|------|------|
| 서비스 | 17개 |
| 테이블 (엔티티) | 66개 |
| API 엔드포인트 | 163개 |
| 테스트 케이스 | 258개 |
| Kotlin 파일 | 305개 |
| ADR 문서 | 8개 |

## FE 구현 기록

### Web (Next.js — closet-web)

| 항목 | 수량 | 세부 |
|------|------|------|
| 페이지 | 10개 | 홈, 상품목록, 상품상세, 로그인, 회원가입, 장바구니, 주문목록, 주문상세, 마이페이지, 배송지관리 |
| 컴포넌트 | 11개 | 상품카드, 장바구니, 주문 관련 UI 컴포넌트 |
| API 연동 | 6개 | client, member, product, order, cart, payment |
| Zustand Store | 2개 | authStore, cartStore |
| 총 TS/TSX 파일 | 3,041개 (node_modules 포함) |

### Mobile (Expo React Native — closet-mobile)

| 항목 | 수량 | 세부 |
|------|------|------|
| 스크린 | 11개 | Home, Login, Register, ProductList, ProductDetail, Cart, Checkout, OrderList, OrderDetail, MyPage, AddressList |
| 컴포넌트 | 12개 | ProductCard, SizeGuide, ProductOptionSelector, OrderStatusBadge, OrderItemRow, CartItemRow, CartSummary, Card, Badge, Loading, Button, Input |
| 네비게이터 | 3개 | RootNavigator, AuthNavigator, MainTabNavigator |
| Zustand Store | 2개 | authStore, cartStore |

## DevOps 구현 기록

| 항목 | 수량 | 세부 |
|------|------|------|
| Docker Compose | 2개 | 메인 (docker-compose.yml), SigNoz (signoz/docker-compose.yml) |
| Docker 서비스 | 24개 | 17 앱 서비스 + MySQL + Redis + Kafka + Exporter 등 |
| Exporter | 4개 | MySQL, Redis, Kafka, Node |
| CI/CD | 1개 | GitHub Actions ci.yml |
| 모니터링 스택 | 4종 | Grafana + Prometheus + Tempo + SigNoz |
| 모니터링 문서 | 4개 | Grafana/Prometheus, Tempo, SigNoz, Pinpoint, Zipkin 설정 가이드 |
| Pinpoint | 1세트 | APM 에이전트 설정 |

## QA 구현 기록

| 항목 | 수량 | 세부 |
|------|------|------|
| 단위 테스트 파일 | 31개 | Kotest BehaviorSpec (Given/When/Then) |
| 테스트 케이스 총수 | 258개 | member(4), product(6), order(27), shipping(40), inventory(24), search(7), review(9), promotion(40), display(13), seller(11), cs(14), settlement(38), notification(10), content(15) |
| QA 검증 리포트 | 8개 | `.analysis/verification/results/` |
| k6 시나리오 | 15개 | realistic-traffic.js (15 시나리오), spike-traffic.js (1 시나리오) |
| k6 시나리오 목록 | — | windowShopper, comparisonShopper, impulseBuyer, cartAbandoner, newMember, loyalBuyer, repeatVisitor, bulkBuyer, sellerOperations, reviewWriter, shippingManager, searchUser, inventoryManager, orderCanceller, bffUser |

## PM 구현 기록

| 항목 | 수량 | 세부 |
|------|------|------|
| PRD 문서 | 10개 | `.analysis/prd/results/` (Phase 1~5 기획서) |
| API 계약서 | 1개 | `docs/api/api-contract.md` |
| 에러 코드 정의 | 1개 | `docs/error-codes.md` |
| 기술 부채 목록 | 1개 | `docs/tech-debt.md` |

## TechLead 구현 기록

| ADR | 제목 |
|-----|------|
| ADR-001 | 멀티 모듈 모놀리스 아키텍처 선택 |
| ADR-002 | JPA + QueryDSL 채택 |
| ADR-003 | Flyway 마이그레이션 전략 |
| ADR-004 | JWT 인증 방식 |
| ADR-005 | Payment ACL 패턴 |
| ADR-006 | 주문 상태 머신 설계 |
| ADR-007 | Soft Delete 전략 |
| ADR-008 | 에러 코드 체계 |
