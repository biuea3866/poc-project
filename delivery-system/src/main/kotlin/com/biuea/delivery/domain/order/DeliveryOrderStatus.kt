package com.biuea.delivery.domain.order

enum class DeliveryOrderStatus {
    COOKING,
    WAITING_RIDER,
    ASSIGNED,
    PICKED_UP,
    DELIVERED,
    CANCELLED,
    ;

    /**
     * 배차 가능 여부 판단을 상태 enum 안에 가둔다.
     * 호출부가 `status == WAITING_RIDER` 로 비교하기 시작하면 동시성 제어 전략마다 판단이 복제되고,
     * 전략별로 조건이 어긋나는 순간 중복 배차가 생긴다.
     */
    fun canAssignRider(): Boolean = this == WAITING_RIDER
}
