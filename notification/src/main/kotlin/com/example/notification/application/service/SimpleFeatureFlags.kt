// 패턴: 통합 알림 시스템의 Feature Flag
package com.example.notification.application.service

import com.example.notification.domain.enums.NotificationTriggerType
import java.util.concurrent.ConcurrentHashMap

/**
 * 간단한 인메모리 Feature Flag.
 * triggerType + storeId 조합으로 Flag 관리
 *
 * 실제로는 Retool 등에서 배포 없이 런타임 ON/OFF.
 */
class SimpleFeatureFlags {
    private val flags = ConcurrentHashMap<String, Boolean>()

    fun isEnabled(trigger: NotificationTriggerType, storeId: Long): Boolean {
        val key = "${trigger.name}:$storeId"
        return flags[key] ?: flags[trigger.name] ?: false
    }

    fun enable(trigger: NotificationTriggerType, storeId: Long) {
        flags["${trigger.name}:$storeId"] = true
    }

    fun disable(trigger: NotificationTriggerType, storeId: Long) {
        flags["${trigger.name}:$storeId"] = false
    }

    fun enableGlobal(trigger: NotificationTriggerType) {
        flags[trigger.name] = true
    }

    fun disableGlobal(trigger: NotificationTriggerType) {
        flags[trigger.name] = false
    }
}
