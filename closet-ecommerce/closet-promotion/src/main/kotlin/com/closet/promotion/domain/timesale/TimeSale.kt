package com.closet.promotion.domain.timesale

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
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
@Table(name = "time_sale")
@EntityListeners(AuditingEntityListener::class)
class TimeSale(
    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "sale_price", nullable = false, columnDefinition = "DECIMAL(15,2)")
    val salePrice: BigDecimal,

    @Column(name = "limit_quantity", nullable = false)
    val limitQuantity: Int,

    @Column(name = "sold_count", nullable = false)
    var soldCount: Int = 0,

    @Column(name = "start_at", nullable = false, columnDefinition = "DATETIME(6)")
    val startAt: LocalDateTime,

    @Column(name = "end_at", nullable = false, columnDefinition = "DATETIME(6)")
    val endAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: TimeSaleStatus = TimeSaleStatus.SCHEDULED,
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

    fun isExhausted(): Boolean = soldCount >= limitQuantity

    fun start() {
        status.validateTransitionTo(TimeSaleStatus.ACTIVE)
        status = TimeSaleStatus.ACTIVE
    }

    fun end() {
        status.validateTransitionTo(TimeSaleStatus.ENDED)
        status = TimeSaleStatus.ENDED
    }

    fun purchase() {
        if (status != TimeSaleStatus.ACTIVE) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "진행 중인 타임세일만 구매할 수 있습니다. status=${status.name}"
            )
        }
        if (isExhausted()) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "타임세일 수량이 소진되었습니다"
            )
        }

        soldCount++

        if (isExhausted()) {
            status = TimeSaleStatus.EXHAUSTED
        }
    }

    companion object {
        fun create(
            productId: Long,
            salePrice: BigDecimal,
            limitQuantity: Int,
            startAt: LocalDateTime,
            endAt: LocalDateTime,
        ): TimeSale {
            require(limitQuantity > 0) { "한정 수량은 1 이상이어야 합니다" }
            require(salePrice > BigDecimal.ZERO) { "세일 가격은 0보다 커야 합니다" }
            require(endAt.isAfter(startAt)) { "종료일은 시작일 이후여야 합니다" }

            return TimeSale(
                productId = productId,
                salePrice = salePrice,
                limitQuantity = limitQuantity,
                startAt = startAt,
                endAt = endAt,
            )
        }
    }
}
