package com.closet.promotion.domain.timesale

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "time_sale_order")
@EntityListeners(AuditingEntityListener::class)
class TimeSaleOrder(
    @Column(name = "time_sale_id", nullable = false)
    val timeSaleId: Long,
    @Column(name = "order_id", nullable = false)
    val orderId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    @Column(name = "purchased_at", nullable = false, columnDefinition = "DATETIME(6)")
    val purchasedAt: ZonedDateTime = ZonedDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    companion object {
        fun create(
            timeSaleId: Long,
            orderId: Long,
            memberId: Long,
            quantity: Int,
        ): TimeSaleOrder {
            require(quantity > 0) { "구매 수량은 1 이상이어야 합니다" }
            return TimeSaleOrder(
                timeSaleId = timeSaleId,
                orderId = orderId,
                memberId = memberId,
                quantity = quantity,
            )
        }
    }
}
