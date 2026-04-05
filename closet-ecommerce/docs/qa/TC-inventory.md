# Inventory 테스트 케이스

> 도메인: 재고 관리 (inventory-service)
> 작성일: 2026-04-05
> 총 20건

## 테스트 케이스

| TC-ID | 분류 | 시나리오 | 사전조건 | 절차 | 기대결과 | 우선순위 |
|-------|------|----------|----------|------|----------|----------|
| INV-001 | 재고 조회 | 정상 재고 조회 | SKU `SKU-001` 재고 50개 등록 | 1. GET `/api/v1/inventory/{skuId}` 호출 | 200 OK, `quantity: 50` 반환 | P1 |
| INV-002 | 재고 조회 | 미존재 SKU 재고 조회 | SKU `SKU-999` 미등록 | 1. GET `/api/v1/inventory/{skuId}` 호출 | 404 Not Found, `INVENTORY_NOT_FOUND` 에러 코드 반환 | P1 |
| INV-003 | 재고 차감 | 정상 재고 차감 | SKU `SKU-001` 재고 50개 | 1. POST `/api/v1/inventory/{skuId}/deduct` (quantity: 3) 호출 | 200 OK, 잔여 재고 47개, `event.closet.inventory` 이벤트 발행 | P1 |
| INV-004 | 재고 차감 | 재고 부족 시 차감 실패 | SKU `SKU-001` 재고 2개 | 1. POST `/api/v1/inventory/{skuId}/deduct` (quantity: 5) 호출 | 409 Conflict, `INSUFFICIENT_STOCK` 에러 코드, 재고 변동 없음 | P1 |
| INV-005 | 재고 차감 | 재고 0개 시 차감 시도 | SKU `SKU-001` 재고 0개 | 1. POST `/api/v1/inventory/{skuId}/deduct` (quantity: 1) 호출 | 409 Conflict, `INSUFFICIENT_STOCK` 에러 코드 | P1 |
| INV-006 | 재고 복원 | 주문 취소 시 재고 복원 | SKU `SKU-001` 재고 47개, 주문 취소 이벤트 발생 | 1. `event.closet.order` 주문 취소 이벤트 수신 2. 재고 복원 처리 | 재고 50개로 복원, `event.closet.inventory` 복원 이벤트 발행 | P1 |
| INV-007 | 재고 입고 | 정상 재고 입고 | SKU `SKU-001` 재고 50개 | 1. POST `/api/v1/inventory/{skuId}/receive` (quantity: 100) 호출 | 200 OK, 재고 150개, 입고 이력 생성 | P1 |
| INV-008 | 동시성 | 100 스레드 동시 재고 차감 | SKU `SKU-001` 재고 100개 | 1. 100개 스레드에서 동시에 1개씩 차감 요청 | 정확히 재고 0개, 모든 요청 성공, Race condition 없음 | P1 |
| INV-009 | 동시성 | 재고 부족 상황 동시 요청 | SKU `SKU-001` 재고 5개 | 1. 10개 스레드에서 동시에 1개씩 차감 요청 | 5건 성공, 5건 실패(INSUFFICIENT_STOCK), 잔여 재고 0개 | P1 |
| INV-010 | 동시성 | 차감과 입고 동시 실행 | SKU `SKU-001` 재고 50개 | 1. 차감 30개 + 입고 20개 동시 실행 | 최종 재고 40개 (50-30+20), 데이터 정합성 보장 | P2 |
| INV-011 | 안전재고 알림 | 안전재고 이하 시 알림 트리거 | SKU `SKU-001` 재고 11개, 안전재고 10개 설정 | 1. POST `/api/v1/inventory/{skuId}/deduct` (quantity: 2) 호출 | 재고 9개, `event.closet.notification` 안전재고 알림 이벤트 발행 | P2 |
| INV-012 | 안전재고 알림 | 안전재고 알림 중복 방지 | SKU `SKU-001` 재고 9개 (이미 알림 발송됨) | 1. POST `/api/v1/inventory/{skuId}/deduct` (quantity: 1) 호출 | 재고 8개, 추가 알림 이벤트 발행하지 않음 | P2 |
| INV-013 | 안전재고 알림 | 재입고 후 알림 리셋 | SKU `SKU-001` 재고 5개 (알림 발송 상태) | 1. POST `/api/v1/inventory/{skuId}/receive` (quantity: 20) 호출 | 재고 25개, 안전재고 알림 플래그 리셋 | P3 |
| INV-014 | 재입고 알림 | 재입고 알림 신청 | SKU `SKU-001` 재고 0개, 회원 `MBR-001` | 1. POST `/api/v1/inventory/{skuId}/restock-notification` (memberId: MBR-001) 호출 | 201 Created, 재입고 알림 신청 등록 | P2 |
| INV-015 | 재입고 알림 | 재입고 시 알림 발송 | SKU `SKU-001` 재고 0개, 알림 신청 3건 | 1. POST `/api/v1/inventory/{skuId}/receive` (quantity: 10) 호출 | 재고 10개, 3명에게 `event.closet.notification` 재입고 알림 이벤트 발행 | P2 |
| INV-016 | 재입고 알림 | 재입고 알림 취소 | SKU `SKU-001` 재입고 알림 신청 상태 | 1. DELETE `/api/v1/inventory/{skuId}/restock-notification` (memberId: MBR-001) 호출 | 200 OK, 알림 신청 삭제 | P3 |
| INV-017 | 재입고 알림 | 재입고 알림 50건 제한 | SKU `SKU-001` 재고 0개, 알림 신청 50건 | 1. POST `/api/v1/inventory/{skuId}/restock-notification` (memberId: MBR-051) 호출 | 409 Conflict, `RESTOCK_NOTIFICATION_LIMIT_EXCEEDED` 에러 | P3 |
| INV-018 | 재고 조회 | 옵션별 재고 일괄 조회 | 상품 `PRD-001`에 SKU 3종 등록 (S/M/L) | 1. GET `/api/v1/inventory/product/{productId}` 호출 | 200 OK, 3개 SKU별 재고 목록 반환 | P2 |
| INV-019 | 재고 차감 | 음수 수량 차감 요청 | SKU `SKU-001` 재고 50개 | 1. POST `/api/v1/inventory/{skuId}/deduct` (quantity: -1) 호출 | 400 Bad Request, `INVALID_QUANTITY` 에러 코드 | P2 |
| INV-020 | 재고 입고 | 입고 이력 조회 | SKU `SKU-001` 입고 이력 3건 | 1. GET `/api/v1/inventory/{skuId}/history` 호출 | 200 OK, 입고/차감/복원 이력 시간순 목록 반환 | P3 |
