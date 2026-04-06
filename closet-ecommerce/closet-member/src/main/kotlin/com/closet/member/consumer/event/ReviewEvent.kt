package com.closet.member.consumer.event

/**
 * event.closet.review 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - ReviewCreated: 포인트 적립 (텍스트 100P, 포토 300P, 사이즈정보 +50P, 최대 350P, 일일 한도 5,000P)
 * - ReviewDeleted: 포인트 회수 (잔액 부족 시 마이너스 허용)
 */
data class ReviewEvent(
    val eventType: String,
    val eventId: String? = null,
    val reviewId: Long? = null,
    val productId: Long? = null,
    val memberId: Long? = null,
    val rating: Int? = null,
    val isPhotoReview: Boolean? = null,
    val pointAmount: Int? = null,
) {
    fun toReviewCreatedEvent(): ReviewCreatedEvent {
        return ReviewCreatedEvent(
            reviewId = reviewId ?: 0L,
            memberId = memberId ?: 0L,
            pointAmount = pointAmount ?: 0,
        )
    }

    fun toReviewDeletedEvent(): ReviewDeletedEvent {
        return ReviewDeletedEvent(
            reviewId = reviewId ?: 0L,
            memberId = memberId ?: 0L,
            pointAmount = pointAmount ?: 0,
        )
    }
}
