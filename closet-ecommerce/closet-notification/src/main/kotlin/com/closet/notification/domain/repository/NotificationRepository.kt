package com.closet.notification.domain.repository

import com.closet.notification.domain.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId: Long, pageable: Pageable): Page<Notification>
    fun countByMemberIdAndIsReadFalseAndDeletedAtIsNull(memberId: Long): Long
    fun findByIdAndDeletedAtIsNull(id: Long): Notification?
}
