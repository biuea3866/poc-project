package com.closet.notification.domain

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 재입고 알림 구독
 */
@Entity
@Table(name = "restock_subscription")
class RestockSubscription(
    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "product_option_id", nullable = false)
    val productOptionId: Long,

    @Column(name = "is_notified", nullable = false)
    var isNotified: Boolean = false,

    @Column(name = "subscribed_at", nullable = false, columnDefinition = "DATETIME(6)")
    val subscribedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "notified_at", columnDefinition = "DATETIME(6)")
    var notifiedAt: LocalDateTime? = null,
) : BaseEntity() {

    companion object {
        fun create(memberId: Long, productOptionId: Long): RestockSubscription {
            return RestockSubscription(
                memberId = memberId,
                productOptionId = productOptionId,
                isNotified = false,
                subscribedAt = LocalDateTime.now(),
            )
        }
    }

    /** 알림 발송 처리 */
    fun markNotified() {
        this.isNotified = true
        this.notifiedAt = LocalDateTime.now()
    }
}
