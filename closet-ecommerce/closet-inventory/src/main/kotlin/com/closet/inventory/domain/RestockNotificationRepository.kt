package com.closet.inventory.domain

import org.springframework.data.jpa.repository.JpaRepository

interface RestockNotificationRepository : JpaRepository<RestockNotification, Long> {
    fun findByProductOptionIdAndStatus(
        productOptionId: Long,
        status: RestockNotificationStatus,
    ): List<RestockNotification>

    fun existsByProductOptionIdAndMemberIdAndStatus(
        productOptionId: Long,
        memberId: Long,
        status: RestockNotificationStatus,
    ): Boolean

    fun countByProductOptionIdAndStatus(
        productOptionId: Long,
        status: RestockNotificationStatus,
    ): Long
}
