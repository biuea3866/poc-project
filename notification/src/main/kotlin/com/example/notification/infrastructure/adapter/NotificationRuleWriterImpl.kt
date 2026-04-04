package com.example.notification.infrastructure.adapter

import com.example.notification.application.port.NotificationRuleWriter
import com.example.notification.domain.model.NotificationRule
import com.example.notification.infrastructure.entity.NotificationRuleEntity
import com.example.notification.infrastructure.repository.NotificationRuleJpaRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationRuleWriterImpl(
    private val jpaRepository: NotificationRuleJpaRepository,
) : NotificationRuleWriter {

    override fun save(rule: NotificationRule): NotificationRule =
        jpaRepository.save(rule.toEntity()).toDomain()

    override fun saveAll(rules: List<NotificationRule>): List<NotificationRule> =
        jpaRepository.saveAll(rules.map { it.toEntity() }).map { it.toDomain() }

    override fun upsert(rule: NotificationRule): NotificationRule {
        val existing = if (rule.userId != null) {
            jpaRepository.findByStoreIdAndUserIdAndTriggerType(rule.storeId, rule.userId, rule.triggerType)
                .firstOrNull { it.channel == rule.channel && it.scopeType == rule.scopeType && it.scopeId == rule.scopeId }
        } else {
            jpaRepository.findByStoreIdAndTriggerTypeAndUserIdIsNull(rule.storeId, rule.triggerType)
                .firstOrNull { it.channel == rule.channel && it.scopeType == rule.scopeType && it.scopeId == rule.scopeId }
        }

        val entityToSave = if (existing != null) {
            rule.toEntity(existing.id)
        } else {
            rule.toEntity()
        }

        return jpaRepository.save(entityToSave).toDomain()
    }

    companion object {
        private fun NotificationRule.toEntity(entityId: Long = 0L) = NotificationRuleEntity(
            id = entityId,
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
