// 패턴: 통합 알림 시스템의 수신자 모델
package com.example.notification.domain.model

/**
 * 알림 수신자.
 */
data class NotificationRecipient(
    val userId: Long,
    val email: String? = null,
    val phone: String? = null,
    val pushToken: String? = null,
    val isStoreOwner: Boolean = false,
)
