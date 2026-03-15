package com.biuea.wiki.infrastructure.notification

import com.biuea.wiki.domain.notification.entity.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByTargetUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>

    fun findByTargetUserIdAndIsReadFalseOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>

    fun countByTargetUserIdAndIsReadFalse(userId: Long): Long

    fun findByTargetUserIdAndIsReadFalse(userId: Long): List<Notification>

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.targetUserId = :userId AND n.isRead = false")
    fun markAllAsReadByUserId(userId: Long): Int
}
