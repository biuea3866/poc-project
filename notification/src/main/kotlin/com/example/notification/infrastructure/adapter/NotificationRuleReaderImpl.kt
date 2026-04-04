package com.example.notification.infrastructure.adapter

import com.example.notification.application.port.NotificationRuleReader
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.NotificationRule
import com.example.notification.infrastructure.repository.NotificationRuleJpaRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationRuleReaderImpl(
    private val jpaRepository: NotificationRuleJpaRepository,
) : NotificationRuleReader {

    override fun findStorePolicies(storeId: Long, triggerType: NotificationTriggerType): List<NotificationRule> =
        jpaRepository.findByStoreIdAndTriggerTypeAndUserIdIsNull(storeId, triggerType)
            .map { it.toDomain() }

    override fun findUserRules(storeId: Long, userId: Long, triggerType: NotificationTriggerType): List<NotificationRule> =
        jpaRepository.findByStoreIdAndUserIdAndTriggerType(storeId, userId, triggerType)
            .map { it.toDomain() }

    override fun existsByUserAndStore(userId: Long, storeId: Long): Boolean =
        jpaRepository.existsByUserIdAndStoreId(userId, storeId)
}
