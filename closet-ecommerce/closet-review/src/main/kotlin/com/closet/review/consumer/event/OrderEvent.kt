package com.closet.review.consumer.event

/**
 * event.closet.order 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - OrderItemConfirmed: 주문 아이템 구매확정 (리뷰 작성 가능 상태)
 */
data class OrderEvent(
    val eventType: String,
    val orderId: Long = 0L,
    val orderItemId: Long = 0L,
    val memberId: Long = 0L,
    val productId: Long = 0L,
)
