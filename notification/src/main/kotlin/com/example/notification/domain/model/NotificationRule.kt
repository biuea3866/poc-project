// 패턴: 통합 알림 시스템의 알림 규칙 모델
package com.example.notification.domain.model

import com.example.notification.domain.enums.Frequency
import com.example.notification.domain.enums.NotificationCategory
import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationPriority
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.enums.ScopeType

/**
 * 알림 규칙 (DB 저장 단위).
 * storeId, userId, scopeType(GLOBAL/STORE/PRODUCT)
 *
 * userId == null → Store Policy (관리자 설정, Workspace Policy에 대응)
 * userId != null → User Rule (개인 설정)
 */
data class NotificationRule(
    val id: Long = 0L,
    val storeId: Long,
    val userId: Long?,
    val scopeType: ScopeType,
    val scopeId: Long?,
    val category: NotificationCategory,
    val triggerType: NotificationTriggerType,
    val channel: NotificationChannel,
    val enabled: Boolean,
    val priority: NotificationPriority,
    val frequency: Frequency,
) {
    fun isStorePolicy(): Boolean = userId == null
    fun isUserRule(): Boolean = userId != null

    fun isApplicableTo(targetScopeType: ScopeType, targetScopeId: Long?): Boolean {
        if (scopeType != targetScopeType) return false
        if (scopeType == ScopeType.GLOBAL) return true
        return scopeId == targetScopeId
    }

    fun toEffectiveRule(): EffectiveRule = EffectiveRule(
        channel = channel,
        enabled = enabled,
        priority = priority,
        frequency = frequency,
    )

    companion object {
        fun create(
            storeId: Long,
            userId: Long?,
            scopeType: ScopeType,
            scopeId: Long? = null,
            category: NotificationCategory,
            triggerType: NotificationTriggerType,
            channel: NotificationChannel,
            enabled: Boolean = true,
            priority: NotificationPriority = triggerType.defaultPriority,
            frequency: Frequency = Frequency.IMMEDIATE,
        ): NotificationRule = NotificationRule(
            storeId = storeId,
            userId = userId,
            scopeType = scopeType,
            scopeId = scopeId,
            category = category,
            triggerType = triggerType,
            channel = channel,
            enabled = enabled,
            priority = priority,
            frequency = frequency,
        )
    }
}
