package com.closet.member.consumer.event

/**
 * event.closet.review 토픽의 리뷰 삭제 이벤트 페이로드.
 *
 * eventType: "ReviewDeleted"
 * 리뷰 포인트 회수를 위한 정보를 담는다.
 */
data class ReviewDeletedEvent(
    val reviewId: Long,
    val memberId: Long,
    val pointAmount: Int,
)
