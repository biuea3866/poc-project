# Phase 2 QA 테스트 케이스

> 작성일: 2026-04-04
> 프로젝트: Closet E-commerce Phase 2 (배송 + 재고 + 검색 + 리뷰)
> 작성자: QA Lead
> 총 테스트 케이스: 105건

---

## 1. 배송 도메인 (US-501 ~ US-505)

### 1.1 US-501: 송장 등록 + 주문 상태 변경

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-001 | 시스템 자동 채번으로 송장 등록 성공 | PAID 상태 주문 존재, 판매자 인증 | POST /api/v1/shippings (orderId, carrier=CJ_LOGISTICS) | 201 Created, status=READY, 주문 상태 PAID->PREPARING, Kafka order.status.changed 이벤트 발행 | P0 |
| SH-002 | 판매자 수동 송장번호 입력으로 등록 성공 | PAID 상태 주문, 유효한 송장번호 | POST /api/v1/shippings (orderId, carrier, trackingNumber="1234567890123") | 201 Created, 입력된 trackingNumber 그대로 저장 | P0 |
| SH-003 | 택배사별 송장 등록 (CJ/로젠/롯데/우체국) | PAID 상태 주문 4건 | 각 택배사 코드로 POST /api/v1/shippings | 4건 모두 201 Created, 각 택배사에 맞는 CarrierAdapter 호출 확인 | P0 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-004 | 이미 송장 등록된 주문에 중복 등록 | PREPARING 상태 주문 (송장 등록 완료) | POST /api/v1/shippings (동일 orderId) | 409 Conflict | P0 |
| SH-005 | PAID가 아닌 상태에서 송장 등록 | PENDING 상태 주문 | POST /api/v1/shippings | 400 Bad Request (상태 전이 불가) | P0 |
| SH-006 | 잘못된 송장번호 형식 (숫자 미만) | PAID 상태 주문 | trackingNumber="12345" (10자리 미만) | 400 Bad Request (형식 오류) | P0 |
| SH-007 | 잘못된 송장번호 형식 (특수문자 포함) | PAID 상태 주문 | trackingNumber="123-456-789" | 400 Bad Request (형식 오류: `[A-Z]{0,4}[0-9]{10,15}` 위반) | P1 |
| SH-008 | 다른 판매자의 주문에 송장 등록 | 판매자A 인증, 판매자B의 주문 | POST /api/v1/shippings | 403 Forbidden | P0 |
| SH-009 | 존재하지 않는 orderId | - | POST /api/v1/shippings (orderId=999999) | 404 Not Found | P1 |
| SH-010 | 유효하지 않은 택배사 코드 | PAID 상태 주문 | carrier="INVALID_CARRIER" | 400 Bad Request | P1 |

#### 경계 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-011 | 송장번호 최소 길이 (10자리) | PAID 상태 주문 | trackingNumber="1234567890" | 201 Created | P1 |
| SH-012 | 송장번호 최대 길이 (15자리) | PAID 상태 주문 | trackingNumber="123456789012345" | 201 Created | P1 |
| SH-013 | 송장번호 알파벳 prefix 포함 | PAID 상태 주문 | trackingNumber="CJ1234567890" | 201 Created (PD-08 확정) | P1 |

### 1.2 US-502: 택배사 API 연동 배송 추적

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-014 | 배송 추적 조회 성공 (shippingId) | 송장 등록된 배송 건, Redis 캐시 비어있음 | GET /api/v1/shippings/{shippingId}/tracking | 200 OK, 택배사 Mock API 호출, trackingLogs 반환, Redis 캐시 저장 (TTL 5분) | P0 |
| SH-015 | orderId 기반 배송 추적 조회 | 송장 등록된 배송 건 | GET /api/v1/shippings?orderId={orderId} | 200 OK, orderId -> shippingId 변환 없이 직접 조회 (PD-44) | P1 |
| SH-016 | Redis 캐시 hit 시 택배사 API 미호출 | 5분 이내 캐시된 추적 정보 존재 | GET /api/v1/shippings/{shippingId}/tracking | 200 OK, 캐시된 정보 반환, Mock API 호출 0회 | P1 |
| SH-017 | 배송 상태 ACCEPTED -> READY 매핑 | Mock 서버 응답 status=ACCEPTED | 배송 추적 조회 | currentStatus=READY (PD-10 매핑) | P0 |
| SH-018 | 배송 상태 IN_TRANSIT/OUT_FOR_DELIVERY -> IN_TRANSIT 매핑 | Mock 서버 응답 status=OUT_FOR_DELIVERY | 배송 추적 조회 | currentStatus=IN_TRANSIT, shipping_tracking_log에 원본 상태 저장 | P0 |
| SH-019 | 배송 상태 DELIVERED -> DELIVERED 매핑 + 주문 상태 갱신 | Mock 서버 응답 status=DELIVERED | 배송 추적 조회 | currentStatus=DELIVERED, shipping.delivered_at 설정, 주문 상태 SHIPPED->DELIVERED | P0 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-020 | 택배사 API 장애 시 캐시 폴백 | Redis 캐시에 이전 조회 결과 존재, Mock API 500 에러 | GET /api/v1/shippings/{shippingId}/tracking | 200 OK, 캐시된 정보 반환, ERROR 레벨 로깅 | P0 |
| SH-021 | 택배사 API 장애 + 캐시 미스 | Redis 캐시 비어있음, Mock API 500 에러 | GET /api/v1/shippings/{shippingId}/tracking | 503 Service Unavailable 또는 DB 저장 이력 반환 | P1 |
| SH-022 | 존재하지 않는 shippingId 조회 | - | GET /api/v1/shippings/999999/tracking | 404 Not Found | P1 |
| SH-023 | 다른 사용자의 배송 정보 조회 | 구매자A 인증, 구매자B의 배송 건 | GET /api/v1/shippings/{shippingId}/tracking | 403 Forbidden | P0 |

### 1.3 US-503: 자동 구매확정

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-024 | 수동 구매확정 성공 | DELIVERED 상태 주문 (7일 이전) | POST /api/v1/orders/{orderId}/confirm | 200 OK, status=CONFIRMED, Kafka order.confirmed 이벤트 발행 | P0 |
| SH-025 | 자동 구매확정 배치 실행 | delivered_at이 168시간 이전인 주문 3건 | 배치 스케줄러 실행 (매일 00:00, 12:00) | 3건 모두 CONFIRMED, 각 건에 order.confirmed 이벤트 발행 | P0 |
| SH-026 | 반품 진행 중 자동확정 제외 | delivered_at 168시간 초과, 반품 status=REQUESTED | 자동 구매확정 배치 | 해당 건 스킵 (CONFIRMED 미전이) | P0 |
| SH-027 | 교환 진행 중 자동확정 제외 | delivered_at 168시간 초과, 교환 status=PICKUP_SCHEDULED | 자동 구매확정 배치 | 해당 건 스킵 | P0 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-028 | 이미 CONFIRMED인 주문 수동 확정 | CONFIRMED 상태 주문 | POST /api/v1/orders/{orderId}/confirm | 400 Bad Request (이미 확정) | P1 |
| SH-029 | DELIVERED가 아닌 상태에서 확정 시도 | SHIPPED 상태 주문 | POST /api/v1/orders/{orderId}/confirm | 400 Bad Request (상태 전이 불가) | P1 |

#### 경계 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-030 | 168시간 정확히 경과 시점 | delivered_at = NOW() - 168시간 정확히 | 자동 구매확정 배치 | CONFIRMED (경계값 포함) | P0 |
| SH-031 | 167시간 59분 경과 (미달) | delivered_at = NOW() - 167시간 59분 | 자동 구매확정 배치 | 스킵 (미도달) | P0 |
| SH-032 | D-1 사전 알림 이벤트 발행 | delivered_at = NOW() - 144시간 (D-1 시점) | D-1 알림 배치 | Kafka 이벤트 발행 (PD-45) | P1 |

### 1.4 US-504: 반품 신청 + 수거 + 검수 + 환불

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-033 | DEFECTIVE 반품 신청 (판매자 부담) | DELIVERED 상태, 7일 이내 | POST /api/v1/returns (reason=DEFECTIVE) | 201 Created, shippingFeeBearer=SELLER, shippingFee=0 | P0 |
| SH-034 | WRONG_ITEM 반품 신청 (판매자 부담) | DELIVERED 상태, 7일 이내 | POST /api/v1/returns (reason=WRONG_ITEM) | 201 Created, shippingFeeBearer=SELLER, shippingFee=0 | P0 |
| SH-035 | SIZE_MISMATCH 반품 신청 (구매자 부담) | DELIVERED 상태, 7일 이내 | POST /api/v1/returns (reason=SIZE_MISMATCH) | 201 Created, shippingFeeBearer=BUYER, shippingFee=3000 | P0 |
| SH-036 | CHANGE_OF_MIND 반품 신청 (구매자 부담) | DELIVERED 상태, 7일 이내 | POST /api/v1/returns (reason=CHANGE_OF_MIND) | 201 Created, shippingFeeBearer=BUYER, shippingFee=3000 | P0 |
| SH-037 | 반품 전체 흐름 (승인) | 반품 REQUESTED 상태 | REQUESTED->PICKUP_SCHEDULED->PICKUP_COMPLETED->INSPECTING->APPROVED | 각 상태 전이 성공, APPROVED 시 결제 환불 API 호출 (PD-12: 결제금액 - 3000원) + 재고 복구 이벤트 | P0 |
| SH-038 | 반품 검수 거절 | 반품 INSPECTING 상태 | PATCH /api/v1/returns/{id}/reject (rejectReason 필수) | status=REJECTED, 거절 사유 저장, 알림 이벤트 발행 | P1 |
| SH-039 | 반품 검수 3영업일 초과 자동 승인 | 반품 INSPECTING 상태, 3영업일 경과 | 자동 승인 배치 | status=APPROVED, 환불 처리 (PD-13) | P1 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-040 | 7일 초과 반품 신청 | delivered_at + 169시간 경과 | POST /api/v1/returns | 400 Bad Request (기간 만료) | P0 |
| SH-041 | 구매확정 후 반품 신청 | CONFIRMED 상태 주문 | POST /api/v1/returns | 400 Bad Request (구매확정 후 반품 불가) | P0 |
| SH-042 | 반품 사유 미선택 | DELIVERED 상태, 7일 이내 | POST /api/v1/returns (reason 누락) | 400 Bad Request (필수 필드) | P1 |
| SH-043 | PG 환불 API 실패 시 재시도 | 반품 APPROVED, PG API 500 에러 | 환불 처리 | 반품 상태 APPROVED 유지, 환불 재시도 큐 등록, ERROR 로깅 | P0 |

#### 경계 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-044 | 168시간 직전 반품 신청 | delivered_at + 167시간 59분 | POST /api/v1/returns | 201 Created (기한 내) | P0 |
| SH-045 | 168시간 만료 시점에 반품 접수 중이면 구매확정 배제 | delivered_at + 168시간, 반품 status=REQUESTED | 자동 구매확정 배치 | 스킵 (반품 우선, PD-16) | P0 |

### 1.5 US-505: 교환 신청

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-046 | 사이즈 교환 신청 성공 (단순변심) | DELIVERED 7일 이내, 교환 옵션 재고 있음, 동일 가격 | POST /api/v1/exchanges (reason=SIZE_MISMATCH) | 201 Created, shippingFee=6000(왕복), 교환 옵션 재고 RESERVE | P1 |
| SH-047 | 불량 사유 교환 (판매자 부담) | DELIVERED, reason=DEFECTIVE | POST /api/v1/exchanges | 201 Created, shippingFeeBearer=SELLER, shippingFee=0 | P1 |
| SH-048 | 교환 전체 흐름 | 교환 REQUESTED 상태 | REQUESTED->PICKUP_SCHEDULED->PICKUP_COMPLETED->RESHIPPING->COMPLETED | 각 상태 전이 성공, PICKUP_COMPLETED 시 기존 옵션 재고 복구, COMPLETED 시 교환 옵션 재고 DEDUCT | P1 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SH-049 | 교환 희망 옵션 재고 부족 | 교환 옵션 available_quantity=0 | POST /api/v1/exchanges | 400 Bad Request (재고 부족) | P1 |
| SH-050 | 동일 상품이 아닌 옵션으로 교환 | 다른 productId의 optionId | POST /api/v1/exchanges | 400 Bad Request (동일 상품만 교환 가능) | P1 |
| SH-051 | 7일 초과 교환 신청 | delivered_at + 169시간 경과 | POST /api/v1/exchanges | 400 Bad Request (기간 만료) | P1 |
| SH-052 | 가격 차이 있는 옵션으로 교환 시도 | 교환 옵션의 additionalPrice != 기존 옵션 | POST /api/v1/exchanges | 400 Bad Request (동일 가격만 교환 허용, PD-14) | P1 |
| SH-053 | 수거 중 교환 옵션 재고 소진 | 교환 PICKUP_SCHEDULED 중 재고 0 | 재고 소진 이벤트 | 교환 취소 -> 반품 전환 안내 | P1 |

---

## 2. 재고 도메인 (US-601 ~ US-604)

### 2.1 US-601: SKU별 재고 관리

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-001 | 주문 생성 시 재고 RESERVE | 재고: total=100, available=100, reserved=0 | Kafka order.created (quantity=2) | available=98, reserved=2, inventory_history (type=RESERVE) | P0 |
| INV-002 | 결제 완료 시 재고 DEDUCT | 재고: total=100, available=98, reserved=2 | Kafka payment.completed (quantity=2) | total=98, available=98, reserved=0, inventory_history (type=DEDUCT) | P0 |
| INV-003 | 결제 실패/취소 시 재고 RELEASE | 재고: total=100, available=98, reserved=2 | Kafka payment.failed (quantity=2) | available=100, reserved=0, inventory_history (type=RELEASE) | P0 |
| INV-004 | 반품 완료 시 재고 RESTORE | 재고: total=98, available=98 | Kafka return.approved (quantity=2) | total=100, available=100, inventory_history (type=RESTORE) | P0 |
| INV-005 | 입고(INBOUND) API | 재고: total=100, available=100 | POST /api/v1/inventories/{id}/inbound (quantity=50) | total=150, available=150, inventory_history (type=INBOUND) | P0 |
| INV-006 | 재고 조회 | productId로 등록된 재고 | GET /api/v1/inventories?productId={id} | 200 OK, SKU별 total/available/reserved 수량 |P1 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-007 | 재고 부족 시 RESERVE 거절 | available=1, 주문 quantity=2 | Kafka order.created | inventory.insufficient 이벤트 발행, 부족 SKU 정보 포함 | P0 |
| INV-008 | 복수 SKU All-or-Nothing (일부 부족) | SKU-A: available=10, SKU-B: available=0 | 주문(SKU-A:1, SKU-B:1) | 전체 주문 거절, SKU-A도 RESERVE 하지 않음 (PD-19) | P0 |
| INV-009 | 재고 음수 방지 | available=0 | DEDUCT (quantity=1) | 차감 거부, 에러 | P0 |
| INV-010 | Kafka 이벤트 중복 수신 (멱등성) | 동일 eventId로 order.created 2회 발행 | Consumer 처리 | processed_event 테이블 UK로 2회차 스킵 (PD-04) | P0 |

#### 경계 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-011 | 재고 정확히 0으로 차감 | available=2, quantity=2 | RESERVE | available=0, reserved=2, out_of_stock 이벤트 발행 | P0 |
| INV-012 | 15분 TTL 만료 자동 RELEASE | RESERVE 후 15분 경과, 결제 미완료 | TTL 만료 스케줄러 | reserved 복구 -> available 증가, 주문 자동 취소 (PD-18) | P0 |

### 2.2 US-602: Redis 분산 락

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-013 | 분산 락 기본 동작 | 재고 available=100, 락 미점유 | 재고 차감 요청 | 락 획득 (inventory:lock:{sku}) -> 차감 -> 락 해제 | P0 |
| INV-014 | 100스레드 동시 차감 정합성 | available=100, 100스레드 동시 차감 (각 1개) | 동시 실행 | 최종 available=0, 성공 100건, 실패 0건, 오버셀링 0건 | P0 |
| INV-015 | 100스레드 동시 차감 (재고 부족) | available=50, 100스레드 동시 차감 (각 1개) | 동시 실행 | 성공 50건, 실패 50건 (재고 부족), available=0 | P0 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-016 | 락 획득 실패 (5초 타임아웃) | 다른 프로세스가 락 점유 중 (5초 이상) | 재고 차감 요청 | 3회 재시도 후 실패 응답 | P1 |
| INV-017 | Redis 장애 시 낙관적 락 폴백 | Redis 연결 불가 | 재고 차감 요청 | @Version 기반 낙관적 락으로 처리 | P1 |
| INV-018 | 복수 SKU 락 순서 정렬 (데드락 방지) | SKU-A, SKU-B 동시 주문 | Thread1: SKU-A,B / Thread2: SKU-B,A | 둘 다 SKU 코드 오름차순으로 락 획득, 데드락 미발생 | P1 |

### 2.3 US-603: 안전재고 알림

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-019 | 안전재고 이하 알림 발행 | safety_stock=10, 차감 후 available=9 | 재고 차감 | Kafka inventory.low_stock 이벤트 발행 | P1 |
| INV-020 | 재고 0 알림 발행 | 차감 후 available=0 | 재고 차감 | Kafka inventory.out_of_stock 이벤트 발행 | P1 |
| INV-021 | 카테고리별 안전재고 기본값 | 상의/하의 SKU (기본값 10), 아우터 SKU (기본값 5), 신발 SKU (기본값 8), 액세서리 SKU (기본값 15) | 안전재고 미설정 SKU 등록 | 카테고리별 기본값 자동 적용 (PD-20) | P1 |
| INV-022 | SKU별 안전재고 수동 오버라이드 | safety_stock=10인 SKU | PATCH /api/v1/inventories/{id}/safety-stock (safetyStock=20) | 200 OK, safetyStock=20 | P1 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-023 | 24시간 내 중복 알림 방지 | 동일 SKU 24시간 내 알림 발행 이력 존재 (Redis 키) | 재고 차감 후 안전재고 이하 | 중복 알림 미발행 | P1 |
| INV-024 | 24시간 경과 후 재알림 | 동일 SKU 24시간 이전 알림 이력 (Redis 키 만료) | 재고 차감 후 안전재고 이하 | 알림 재발행 | P1 |

### 2.4 US-604: 재입고 알림

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-025 | 재입고 알림 신청 | 품절 상품(available=0) | POST /api/v1/restock-notifications (productId, optionId) | 201 Created, status=WAITING | P2 |
| INV-026 | 재입고 시 알림 발행 | WAITING 상태 알림 3건, 해당 SKU 입고 | POST /api/v1/inventories/{id}/inbound | 3건 모두 status=NOTIFIED, Kafka inventory.restock_notification 이벤트 3건 | P2 |
| INV-027 | 알림 신청 취소 | WAITING 상태 알림 | DELETE /api/v1/restock-notifications/{id} | 204 No Content | P2 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-028 | 50건 초과 신청 제한 | 회원의 WAITING 알림 50건 | POST /api/v1/restock-notifications | 400 Bad Request (최대 50건 초과) | P2 |
| INV-029 | 재고 있는 상품에 알림 신청 방지 | available > 0인 상품 | POST /api/v1/restock-notifications | 400 Bad Request (품절 상품만 신청 가능) | P2 |
| INV-030 | 동일 상품/옵션 중복 신청 방지 | 동일 memberId+productId+optionId WAITING 존재 | POST /api/v1/restock-notifications | 409 Conflict | P2 |

#### 경계 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| INV-031 | 90일 자동 만료 | WAITING 상태 알림, created_at + 90일 경과 | 만료 배치 | status=EXPIRED (PD-21) | P2 |
| INV-032 | 50번째 알림 신청 (경계) | 회원의 WAITING 알림 49건 | POST /api/v1/restock-notifications | 201 Created (50건째 허용) | P2 |

---

## 3. 검색 도메인 (US-701 ~ US-705)

### 3.1 US-701: ES 상품 인덱싱

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-001 | 상품 생성 이벤트 -> ES 인덱싱 | product.created Kafka 이벤트 | Consumer 처리 | closet-products 인덱스에 문서 생성, 3초 이내 검색 가능 | P1 |
| SE-002 | 상품 수정 이벤트 -> ES 업데이트 | product.updated Kafka 이벤트 | Consumer 처리 | 기존 문서 업데이트 | P1 |
| SE-003 | 상품 삭제 이벤트 -> ES 삭제 | product.deleted Kafka 이벤트 | Consumer 처리 | 기존 문서 삭제 | P1 |
| SE-004 | 벌크 인덱싱 (관리자) | 상품 10만건 DB 존재 | POST /api/v1/search/reindex (admin) | 202 Accepted, 5분 이내 완료 (PD-28) | P1 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-005 | ES 장애 시 DLQ 저장 | ES 클러스터 다운 | product.created 이벤트 수신 | DLQ에 저장, 알림 발행 | P1 |
| SE-006 | DLQ 재처리 (3회 지수 백오프) | DLQ에 실패 메시지 | 재처리 트리거 | 1분/5분/30분 간격 3회 재시도, 최종 실패 시 수동 큐 (PD-31) | P1 |
| SE-007 | 이벤트 멱등성 (중복 인덱싱 방지) | 동일 eventId로 product.created 2회 수신 | Consumer 처리 | 1회만 인덱싱, 2회차 스킵 | P1 |

### 3.2 US-702: 한글 형태소 검색

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-008 | 기본 키워드 검색 | "반팔 티셔츠" 인덱싱된 상품 | GET /api/v1/search/products?q=반팔 | 200 OK, 반팔 포함 상품 반환 | P1 |
| SE-009 | 복합어 분리 검색 | "반팔티셔츠" 상품명 | q=반팔 또는 q=티셔츠 | 두 검색어 모두 해당 상품 반환 (nori 분리) | P1 |
| SE-010 | 유의어 검색 | "바지" 유의어 = "팬츠" | q=팬츠 | "바지" 포함 상품도 반환 (PD-24) | P2 |
| SE-011 | 정렬 - 최신순 | 상품 A(created 3일전), B(created 1일전) | q=티셔츠&sort=LATEST | B가 A보다 먼저 반환 | P1 |
| SE-012 | 정렬 - 가격 오름차순 | 상품 A(10,000원), B(5,000원) | q=티셔츠&sort=PRICE_ASC | B가 A보다 먼저 반환 | P1 |
| SE-013 | 정렬 - 인기순 (Sprint 5 판매량) | 상품 A(판매량 100), B(판매량 50) | q=티셔츠&sort=POPULAR | A가 B보다 먼저 반환 | P1 |
| SE-014 | 오타 교정 제안 | "반팔 티셔추" (오타) | q=반팔 티셔추 | suggestion 필드에 "반팔 티셔츠" 제안 (ES fuzzy, PD-26) | P2 |
| SE-015 | 검색 결과 하이라이팅 | "반팔 티셔츠" 검색 | q=반팔 | 상품명에 `<em>반팔</em>` 하이라이팅 적용 | P2 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-016 | 검색 결과 없음 | 해당 키워드 상품 미존재 | q=없는상품명 | 200 OK, totalCount=0, items=[], suggestion 제안 | P1 |
| SE-017 | 빈 검색어 | - | q= (빈 문자열) | 400 Bad Request (필수 파라미터) | P1 |

### 3.3 US-703: 필터

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-018 | 카테고리 필터 | 상의 10건, 하의 5건 인덱싱 | q=셔츠&category=TOP | 상의 카테고리만 반환 | P2 |
| SE-019 | 브랜드 복수 필터 | Nike 5건, Adidas 3건 | q=신발&brand=Nike&brand=Adidas | Nike + Adidas 8건 반환 | P2 |
| SE-020 | 가격 범위 필터 | 다양한 가격대 상품 | q=티셔츠&minPrice=10000&maxPrice=30000 | 10,000~30,000원 상품만 반환 | P2 |
| SE-021 | 복수 필터 AND 조합 | - | q=티셔츠&category=TOP&color=BLACK&size=M | 상의+블랙+M사이즈 교집합만 반환 | P2 |
| SE-022 | facet count 반환 | 검색 결과 존재 | q=티셔츠 | facets에 카테고리/브랜드/가격대/색상/사이즈별 count 반환 | P2 |

#### 경계 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-023 | 필터 결과 없음 | 조건에 맞는 상품 없음 | q=티셔츠&category=SHOES&color=PINK | 200 OK, items=[] | P2 |
| SE-024 | 페이지 크기 최대값 (100) | 검색 결과 150건 | q=티셔츠&size=100 | 100건 반환 | P2 |
| SE-025 | 페이지 크기 초과 (101) | - | q=티셔츠&size=101 | 400 Bad Request 또는 100으로 조정 | P2 |

### 3.4 US-704: 자동완성

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-026 | 자동완성 기본 동작 | "반팔 티셔츠" 인덱싱 | GET /api/v1/search/autocomplete?q=반팔 | 200 OK, suggestions에 "반팔 티셔츠" 포함 | P2 |
| SE-027 | 자동완성 최대 10건 | 다수 상품 인덱싱 | q=티 | 최대 10개 suggestions | P2 |

#### 예외/경계 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-028 | 1글자 입력 (최소 미달) | - | q=반 | 200 OK, suggestions=[] (2글자 미만) | P2 |
| SE-029 | 자동완성 P99 응답시간 | 10만건 인덱싱 상태 | q=반팔 (100 req/s) | P99 50ms 이내 (PD-27) | P2 |

### 3.5 US-705: 인기 검색어

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-030 | 인기 검색어 Top 10 조회 | 검색 이력 존재 (Redis Sorted Set) | GET /api/v1/search/popular-keywords | 200 OK, keywords 10건, rank/keyword/change/searchCount 포함 | P2 |
| SE-031 | 검색 시 Redis Sorted Set 기록 | - | 상품 검색 실행 | Redis Sorted Set에 score=timestamp로 기록 (PD-25) | P2 |
| SE-032 | 1시간 이전 데이터 정리 | 1시간 이전 검색 기록 존재 | 인기 검색어 조회 | ZREMRANGEBYSCORE로 1시간 이전 데이터 제거 (sliding window) | P2 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-033 | 금칙어 필터링 | 금칙어 "비속어A" DB 등록 | "비속어A" 검색 후 인기 검색어 조회 | 인기 검색어에서 "비속어A" 제외 (PD-39) | P2 |
| SE-034 | Rate Limiting (IP 120/min 초과) | - | 동일 IP에서 121회/분 검색 | 429 Too Many Requests (PD-30) | P1 |
| SE-035 | Rate Limiting (사용자 60/min 초과) | 인증된 사용자 | 동일 사용자 61회/분 검색 | 429 Too Many Requests (PD-30) | P1 |

### 3.6 최근 검색어

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| SE-036 | 최근 검색어 저장 | 인증된 사용자 | 상품 검색 실행 | Redis List search:recent:{memberId}에 저장 (PD-48) | P2 |
| SE-037 | 최근 검색어 조회 | 검색 이력 존재 | GET /api/v1/search/recent | 200 OK, 최근 검색어 목록 (최신순) | P2 |

---

## 4. 리뷰 도메인 (US-801 ~ US-804)

### 4.1 US-801: 텍스트 + 포토 리뷰

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| RV-001 | 텍스트 리뷰 작성 | CONFIRMED 상태 주문 | POST /api/v1/reviews (rating=5, content=20자 이상) | 201 Created, Kafka review.created 이벤트 (포인트 100P) | P1 |
| RV-002 | 포토 리뷰 작성 (이미지 5장) | CONFIRMED 상태 주문, JPEG 이미지 5장 | POST /api/v1/reviews (multipart, images 5장) | 201 Created, hasPhoto=true, 원본+썸네일(400x400) 저장, 포인트 300P 이벤트 | P1 |
| RV-003 | 리뷰 수정 (텍스트만) | 작성 7일 이내 리뷰 | PUT /api/v1/reviews/{id} (content 변경) | 200 OK, review_edit_history 기록, 수정 카운터 증가 | P1 |
| RV-004 | 리뷰 수정 (이미지 교체) | 포토 리뷰, 7일 이내 | PUT /api/v1/reviews/{id} (images 교체) | 200 OK, 기존 이미지 삭제 + 신규 이미지 저장 | P1 |
| RV-005 | 리뷰 삭제 | 본인 리뷰 | DELETE /api/v1/reviews/{id} | 204 No Content, status=DELETED, 포인트 회수 이벤트 | P1 |
| RV-006 | 리뷰 목록 조회 (정렬) | 상품에 리뷰 10건 | GET /api/v1/reviews?productId={id}&sort=LATEST | 200 OK, 최신순 정렬 | P1 |
| RV-007 | 포토 리뷰만 필터 | 포토 리뷰 3건, 텍스트 리뷰 7건 | GET /api/v1/reviews?productId={id}&photoOnly=true | 3건만 반환 | P2 |

#### 예외 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| RV-008 | CONFIRMED 아닌 주문에 리뷰 | DELIVERED 상태 주문 | POST /api/v1/reviews | 400 Bad Request (구매확정 후 작성 가능) | P1 |
| RV-009 | 동일 OrderItem 중복 리뷰 | 이미 리뷰 작성된 orderItemId | POST /api/v1/reviews | 409 Conflict | P1 |
| RV-010 | 내용 20자 미만 | - | content="짧은리뷰" (10자) | 400 Bad Request (최소 20자) | P1 |
| RV-011 | 이미지 5장 초과 | - | images 6장 첨부 | 400 Bad Request (최대 5장) | P1 |
| RV-012 | 이미지 5MB 초과 | - | 6MB 이미지 1장 | 400 Bad Request / 413 Payload Too Large (PD-34) | P1 |
| RV-013 | 요청 전체 30MB 초과 | - | 5MB x 7장 = 35MB | 400 Bad Request (PD-34) | P1 |
| RV-014 | 별점 수정 시도 | 기존 리뷰 rating=5 | PUT /api/v1/reviews/{id} (rating=3) | 400 Bad Request (별점 수정 불가, PD-32) | P1 |
| RV-015 | 수정 3회 초과 시도 | 수정 이력 3회인 리뷰 | PUT /api/v1/reviews/{id} | 400 Bad Request (최대 3회, PD-32) | P1 |
| RV-016 | 7일 초과 리뷰 수정 시도 | 작성 8일 경과 리뷰 | PUT /api/v1/reviews/{id} | 400 Bad Request (수정 기한 만료) | P1 |
| RV-017 | 타인 리뷰 삭제 시도 | 다른 member의 리뷰 | DELETE /api/v1/reviews/{id} | 403 Forbidden | P1 |

#### 경계 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| RV-018 | 내용 정확히 20자 | - | content="정확히이십자로작성된리뷰내용입니다아" | 201 Created | P1 |
| RV-019 | 내용 정확히 1000자 | - | content=1000자 텍스트 | 201 Created | P2 |
| RV-020 | 별점 경계값 (1, 5) | - | rating=1 / rating=5 | 201 Created | P1 |
| RV-021 | 별점 범위 초과 (0, 6) | - | rating=0 / rating=6 | 400 Bad Request | P1 |

### 4.2 US-801 관리자 기능

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| RV-022 | 관리자 리뷰 숨김 | ACTIVE 상태 리뷰, ADMIN 인증 | PATCH /api/v1/admin/reviews/{id}/hide | status=HIDDEN (PD-35) | P1 |
| RV-023 | 비관리자 리뷰 숨김 시도 | BUYER 인증 | PATCH /api/v1/admin/reviews/{id}/hide | 403 Forbidden | P1 |

### 4.3 US-802: 사이즈 후기

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| RV-024 | 사이즈 정보 포함 리뷰 작성 | CONFIRMED 주문 | POST /api/v1/reviews (height=175, weight=70, sizeFit=PERFECT) | 201 Created, review_size_info 저장, 추가 50P 적립 이벤트 (총 350P) | P2 |
| RV-025 | 사이즈 핏 분포 조회 | 상품에 사이즈 리뷰 20건 (SMALL:3, PERFECT:14, LARGE:3) | GET /api/v1/reviews/size-summary?productId={id} | fitDistribution 반환, recommendation="정사이즈 추천" | P2 |
| RV-026 | 비슷한 체형 필터 | 키 175, 몸무게 70 입력 | GET /api/v1/reviews?productId={id}&minHeight=170&maxHeight=180&minWeight=65&maxWeight=75 | 해당 범위(+-5) 리뷰만 반환 | P2 |

### 4.4 US-803: 리뷰 포인트 적립

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| RV-027 | 텍스트 리뷰 100P 적립 | 리뷰 작성 | review.created 이벤트 | point.earn 이벤트 (amount=100, reason=TEXT_REVIEW) | P2 |
| RV-028 | 포토 리뷰 300P 적립 | 이미지 포함 리뷰 작성 | review.created 이벤트 | point.earn 이벤트 (amount=300, reason=PHOTO_REVIEW) | P2 |
| RV-029 | 포토 + 사이즈 350P 적립 | 이미지 + 사이즈 정보 포함 리뷰 | review.created 이벤트 | point.earn 이벤트 (amount=350) | P2 |
| RV-030 | 리뷰 삭제 시 포인트 회수 | 300P 적립된 포토 리뷰 | DELETE /api/v1/reviews/{id} | point.deduct 이벤트 (amount=300) | P2 |
| RV-031 | 포인트 회수 시 잔액 부족 (마이너스 허용) | 포인트 잔액 100P, 회수 300P | 리뷰 삭제 | 잔액 -200P (PD-36, 다음 적립에서 상계) | P2 |

#### 경계 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| RV-032 | 일일 한도 5,000P 도달 | 오늘 적립 누계 4,700P | 포토 리뷰 작성 (300P) | 5,000P까지만 적립 (PD-37) 또는 전체 적립 허용 후 한도 초과분 차단 | P2 |
| RV-033 | 일일 한도 KST 00:00 리셋 | 어제 적립 5,000P (한도 도달) | 오늘 00:00 이후 리뷰 작성 | Redis 키 만료, 적립 성공 (PD-37) | P2 |

### 4.5 US-804: 리뷰 집계

#### 정상 케이스

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| RV-034 | 리뷰 생성 시 집계 갱신 | 상품 리뷰 10건 (avg 4.0) | 새 리뷰 작성 (rating=5) | review_summary: count=11, avg_rating 재계산, Redis 캐시 갱신 | P2 |
| RV-035 | 리뷰 삭제 시 집계 갱신 | 상품 리뷰 10건 | 리뷰 1건 삭제 | review_summary: count=9, avg_rating 재계산 | P2 |
| RV-036 | 집계 조회 (Redis 캐시 hit) | Redis 캐시에 집계 데이터 | GET /api/v1/reviews/summary?productId={id} | 200 OK, 캐시된 집계 반환 | P2 |
| RV-037 | 리뷰 집계 -> ES 동기화 | review.summary.updated 이벤트 | Kafka Consumer | ES 상품 인덱스의 reviewCount, avgRating 갱신 | P2 |
| RV-038 | 벌크 집계 배치 (데이터 보정) | 집계 불일치 | POST /api/v1/admin/reviews/recalculate | 전체 상품 집계 재계산 | P2 |

---

## 5. 크로스 도메인 테스트

### 5.1 주문 -> 재고 -> 결제 -> 배송 -> 리뷰 (Happy Path E2E)

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| XD-001 | 전체 구매 흐름 E2E | 상품 재고 available=50 | 1) 주문 생성 -> 2) 재고 RESERVE(available=48, reserved=2) -> 3) 결제 완료 -> 4) 재고 DEDUCT(total=48) -> 5) 송장 등록 -> 6) 배송 추적(READY->IN_TRANSIT->DELIVERED) -> 7) 구매확정 -> 8) 리뷰 작성 -> 9) 포인트 적립 | 모든 단계 성공, 최종 상태: 주문=CONFIRMED, 재고=48, 리뷰=ACTIVE, 포인트 적립 완료 | P0 |

### 5.2 반품 -> 환불 -> 재고 복구

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| XD-002 | 반품 전체 흐름 E2E (단순변심) | DELIVERED 주문 (결제금액 30,000원), 재고 total=48 | 1) 반품 신청(CHANGE_OF_MIND) -> 2) PICKUP_SCHEDULED -> 3) PICKUP_COMPLETED -> 4) INSPECTING -> 5) APPROVED -> 6) 환불(30,000-3,000=27,000원) -> 7) 재고 RESTORE(total=50) | 환불 금액 27,000원, 재고 복구 +2, 반품 상태 APPROVED | P0 |
| XD-003 | 반품 전체 흐름 E2E (불량) | DELIVERED 주문 (결제금액 30,000원) | 반품 신청(DEFECTIVE) -> ... -> APPROVED -> 환불(30,000원 전액) | 환불 금액 30,000원 (배송비 0원), 재고 복구 | P0 |

### 5.3 교환 -> 재고 예약 -> 출고

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| XD-004 | 교환 전체 흐름 E2E | DELIVERED 주문 (옵션A:M), 옵션B(L) available=10 | 1) 교환 신청(A->B) -> 2) 옵션B RESERVE(reserved=1) -> 3) PICKUP_SCHEDULED -> 4) PICKUP_COMPLETED -> 5) 옵션A RESTORE -> 6) RESHIPPING -> 7) COMPLETED -> 8) 옵션B DEDUCT | 옵션A 재고 복구, 옵션B 재고 차감, 교환 완료 | P1 |

### 5.4 결제 실패 -> 재고 RELEASE

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| XD-005 | 결제 실패 시 재고 자동 해제 | 주문 생성 -> 재고 RESERVE(available=48, reserved=2) | 결제 실패 이벤트 (payment.failed) | reserved=0, available=50 복구, 주문 상태 CANCELLED | P0 |
| XD-006 | 15분 TTL 만료 시 자동 해제 | 주문 생성 -> 재고 RESERVE, 결제 미진행 | 15분 경과 | reserved=0, available=50 복구, 주문 자동 취소 | P0 |

### 5.5 상품 -> 검색 인덱싱 -> 검색 결과

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| XD-007 | 상품 생성 -> 검색 가능 | - | 1) 상품 생성 -> 2) product.created Kafka -> 3) ES 인덱싱 -> 4) 검색 | 3초 이내 검색 결과에 신규 상품 반환 | P1 |
| XD-008 | 상품 삭제 -> 검색 제외 | 인덱싱된 상품 | 1) 상품 삭제 -> 2) product.deleted Kafka -> 3) ES 삭제 -> 4) 검색 | 검색 결과에서 제외 | P1 |

### 5.6 리뷰 -> 집계 -> ES 동기화

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| XD-009 | 리뷰 작성 -> 집계 갱신 -> ES 반영 | 상품 인덱싱 완료 (avgRating=4.0) | 1) 리뷰 작성 (rating=5) -> 2) review_summary 갱신 -> 3) review.summary.updated Kafka -> 4) ES 상품 인덱스 avgRating 갱신 | ES 인덱스의 avgRating 값 갱신 확인 | P2 |

### 5.7 이벤트 멱등성

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| XD-010 | Transactional Outbox + 중복 이벤트 처리 | - | 1) 주문 생성 -> 2) outbox 테이블 INSERT -> 3) 폴러가 Kafka 발행 -> 4) Consumer: processed_event UNIQUE KEY 체크 -> 5) 동일 이벤트 재발행 | 2회차 이벤트 스킵, 재고 1회만 차감 (PD-04) | P0 |

### 5.8 서비스 간 인증

| ID | 테스트명 | 전제 조건 | 실행 | 기대 결과 | 우선순위 |
|----|---------|----------|------|-----------|---------|
| XD-011 | 내부 API 키 인증 성공 | X-Internal-Api-Key 헤더 설정 | 서비스 간 내부 API 호출 | 200 OK (PD-03) | P0 |
| XD-012 | 외부에서 내부 API 키 헤더 주입 시도 | 외부 요청에 X-Internal-Api-Key 포함 | Gateway 통과 | Gateway에서 헤더 제거, 401 Unauthorized (PD-03) | P0 |

---

## 테스트 케이스 요약

| 도메인 | 정상 | 예외 | 경계 | 소계 |
|--------|------|------|------|------|
| 배송 (US-501~505) | 17 | 15 | 9 | 41 |
| 재고 (US-601~604) | 14 | 10 | 6 | 30 |
| 검색 (US-701~705) | 16 | 7 | 5 | 28 |
| 리뷰 (US-801~804) | 18 | 12 | 8 | 38 |
| 크로스 도메인 | 12 | 0 | 0 | 12 |
| **합계** | **77** | **44** | **28** | **149** |

> 전체 149건 (정상 77건 + 예외 44건 + 경계 28건), 크로스 도메인 12건 포함.
> P0: 42건 / P1: 56건 / P2: 51건
