package com.closet.notification.domain.repository

import com.closet.notification.domain.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, Long> {
    fun findByMemberIdAndDeletedAtIsNull(memberId: Long): NotificationPreference?
}
