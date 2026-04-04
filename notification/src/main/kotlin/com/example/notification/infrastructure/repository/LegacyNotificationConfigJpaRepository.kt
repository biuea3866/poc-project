package com.example.notification.infrastructure.repository

import com.example.notification.infrastructure.entity.LegacyNotificationConfigEntity
import org.springframework.data.jpa.repository.JpaRepository

interface LegacyNotificationConfigJpaRepository : JpaRepository<LegacyNotificationConfigEntity, Long> {
    fun findByUserId(userId: Long): LegacyNotificationConfigEntity?
}
