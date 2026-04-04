// 패턴: 통합 알림 시스템의 오케스트레이터
package com.example.notification.application.service

import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationRecipient
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 알림 처리의 단일 진입점.
 *
 * 1. RecipientResolver -- 누구에게
 * 2. RuleResolver -- 어떤 채널로 (4계층 Preference)
 * 3. NotificationDispatcher -- 발송 (단건 즉시 or 벌크 배칭)
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT) 으로 트랜잭션 커밋 후 처리.
 */
@Component
class NotificationOrchestrator(
    private val recipientResolver: NotificationRecipientResolver,
    private val ruleResolver: NotificationRuleResolver,
    private val dispatcher: NotificationDispatcher,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: NotificationEvent) {
        println(
            "NotificationEvent received: trigger=${event.triggerType}, " +
                "storeId=${event.storeId}, orderId=${event.orderId}, correlationId=${event.correlationId}"
        )

        val recipients = recipientResolver.resolve(event)
        if (recipients.isEmpty()) {
            println("No recipients for event: trigger=${event.triggerType}, storeId=${event.storeId}")
            return
        }

        recipients.forEach { recipient ->
            processForRecipient(event, recipient)
        }
    }

    private fun processForRecipient(
        event: NotificationEvent,
        recipient: NotificationRecipient,
    ) {
        val effectiveRules = ruleResolver.resolve(
            userId = recipient.userId,
            storeId = event.storeId,
            triggerType = event.triggerType,
            productId = event.productId,
            isStoreOwner = recipient.isStoreOwner,
        )

        effectiveRules
            .filter { it.enabled }
            .forEach { rule ->
                dispatcher.deliver(event, recipient, rule)
            }
    }
}
