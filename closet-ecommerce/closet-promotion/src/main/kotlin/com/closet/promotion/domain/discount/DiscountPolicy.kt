package com.closet.promotion.domain.discount

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
import java.math.RoundingMode
import java.time.ZonedDateTime

@Entity
@Table(name = "discount_policy")
@EntityListeners(AuditingEntityListener::class)
class DiscountPolicy(
    @Column(name = "name", nullable = false, length = 100)
    val name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    val discountType: DiscountType,
    @Column(name = "discount_value", nullable = false, columnDefinition = "DECIMAL(10,2)")
    val discountValue: BigDecimal,
    @Column(name = "max_discount_amount", columnDefinition = "DECIMAL(12,2)")
    val maxDiscountAmount: BigDecimal? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    val conditionType: ConditionType,
    @Column(name = "condition_value", nullable = false, length = 100)
    val conditionValue: String = "",
    @Column(name = "priority", nullable = false)
    val priority: Int = 1,
    @Column(name = "is_stackable", nullable = false, columnDefinition = "TINYINT(1)")
    val isStackable: Boolean = false,
    @Column(name = "started_at", nullable = false, columnDefinition = "DATETIME(6)")
    val startedAt: ZonedDateTime,
    @Column(name = "ended_at", nullable = false, columnDefinition = "DATETIME(6)")
    val endedAt: ZonedDateTime,
    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    var isActive: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    var deletedAt: ZonedDateTime? = null

    fun isCurrentlyActive(): Boolean {
        val now = ZonedDateTime.now()
        return isActive && now.isAfter(startedAt) && now.isBefore(endedAt)
    }

    fun deactivate() {
        isActive = false
    }

    fun calculateDiscount(orderAmount: BigDecimal): BigDecimal {
        val discount =
            when (discountType) {
                DiscountType.FIXED -> discountValue
                DiscountType.PERCENT -> {
                    val calculated =
                        orderAmount.multiply(discountValue)
                            .divide(BigDecimal(100), 0, RoundingMode.DOWN)
                    if (maxDiscountAmount != null && calculated > maxDiscountAmount) {
                        maxDiscountAmount
                    } else {
                        calculated
                    }
                }
            }
        return discount.min(orderAmount)
    }

    fun matchesCondition(
        categoryId: Long? = null,
        brandId: Long? = null,
        orderAmount: BigDecimal? = null,
    ): Boolean =
        when (conditionType) {
            ConditionType.ALL -> true
            ConditionType.CATEGORY -> categoryId != null && conditionValue == categoryId.toString()
            ConditionType.BRAND -> brandId != null && conditionValue == brandId.toString()
            ConditionType.AMOUNT_RANGE -> {
                val minAmount = conditionValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
                orderAmount != null && orderAmount >= minAmount
            }
        }

    companion object {
        fun create(
            name: String,
            discountType: DiscountType,
            discountValue: BigDecimal,
            maxDiscountAmount: BigDecimal? = null,
            conditionType: ConditionType,
            conditionValue: String = "",
            priority: Int = 1,
            isStackable: Boolean = false,
            startedAt: ZonedDateTime,
            endedAt: ZonedDateTime,
        ): DiscountPolicy {
            require(discountValue > BigDecimal.ZERO) { "할인 값은 0보다 커야 합니다" }
            require(endedAt.isAfter(startedAt)) { "종료일은 시작일 이후여야 합니다" }
            if (discountType == DiscountType.PERCENT) {
                require(discountValue <= BigDecimal(100)) { "정률 할인은 100% 이하여야 합니다" }
            }

            return DiscountPolicy(
                name = name,
                discountType = discountType,
                discountValue = discountValue,
                maxDiscountAmount = maxDiscountAmount,
                conditionType = conditionType,
                conditionValue = conditionValue,
                priority = priority,
                isStackable = isStackable,
                startedAt = startedAt,
                endedAt = endedAt,
            )
        }
    }
}
