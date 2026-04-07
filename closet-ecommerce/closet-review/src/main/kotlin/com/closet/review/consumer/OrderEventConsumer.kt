package com.closet.review.consumer

import com.closet.common.event.ClosetTopics
import com.closet.review.application.ReviewService
import com.closet.review.consumer.event.OrderEvent
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.order 토픽 Consumer.
 *
 * 주문 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - OrderItemConfirmed: 구매확정 이벤트 수신 (리뷰 작성 가능 상태 기록)
 *
 * Consumer는 이벤트 수신 및 라우팅만 담당하고,
 * 실제 비즈니스 로직은 Service에 위임한다.
 */
@Component
class OrderEventConsumer(
    private val reviewService: ReviewService,
) {
    @KafkaListener(topics = [ClosetTopics.ORDER], groupId = "review-service")
    fun handle(event: OrderEvent) {
        logger.info { "${ClosetTopics.ORDER} 수신: eventType=${event.eventType}, orderItemId=${event.orderItemId}" }

        try {
            when (event.eventType) {
                "OrderItemConfirmed" -> {
                    reviewService.onOrderItemConfirmed(
                        orderItemId = event.orderItemId,
                        memberId = event.memberId,
                        productId = event.productId,
                    )
                    logger.info { "OrderItemConfirmed 처리 완료: orderItemId=${event.orderItemId}" }
                }
                else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.ORDER} 처리 실패: eventType=${event.eventType}, orderItemId=${event.orderItemId}" }
            throw e
        }
    }
}
