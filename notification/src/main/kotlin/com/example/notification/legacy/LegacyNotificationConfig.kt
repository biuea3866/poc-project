// 패턴: 레거시 알림 설정 (유저별 채널 ON/OFF만)
package com.example.notification.legacy

/**
 * 레거시 알림 설정: 유저별 채널 ON/OFF만.
 * 문제:
 * - 매장별, 상품별 알림 ON/OFF 불가
 * - 우선순위, 빈도 설정 불가
 * - 강제 알림 개념 없음
 */
data class LegacyNotificationConfig(
    val userId: Long,
    val emailEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val smsEnabled: Boolean = true,
    val inAppEnabled: Boolean = true,
)
