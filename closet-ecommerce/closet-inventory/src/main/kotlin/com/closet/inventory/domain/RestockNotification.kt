package com.closet.inventory.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "restock_notification")
@EntityListeners(AuditingEntityListener::class)
class RestockNotification(
    @Column(name = "product_option_id", nullable = false)
    val productOptionId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    var status: RestockNotificationStatus = RestockNotificationStatus.WAITING,

    @Column(name = "expired_at", nullable = false, columnDefinition = "DATETIME(6)")
    val expiredAt: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    @Column(name = "notified_at", columnDefinition = "DATETIME(6)")
    var notifiedAt: LocalDateTime? = null

    fun markNotified() {
        this.status = RestockNotificationStatus.NOTIFIED
        this.notifiedAt = LocalDateTime.now()
    }

    fun markExpired() {
        this.status = RestockNotificationStatus.EXPIRED
    }

    fun isExpired(): Boolean {
        return status == RestockNotificationStatus.EXPIRED || LocalDateTime.now().isAfter(expiredAt)
    }

    companion object {
        private const val EXPIRY_DAYS = 90L

        fun create(
            productOptionId: Long,
            memberId: Long,
        ): RestockNotification {
            val now = LocalDateTime.now()
            return RestockNotification(
                productOptionId = productOptionId,
                memberId = memberId,
                expiredAt = now.plusDays(EXPIRY_DAYS),
            )
        }
    }
}
