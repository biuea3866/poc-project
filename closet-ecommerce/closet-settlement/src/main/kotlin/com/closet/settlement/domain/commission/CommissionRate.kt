package com.closet.settlement.domain.commission

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
import java.time.LocalDateTime

@Entity
@Table(name = "commission_rate")
@EntityListeners(AuditingEntityListener::class)
class CommissionRate(
    @Column(name = "category_id", nullable = false)
    val categoryId: Long,

    @Column(name = "rate", nullable = false, columnDefinition = "DECIMAL(5,4)")
    val rate: BigDecimal,

    @Column(name = "effective_from", nullable = false, columnDefinition = "DATETIME(6)")
    val effectiveFrom: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    init {
        require(rate >= BigDecimal("0.10") && rate <= BigDecimal("0.30")) {
            "수수료율은 0.10~0.30 범위여야 합니다. 입력값: $rate"
        }
    }

    companion object {
        fun create(
            categoryId: Long,
            rate: BigDecimal,
            effectiveFrom: LocalDateTime = LocalDateTime.now(),
        ): CommissionRate {
            return CommissionRate(
                categoryId = categoryId,
                rate = rate,
                effectiveFrom = effectiveFrom,
            )
        }
    }
}
