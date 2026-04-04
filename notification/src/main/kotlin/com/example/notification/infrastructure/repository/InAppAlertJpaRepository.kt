package com.example.notification.infrastructure.repository

import com.example.notification.infrastructure.entity.InAppAlertEntity
import org.springframework.data.jpa.repository.JpaRepository

interface InAppAlertJpaRepository : JpaRepository<InAppAlertEntity, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<InAppAlertEntity>
}
