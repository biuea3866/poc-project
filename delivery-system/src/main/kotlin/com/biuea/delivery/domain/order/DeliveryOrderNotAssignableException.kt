package com.biuea.delivery.domain.order

/**
 * 배차 불가 상태의 주문을 수락하려 할 때 발생한다.
 * 경합에서 진 라이더에게 "누가 가져갔는지"를 알려주려면 실패 사유에 현재 배차 라이더가 함께 실려야 한다.
 */
class DeliveryOrderNotAssignableException(
    val orderId: Long?,
    val currentStatus: DeliveryOrderStatus,
    val assignedRiderId: Long?,
) : RuntimeException(
    "배차할 수 없는 주문입니다. orderId=$orderId, status=$currentStatus, assignedRiderId=$assignedRiderId",
)
