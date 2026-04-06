package com.closet.order.domain.order

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
import java.time.ZonedDateTime

@Entity
@Table(name = "order_status_history")
@EntityListeners(AuditingEntityListener::class)
class OrderStatusHistory(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30, columnDefinition = "VARCHAR(30)")
    val fromStatus: OrderStatus?,

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val toStatus: OrderStatus,

    @Column(name = "reason", length = 500)
    val reason: String? = null,

    @Column(name = "changed_by", length = 100)
    val changedBy: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    companion object {
        fun create(
            orderId: Long,
            fromStatus: OrderStatus?,
            toStatus: OrderStatus,
            reason: String? = null,
            changedBy: String? = null,
        ): OrderStatusHistory {
            return OrderStatusHistory(
                orderId = orderId,
                fromStatus = fromStatus,
                toStatus = toStatus,
                reason = reason,
                changedBy = changedBy,
            )
        }
    }
}
