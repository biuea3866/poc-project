package com.biuea.batch.domain

/**
 * 실제 발송되는 알림 메시지(value). bundled=true 면 묶음 메시지, false 면 단건 메시지.
 */
data class NotificationMessage(
    val userId: Long,
    val type: NotificationType,
    val groupKey: String,
    val content: String,
    val bundled: Boolean,
)
