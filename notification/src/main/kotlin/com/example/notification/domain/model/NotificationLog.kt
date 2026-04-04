// 패턴: 통합 알림 시스템의 발송 로그 모델
package com.example.notification.domain.model

import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationLogStatus
import com.example.notification.domain.enums.NotificationPriority
import com.example.notification.domain.enums.NotificationTriggerType

/**
 * 알림 발송 로그.
 * 멱등성 보장을 위한 idempotencyKey 포함.
 */
data class NotificationLog(
    val id: Long = 0L,
    val storeId: Long,
    val recipientUserId: Long,
    val triggerType: NotificationTriggerType,
    val channel: NotificationChannel,
    val status: NotificationLogStatus,
    val priority: NotificationPriority,
    val idempotencyKey: String,
    val failureReason: String? = null,
) {
    companion object {
        fun sent(
            event: NotificationEvent,
            recipient: NotificationRecipient,
            rule: EffectiveRule,
            idempotencyKey: String,
        ) = NotificationLog(
            storeId = event.storeId,
            recipientUserId = recipient.userId,
            triggerType = event.triggerType,
            channel = rule.channel,
            status = NotificationLogStatus.SENT,
            priority = rule.priority,
            idempotencyKey = idempotencyKey,
        )

        fun failed(
            event: NotificationEvent,
            recipient: NotificationRecipient,
            rule: EffectiveRule,
            idempotencyKey: String,
            reason: String,
        ) = NotificationLog(
            storeId = event.storeId,
            recipientUserId = recipient.userId,
            triggerType = event.triggerType,
            channel = rule.channel,
            status = NotificationLogStatus.FAILED,
            priority = rule.priority,
            idempotencyKey = idempotencyKey,
            failureReason = reason,
        )
    }
}
