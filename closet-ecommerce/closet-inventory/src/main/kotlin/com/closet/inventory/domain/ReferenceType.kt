package com.closet.inventory.domain

/**
 * 재고 변동 참조 유형.
 *
 * ORDER: 주문으로 인한 재고 변동
 * RETURN: 반품으로 인한 재고 복구
 * EXCHANGE: 교환으로 인한 재고 변동
 * MANUAL: 수동 조정 (관리자)
 */
enum class ReferenceType {
    ORDER,
    RETURN,
    EXCHANGE,
    MANUAL,
}
