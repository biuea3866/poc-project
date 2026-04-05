package com.closet.payment.consumer.event

/**
 * event.closet.shipping 토픽의 반품 승인 이벤트 페이로드.
 *
 * eventType: "ReturnApproved"
 * 반품 승인 시 부분 환불 처리를 위한 정보를 담는다.
 */
data class ReturnApprovedEvent(
    val orderId: Long,
    val returnRequestId: Long,
    val refundAmount: Long,
)
