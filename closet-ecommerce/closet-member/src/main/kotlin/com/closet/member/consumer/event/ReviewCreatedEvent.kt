package com.closet.member.consumer.event

/**
 * event.closet.review 토픽의 리뷰 생성 이벤트 페이로드.
 *
 * eventType: "ReviewCreated"
 * 리뷰 포인트 적립을 위한 정보를 담는다.
 */
data class ReviewCreatedEvent(
    val reviewId: Long,
    val memberId: Long,
    val pointAmount: Int,
)
