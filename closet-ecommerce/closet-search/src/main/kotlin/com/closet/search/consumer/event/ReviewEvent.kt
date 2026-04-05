package com.closet.search.consumer.event

/**
 * event.closet.review 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - ReviewSummaryUpdated: ES 문서의 reviewCount, avgRating 부분 업데이트
 */
data class ReviewEvent(
    val eventType: String,
    val productId: Long = 0L,
    val reviewCount: Int = 0,
    val avgRating: Double = 0.0,
)
