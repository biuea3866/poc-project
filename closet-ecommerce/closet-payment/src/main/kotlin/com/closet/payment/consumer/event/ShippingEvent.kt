package com.closet.payment.consumer.event

/**
 * event.closet.shipping 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - ReturnApproved: 반품 승인 시 부분 환불 처리
 */
data class ShippingEvent(
    val eventType: String,
    val orderId: Long? = null,
    val returnRequestId: Long? = null,
    val refundAmount: Long? = null,
    val shippingFee: Long? = null,
) {
    fun toReturnApprovedEvent(): ReturnApprovedEvent {
        return ReturnApprovedEvent(
            orderId = orderId ?: 0L,
            returnRequestId = returnRequestId ?: 0L,
            refundAmount = refundAmount ?: 0L,
        )
    }
}
