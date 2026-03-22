package com.closet.settlement.domain.settlement

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
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "settlement")
@EntityListeners(AuditingEntityListener::class)
class Settlement(
    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,

    @Column(name = "period_from", nullable = false, columnDefinition = "DATETIME(6)")
    val periodFrom: LocalDateTime,

    @Column(name = "period_to", nullable = false, columnDefinition = "DATETIME(6)")
    val periodTo: LocalDateTime,

    @Column(name = "total_sales", nullable = false, columnDefinition = "DECIMAL(15,2)")
    var totalSales: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_commission", nullable = false, columnDefinition = "DECIMAL(15,2)")
    var totalCommission: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_refund", nullable = false, columnDefinition = "DECIMAL(15,2)")
    var totalRefund: BigDecimal = BigDecimal.ZERO,

    @Column(name = "net_amount", nullable = false, columnDefinition = "DECIMAL(15,2)")
    var netAmount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: SettlementStatus = SettlementStatus.PENDING,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: LocalDateTime

    private fun transitionTo(newStatus: SettlementStatus) {
        status.validateTransitionTo(newStatus)
        status = newStatus
    }

    fun calculate(items: List<SettlementItem>, totalRefund: BigDecimal = BigDecimal.ZERO) {
        require(status == SettlementStatus.PENDING) {
            "정산 계산은 PENDING 상태에서만 가능합니다. 현재 상태: ${status.name}"
        }
        this.totalSales = items.sumOf { it.saleAmount }
        this.totalCommission = items.sumOf { it.commissionAmount }
        this.totalRefund = totalRefund
        this.netAmount = this.totalSales - this.totalCommission - this.totalRefund
        transitionTo(SettlementStatus.CALCULATED)
    }

    fun confirm() {
        transitionTo(SettlementStatus.CONFIRMED)
    }

    fun pay() {
        transitionTo(SettlementStatus.PAID)
    }

    companion object {
        fun create(
            sellerId: Long,
            periodFrom: LocalDateTime,
            periodTo: LocalDateTime,
        ): Settlement {
            require(periodFrom.isBefore(periodTo)) {
                "정산 시작일은 종료일보다 이전이어야 합니다"
            }
            return Settlement(
                sellerId = sellerId,
                periodFrom = periodFrom,
                periodTo = periodTo,
            )
        }
    }
}
