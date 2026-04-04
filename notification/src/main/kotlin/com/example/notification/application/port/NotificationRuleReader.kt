// 패턴: 통합 알림 시스템의 규칙 조회 포트
package com.example.notification.application.port

import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.NotificationRule

interface NotificationRuleReader {
    fun findStorePolicies(storeId: Long, triggerType: NotificationTriggerType): List<NotificationRule>
    fun findUserRules(storeId: Long, userId: Long, triggerType: NotificationTriggerType): List<NotificationRule>
    fun existsByUserAndStore(userId: Long, storeId: Long): Boolean
}
