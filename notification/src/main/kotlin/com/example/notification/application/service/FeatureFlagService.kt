package com.example.notification.application.service

import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.infrastructure.entity.SimpleRuntimeConfigEntity
import com.example.notification.infrastructure.repository.SimpleRuntimeConfigRepository
import org.springframework.stereotype.Component

/**
 * DB 기반 Feature Flag.
 * triggerType + storeId 조합으로 Flag 관리.
 *
 * 실제로는 Retool 등에서 배포 없이 런타임 ON/OFF.
 */
@Component
class FeatureFlagService(
    private val repository: SimpleRuntimeConfigRepository,
) {
    fun isEnabled(trigger: NotificationTriggerType, storeId: Long): Boolean {
        val specificKey = "${trigger.name}:$storeId"
        val specificConfig = repository.findById(specificKey)
        if (specificConfig.isPresent) {
            return specificConfig.get().value.toBoolean()
        }

        val globalConfig = repository.findById(trigger.name)
        if (globalConfig.isPresent) {
            return globalConfig.get().value.toBoolean()
        }

        return false
    }

    fun enable(trigger: NotificationTriggerType, storeId: Long) {
        val key = "${trigger.name}:$storeId"
        repository.save(SimpleRuntimeConfigEntity(key = key, value = "true"))
    }

    fun disable(trigger: NotificationTriggerType, storeId: Long) {
        val key = "${trigger.name}:$storeId"
        repository.save(SimpleRuntimeConfigEntity(key = key, value = "false"))
    }

    fun enableGlobal(trigger: NotificationTriggerType) {
        repository.save(SimpleRuntimeConfigEntity(key = trigger.name, value = "true"))
    }

    fun disableGlobal(trigger: NotificationTriggerType) {
        repository.save(SimpleRuntimeConfigEntity(key = trigger.name, value = "false"))
    }
}
