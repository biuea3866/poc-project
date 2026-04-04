// 패턴: 통합 알림 시스템의 알림 이벤트 모델
package com.example.notification.domain.model

import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationTriggerType

/**
 * 알림 이벤트. 하나의 이벤트로 모든 채널(EMAIL, PUSH, SMS)을 통합 발송.
 * storeId, productId, orderId
 *
 * correlationId/correlationTotalCount: 벌크 배칭용 (예: 대량 주문 접수 시 요약)
 */
data class NotificationEvent(
    val triggerType: NotificationTriggerType,
    val storeId: Long,
    val productId: Long? = null,
    val orderId: Long? = null,
    val actorUserId: Long? = null,
    val payload: NotificationPayload,
    val occurredAt: Long = System.currentTimeMillis(),
    val correlationId: String? = null,
    val correlationTotalCount: Int? = null,
) {
    val isCorrelated: Boolean get() = correlationId != null

    fun isCorrelationCountReached(currentCount: Int): Boolean =
        correlationTotalCount != null && currentCount >= correlationTotalCount

    fun generateIdempotencyKey(recipientUserId: Long, channel: NotificationChannel): String =
        "$storeId:$triggerType:$recipientUserId:${orderId ?: 0}:$channel:$occurredAt"

    fun generateCorrelationIdempotencyKey(recipientUserId: Long, channel: NotificationChannel): String =
        "$storeId:CORR:$correlationId:$recipientUserId:$channel"
}
