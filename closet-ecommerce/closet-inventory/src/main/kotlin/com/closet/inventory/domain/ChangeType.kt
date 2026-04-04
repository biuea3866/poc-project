package com.closet.inventory.domain

/**
 * 재고 변동 유형.
 *
 * RESERVE: 주문 생성 시 가용 재고 예약 (available--, reserved++)
 * DEDUCT: 결제 완료 시 예약 재고 차감 (reserved--)
 * RELEASE: 주문 취소 시 예약 재고 복구 (reserved--, available++)
 * INBOUND: 입고 (total++, available++)
 * RETURN_RESTORE: 반품 양품 복구 (total++, available++)
 * ADJUSTMENT: 수동 조정
 */
enum class ChangeType {
    RESERVE,
    DEDUCT,
    RELEASE,
    INBOUND,
    RETURN_RESTORE,
    ADJUSTMENT,
}
