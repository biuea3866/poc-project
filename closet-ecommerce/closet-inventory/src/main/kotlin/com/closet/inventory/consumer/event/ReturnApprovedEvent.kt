package com.closet.inventory.consumer.event

/**
 * return.approved 토픽으로 수신되는 이벤트 DTO.
 *
 * 반품 승인 시 재고 양품 복구(returnRestore)를 위한 정보를 담는다.
 */
data class ReturnApprovedEvent(
    val orderId: Long,
    val items: List<ReturnItemInfo>,
) {
    data class ReturnItemInfo(
        val productOptionId: Long,
        val quantity: Int,
    )
}
