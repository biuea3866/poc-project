// 패턴: 통합 알림 시스템의 최종 적용 규칙 모델
package com.example.notification.domain.model

import com.example.notification.domain.enums.Frequency
import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationPriority
import com.example.notification.domain.enums.NotificationTriggerType

/**
 * 최종 적용된 규칙. RuleResolver가 4계층 우선순위를 거쳐 결정한 결과.
 */
data class EffectiveRule(
    val channel: NotificationChannel,
    val enabled: Boolean,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val frequency: Frequency = Frequency.IMMEDIATE,
) {
    companion object {
        fun systemDefault(channel: NotificationChannel, triggerType: NotificationTriggerType): EffectiveRule {
            val category = triggerType.category
            return EffectiveRule(
                channel = channel,
                enabled = category.isChannelEnabledByDefault(channel),
                priority = triggerType.defaultPriority,
            )
        }
    }
}
