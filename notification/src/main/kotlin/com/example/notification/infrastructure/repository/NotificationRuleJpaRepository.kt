package com.example.notification.infrastructure.repository

import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.infrastructure.entity.NotificationRuleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRuleJpaRepository : JpaRepository<NotificationRuleEntity, Long> {
    fun findByStoreIdAndTriggerTypeAndUserIdIsNull(storeId: Long, triggerType: NotificationTriggerType): List<NotificationRuleEntity>
    fun findByStoreIdAndUserIdAndTriggerType(storeId: Long, userId: Long, triggerType: NotificationTriggerType): List<NotificationRuleEntity>
    fun existsByUserIdAndStoreId(userId: Long, storeId: Long): Boolean
}
