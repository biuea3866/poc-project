package com.closet.notification.consumer

import com.closet.common.event.ClosetTopics
import com.closet.notification.application.NotificationFacade
import com.closet.notification.consumer.event.InventoryEvent
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.inventory 토픽 Consumer.
 *
 * 재고 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - RESTOCK_NOTIFICATION: 재입고 알림 발송
 *
 * Consumer는 이벤트 수신 및 라우팅만 담당하고,
 * 실제 비즈니스 로직은 Facade/Service에 위임한다.
 */
@Component
class RestockEventConsumer(
    private val notificationFacade: NotificationFacade,
) {
    @KafkaListener(topics = [ClosetTopics.INVENTORY], groupId = "notification-service")
    fun handle(event: InventoryEvent) {
        logger.info { "${ClosetTopics.INVENTORY} 수신: eventType=${event.eventType}, productOptionId=${event.productOptionId}" }

        try {
            when (event.eventType) {
                "RESTOCK_NOTIFICATION" -> notificationFacade.handleRestockNotification(event)
                else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.INVENTORY} 처리 실패: eventType=${event.eventType}, productOptionId=${event.productOptionId}" }
            throw e
        }
    }
}
