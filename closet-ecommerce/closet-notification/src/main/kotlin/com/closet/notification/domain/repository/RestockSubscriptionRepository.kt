package com.closet.notification.domain.repository

import com.closet.notification.domain.RestockSubscription
import org.springframework.data.jpa.repository.JpaRepository

interface RestockSubscriptionRepository : JpaRepository<RestockSubscription, Long> {
    fun findByMemberIdAndDeletedAtIsNull(memberId: Long): List<RestockSubscription>

    fun findByMemberIdAndProductOptionIdAndDeletedAtIsNull(
        memberId: Long,
        productOptionId: Long,
    ): RestockSubscription?

    fun findByProductOptionIdAndIsNotifiedFalseAndDeletedAtIsNull(
        productOptionId: Long,
    ): List<RestockSubscription>

    fun existsByMemberIdAndProductOptionIdAndDeletedAtIsNull(
        memberId: Long,
        productOptionId: Long,
    ): Boolean
}
