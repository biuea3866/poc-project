package com.closet.notification.application

import com.closet.notification.consumer.event.InventoryEvent
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 알림 Facade.
 *
 * Kafka Consumer 등 외부 진입점에서 호출되는 오케스트레이션 계층.
 * 비즈니스 로직은 Service에 위임한다.
 */
@Component
class NotificationFacade(
    private val notificationService: NotificationService,
    private val notificationTemplateService: NotificationTemplateService,
) {
    companion object {
        private const val DEFAULT_RESTOCK_TITLE = "재입고 알림"
        private const val DEFAULT_RESTOCK_CONTENT = "구독하신 상품이 재입고되었습니다. 지금 확인해보세요!"
    }

    /**
     * 재입고 알림 이벤트 처리.
     *
     * inventory 도메인에서 RESTOCK_NOTIFICATION 이벤트를 수신하면,
     * 각 구독 회원에게 PUSH 알림을 발송한다.
     * 활성화된 템플릿이 있으면 변수 치환 후 발송하고, 없으면 기본 메시지를 사용한다.
     */
    fun handleRestockNotification(event: InventoryEvent) {
        if (event.memberIds.isEmpty()) {
            logger.info { "재입고 알림 대상 회원 없음: productOptionId=${event.productOptionId}" }
            return
        }

        val template =
            notificationTemplateService.findActiveTemplate(
                NotificationType.RESTOCK,
                NotificationChannel.PUSH,
            )

        val variables =
            mapOf(
                "sku" to (event.sku ?: ""),
                "availableQuantity" to (event.availableQuantity?.toString() ?: "0"),
                "productOptionId" to event.productOptionId.toString(),
            )

        event.memberIds.forEach { memberId ->
            try {
                if (template != null) {
                    var renderedTitle = template.titleTemplate
                    var renderedContent = template.contentTemplate
                    variables.forEach { (key, value) ->
                        renderedTitle = renderedTitle.replace("{{$key}}", value)
                        renderedContent = renderedContent.replace("{{$key}}", value)
                    }

                    notificationService.send(
                        memberId = memberId,
                        type = NotificationType.RESTOCK,
                        channel = NotificationChannel.PUSH,
                        title = renderedTitle,
                        content = renderedContent,
                    )
                } else {
                    notificationService.send(
                        memberId = memberId,
                        type = NotificationType.RESTOCK,
                        channel = NotificationChannel.PUSH,
                        title = DEFAULT_RESTOCK_TITLE,
                        content = DEFAULT_RESTOCK_CONTENT,
                    )
                }

                logger.info { "재입고 알림 발송 완료: memberId=$memberId, productOptionId=${event.productOptionId}" }
            } catch (e: Exception) {
                logger.error(e) { "재입고 알림 발송 실패: memberId=$memberId, productOptionId=${event.productOptionId}" }
            }
        }
    }
}
