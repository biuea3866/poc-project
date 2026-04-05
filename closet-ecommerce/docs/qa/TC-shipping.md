# Shipping 테스트 케이스

> 도메인: 배송/물류 (shipping-service)
> 작성일: 2026-04-05
> 총 15건

## 테스트 케이스

| TC-ID | 분류 | 시나리오 | 사전조건 | 절차 | 기대결과 | 우선순위 |
|-------|------|----------|----------|------|----------|----------|
| SHP-001 | 송장 등록 | 정상 송장 등록 | 주문 `ORD-001` 상태 PAID, 택배사 CJ대한통운 | 1. POST `/api/v1/shipping` (orderId, carrierId, trackingNumber) 호출 | 201 Created, 배송 상태 PREPARING, `event.closet.shipping` 이벤트 발행 | P1 |
| SHP-002 | 송장 등록 | 중복 송장 등록 | 주문 `ORD-001`에 이미 송장 등록됨 | 1. POST `/api/v1/shipping` (동일 orderId, 새 trackingNumber) 호출 | 409 Conflict, `SHIPPING_ALREADY_EXISTS` 에러 코드 | P1 |
| SHP-003 | 송장 등록 | 잘못된 송장번호 형식 | 주문 `ORD-002` 상태 PAID | 1. POST `/api/v1/shipping` (trackingNumber: "ABC") 호출 | 400 Bad Request, `INVALID_TRACKING_NUMBER` 에러 코드 | P2 |
| SHP-004 | 송장 등록 | 비PAID 상태 주문에 송장 등록 | 주문 `ORD-003` 상태 PENDING | 1. POST `/api/v1/shipping` (orderId: ORD-003) 호출 | 409 Conflict, `INVALID_ORDER_STATUS` 에러 코드 | P1 |
| SHP-005 | 배송 추적 | 정상 배송 추적 조회 | 주문 `ORD-001` 송장 등록 완료 | 1. GET `/api/v1/shipping/{shippingId}/tracking` 호출 | 200 OK, 배송 상태 이력(집화/이동중/배달중/완료) 반환 | P1 |
| SHP-006 | 배송 추적 | 캐시 적중 시 빠른 응답 | 동일 송장 5분 이내 재조회 | 1. GET `/api/v1/shipping/{shippingId}/tracking` 첫 호출 2. 5분 이내 재호출 | 두 번째 요청은 Redis 캐시에서 반환, 외부 API 미호출, 응답 시간 50ms 이내 | P2 |
| SHP-007 | 배송 추적 | 택배사 API 장애 시 fallback | 택배사 외부 API 다운 | 1. GET `/api/v1/shipping/{shippingId}/tracking` 호출 | 마지막 캐시된 추적 정보 반환, `lastUpdatedAt` 표시, 503이 아닌 200 반환 | P2 |
| SHP-008 | 자동 구매확정 | 배송완료 후 7일 자동 구매확정 | 주문 `ORD-001` 배송완료 7일 경과 | 1. 스케줄러 실행 | 주문 상태 CONFIRMED, `event.closet.order` 구매확정 이벤트 발행, 정산 대상 등록 | P1 |
| SHP-009 | 자동 구매확정 | 수동 구매확정 | 주문 `ORD-001` 배송완료 3일 경과 | 1. POST `/api/v1/shipping/{shippingId}/confirm` 호출 | 200 OK, 주문 상태 CONFIRMED, 자동 구매확정 스케줄 취소 | P2 |
| SHP-010 | 자동 구매확정 | 반품 신청 건 제외 | 주문 `ORD-001` 배송완료 7일 경과, 반품 신청 상태 | 1. 스케줄러 실행 | 자동 구매확정 미처리, 반품 프로세스 유지 | P2 |
| SHP-011 | 반품 | 정상 반품 신청 | 주문 `ORD-001` 배송완료 3일 경과 | 1. POST `/api/v1/shipping/{shippingId}/return` (reason, reasonDetail) 호출 | 201 Created, 반품 상태 RETURN_REQUESTED, `event.closet.shipping` 반품 이벤트 발행 | P1 |
| SHP-012 | 반품 | 사유별 반배송비 적용 | 반품 사유: 단순변심 | 1. POST `/api/v1/shipping/{shippingId}/return` (reason: CHANGE_OF_MIND) 호출 | 반품 배송비 3,000원 구매자 부담, 배송비 정보 응답에 포함 | P1 |
| SHP-013 | 반품 | 배송완료 7일 초과 반품 거부 | 주문 `ORD-001` 배송완료 8일 경과 | 1. POST `/api/v1/shipping/{shippingId}/return` 호출 | 409 Conflict, `RETURN_PERIOD_EXPIRED` 에러 코드 | P1 |
| SHP-014 | 교환 | 정상 교환 신청 | 주문 `ORD-001` 배송완료 3일 경과 | 1. POST `/api/v1/shipping/{shippingId}/exchange` (reason, newSkuId) 호출 | 201 Created, 교환 상태 EXCHANGE_REQUESTED, 교환 배송 정보 생성 | P1 |
| SHP-015 | 교환 | 교환 옵션 재고 확인 | 교환 희망 SKU 재고 0개 | 1. POST `/api/v1/shipping/{shippingId}/exchange` (newSkuId: SKU-002, 재고 0) 호출 | 409 Conflict, `EXCHANGE_OPTION_OUT_OF_STOCK` 에러 코드 | P2 |
