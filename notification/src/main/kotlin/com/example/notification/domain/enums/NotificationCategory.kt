// 패턴: 통합 알림 시스템의 알림 카테고리
package com.example.notification.domain.enums

/**
 * 알림 카테고리.
 *
 * @param description 카테고리 설명
 * @param defaultChannels 기본 활성 채널
 * @param isMandatory true면 모든 채널 강제 ON (사용자 설정 무시)
 */
enum class NotificationCategory(
    val description: String,
    val defaultChannels: Set<NotificationChannel>,
    val isMandatory: Boolean,
) {
    ORDER("주문", setOf(NotificationChannel.EMAIL, NotificationChannel.PUSH, NotificationChannel.IN_APP), false),
    SHIPMENT("배송", setOf(NotificationChannel.EMAIL, NotificationChannel.PUSH, NotificationChannel.SMS, NotificationChannel.IN_APP), false),
    PAYMENT("결제", setOf(NotificationChannel.EMAIL, NotificationChannel.IN_APP), false),
    REVIEW("리뷰", setOf(NotificationChannel.PUSH, NotificationChannel.IN_APP), false),
    SYSTEM("시스템", setOf(NotificationChannel.EMAIL, NotificationChannel.PUSH, NotificationChannel.SMS, NotificationChannel.IN_APP), true),
    ;

    fun isChannelEnabledByDefault(channel: NotificationChannel): Boolean =
        channel in defaultChannels

    fun supportsScopeType(scopeType: ScopeType): Boolean = when (scopeType) {
        ScopeType.GLOBAL -> true
        ScopeType.STORE -> this in setOf(ORDER, SHIPMENT, PAYMENT, REVIEW)
        ScopeType.PRODUCT -> this in setOf(ORDER, REVIEW)
    }
}
