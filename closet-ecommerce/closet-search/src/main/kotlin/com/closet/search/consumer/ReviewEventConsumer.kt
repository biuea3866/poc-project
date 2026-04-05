package com.closet.search.consumer

import com.closet.common.event.ClosetTopics
import com.closet.search.application.facade.SearchFacade
import com.closet.search.consumer.event.ReviewEvent
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.review 토픽 Consumer.
 *
 * 리뷰 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - ReviewSummaryUpdated: ES 문서의 reviewCount, avgRating 부분 업데이트
 *
 * Consumer는 이벤트 수신 및 라우팅만 담당하고,
 * 실제 비즈니스 로직은 SearchFacade에 위임한다.
 *
 * SEARCH_INDEXING_ENABLED Feature Flag로 활성화/비활성화를 제어한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.search-indexing-enabled"], havingValue = "true", matchIfMissing = true)
class ReviewEventConsumer(
    private val searchFacade: SearchFacade,
) {

    @KafkaListener(topics = [ClosetTopics.REVIEW], groupId = "search-service")
    fun handle(event: ReviewEvent) {
        logger.info { "${ClosetTopics.REVIEW} 수신: eventType=${event.eventType}, productId=${event.productId}" }

        try {
            when (event.eventType) {
                "ReviewSummaryUpdated" -> searchFacade.handleReviewSummaryUpdated(
                    productId = event.productId,
                    reviewCount = event.reviewCount,
                    avgRating = event.avgRating,
                )

                else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.REVIEW} 처리 실패: eventType=${event.eventType}, productId=${event.productId}" }
            throw e
        }
    }
}
