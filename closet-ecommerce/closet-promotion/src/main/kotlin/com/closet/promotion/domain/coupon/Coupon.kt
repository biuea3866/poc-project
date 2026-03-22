package com.closet.promotion.domain.coupon

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.vo.Money
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
import java.time.LocalDateTime

@Entity
@Table(name = "coupon")
@EntityListeners(AuditingEntityListener::class)
class Coupon(
    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val couponType: CouponType,

    @Column(name = "discount_value", nullable = false, columnDefinition = "DECIMAL(15,2)")
    val discountValue: BigDecimal,

    @Column(name = "max_discount_amount", columnDefinition = "DECIMAL(15,2)")
    val maxDiscountAmount: BigDecimal? = null,

    @Column(name = "min_order_amount", nullable = false, columnDefinition = "DECIMAL(15,2)")
    val minOrderAmount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val scope: CouponScope = CouponScope.ALL,

    @Column(name = "scope_ids", columnDefinition = "TEXT")
    val scopeIds: String? = null,

    @Column(name = "total_quantity", nullable = false)
    val totalQuantity: Int,

    @Column(name = "issued_count", nullable = false)
    var issuedCount: Int = 0,

    @Column(name = "valid_from", nullable = false, columnDefinition = "DATETIME(6)")
    val validFrom: LocalDateTime,

    @Column(name = "valid_to", nullable = false, columnDefinition = "DATETIME(6)")
    val validTo: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: CouponStatus = CouponStatus.ACTIVE,
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

    fun isExhausted(): Boolean = issuedCount >= totalQuantity

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(validTo)

    fun isValid(): Boolean = status == CouponStatus.ACTIVE && !isExpired() && !isExhausted()

    fun issue(memberId: Long): MemberCoupon {
        if (!isValid()) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "쿠폰을 발급할 수 없는 상태입니다. status=${status.name}")
        }
        if (isExhausted()) {
            status = CouponStatus.EXHAUSTED
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "쿠폰 수량이 소진되었습니다")
        }

        issuedCount++

        if (isExhausted()) {
            status = CouponStatus.EXHAUSTED
        }

        return MemberCoupon(
            couponId = this.id,
            memberId = memberId,
            status = MemberCouponStatus.AVAILABLE,
            issuedAt = LocalDateTime.now(),
            expiredAt = this.validTo,
        )
    }

    fun calculateDiscount(orderAmount: BigDecimal): BigDecimal {
        if (orderAmount < minOrderAmount) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "최소 주문 금액(${minOrderAmount}원) 이상이어야 합니다. 현재 주문 금액: ${orderAmount}원"
            )
        }

        val discount = when (couponType) {
            CouponType.FIXED_AMOUNT -> discountValue
            CouponType.PERCENTAGE -> {
                val calculated = orderAmount.multiply(discountValue).divide(BigDecimal(100), 0, RoundingMode.DOWN)
                if (maxDiscountAmount != null && calculated > maxDiscountAmount) {
                    maxDiscountAmount
                } else {
                    calculated
                }
            }
        }

        return discount.min(orderAmount)
    }

    companion object {
        fun create(
            name: String,
            couponType: CouponType,
            discountValue: BigDecimal,
            maxDiscountAmount: BigDecimal? = null,
            minOrderAmount: BigDecimal = BigDecimal.ZERO,
            scope: CouponScope = CouponScope.ALL,
            scopeIds: String? = null,
            totalQuantity: Int,
            validFrom: LocalDateTime,
            validTo: LocalDateTime,
        ): Coupon {
            require(totalQuantity > 0) { "총 발급 수량은 1 이상이어야 합니다" }
            require(validTo.isAfter(validFrom)) { "유효 종료일은 시작일 이후여야 합니다" }
            require(discountValue > BigDecimal.ZERO) { "할인 값은 0보다 커야 합니다" }

            if (couponType == CouponType.PERCENTAGE) {
                require(discountValue <= BigDecimal(100)) { "정률 할인은 100% 이하여야 합니다" }
            }

            return Coupon(
                name = name,
                couponType = couponType,
                discountValue = discountValue,
                maxDiscountAmount = maxDiscountAmount,
                minOrderAmount = minOrderAmount,
                scope = scope,
                scopeIds = scopeIds,
                totalQuantity = totalQuantity,
                validFrom = validFrom,
                validTo = validTo,
            )
        }
    }
}
