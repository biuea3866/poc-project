package com.closet.shipping.consumer.event

/**
 * event.closet.cs 토픽의 클레임 이벤트 엔벨로프.
 *
 * 향후 CS 도메인 이벤트를 shipping 서비스에서 수신할 때 사용.
 * eventType 필드로 이벤트 유형을 구분한다.
 */
data class ClaimEvent(
    val eventType: String,
    val claimId: Long? = null,
    val orderId: Long? = null,
    val claimType: String? = null,
    val status: String? = null,
)
