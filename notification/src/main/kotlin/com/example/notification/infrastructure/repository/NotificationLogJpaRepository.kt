package com.example.notification.infrastructure.repository

import com.example.notification.infrastructure.entity.NotificationLogEntity
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationLogJpaRepository : JpaRepository<NotificationLogEntity, Long> {
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}
