// 패턴: 통합 알림 시스템의 렌더링된 메시지 모델
package com.example.notification.domain.model

/**
 * 렌더링된 알림 메시지.
 */
data class RenderedMessage(
    val subject: String,
    val body: String,
    val pushTitle: String,
    val pushBody: String,
    val smsText: String,
    val deepLinkUrl: String,
)
