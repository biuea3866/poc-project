// 패턴: 통합 알림 시스템의 수신자 결정 서비스
package com.example.notification.application.service

import com.example.notification.application.port.NotificationRecipientReader
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationRecipient

import org.springframework.stereotype.Component

/**
 * 알림 수신자를 결정한다.
 *
 * 트리거별 수신자 규칙:
 * - ORDER_PLACED, PAYMENT_COMPLETED, REVIEW_SUBMITTED -> 판매자 (storeOwner)
 * - SHIPMENT_STARTED, SHIPMENT_DELIVERED -> 구매자
 * - REFUND_REQUESTED -> 판매자 + 구매자 (강제, isMandatory)
 *
 * actorUserId는 항상 수신자에서 제외한다.
 */
@Component
class NotificationRecipientResolver(
    private val recipientReader: NotificationRecipientReader,
) {
    fun resolve(event: NotificationEvent): List<NotificationRecipient> {
        val candidates = when (event.triggerType) {
            NotificationTriggerType.ORDER_PLACED,
            NotificationTriggerType.PAYMENT_COMPLETED,
            NotificationTriggerType.REVIEW_SUBMITTED,
            -> recipientReader.findByStoreId(event.storeId).filter { it.isStoreOwner }

            NotificationTriggerType.SHIPMENT_STARTED,
            NotificationTriggerType.SHIPMENT_DELIVERED,
            -> event.orderId?.let { recipientReader.findByOrderId(it) }?.filter { !it.isStoreOwner }
                ?: emptyList()

            NotificationTriggerType.REFUND_REQUESTED -> {
                val storeRecipients = recipientReader.findByStoreId(event.storeId).filter { it.isStoreOwner }
                val buyerRecipients = event.orderId?.let { recipientReader.findByOrderId(it) }?.filter { !it.isStoreOwner }
                    ?: emptyList()
                storeRecipients + buyerRecipients
            }
        }

        return candidates.filter { it.userId != event.actorUserId }
    }
}
