package com.closet.inventory.consumer.event

/**
 * event.closet.shipping 토픽의 반품 승인 이벤트 페이로드.
 *
 * eventType: "ReturnApproved"
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
