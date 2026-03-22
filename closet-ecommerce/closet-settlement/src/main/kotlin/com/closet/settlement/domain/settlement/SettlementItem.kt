package com.closet.settlement.domain.settlement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Entity
@Table(name = "settlement_item")
@EntityListeners(AuditingEntityListener::class)
class SettlementItem(
    @Column(name = "settlement_id", nullable = false)
    val settlementId: Long,

    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "order_item_id", nullable = false)
    val orderItemId: Long,

    @Column(name = "sale_amount", nullable = false, columnDefinition = "DECIMAL(15,2)")
    val saleAmount: BigDecimal,

    @Column(name = "commission_rate", nullable = false, columnDefinition = "DECIMAL(5,4)")
    val commissionRate: BigDecimal,

    @Column(name = "commission_amount", nullable = false, columnDefinition = "DECIMAL(15,2)")
    val commissionAmount: BigDecimal,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    companion object {
        fun create(
            settlementId: Long = 0,
            orderId: Long,
            orderItemId: Long,
            saleAmount: BigDecimal,
            commissionRate: BigDecimal,
        ): SettlementItem {
            val commissionAmount = saleAmount.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP)
            return SettlementItem(
                settlementId = settlementId,
                orderId = orderId,
                orderItemId = orderItemId,
                saleAmount = saleAmount,
                commissionRate = commissionRate,
                commissionAmount = commissionAmount,
            )
        }
    }
}
