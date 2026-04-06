package com.closet.promotion.presentation.dto

import com.closet.promotion.domain.coupon.Coupon
import com.closet.promotion.domain.coupon.CouponScope
import com.closet.promotion.domain.coupon.CouponStatus
import com.closet.promotion.domain.coupon.CouponType
import com.closet.promotion.domain.coupon.MemberCoupon
import com.closet.promotion.domain.coupon.MemberCouponStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CreateCouponRequest(
    @field:NotBlank val name: String,
    @field:NotNull val couponType: CouponType,
    @field:NotNull @field:Positive val discountValue: BigDecimal,
    val maxDiscountAmount: BigDecimal? = null,
    val minOrderAmount: BigDecimal = BigDecimal.ZERO,
    val scope: CouponScope = CouponScope.ALL,
    val scopeIds: String? = null,
    @field:Positive val totalQuantity: Int,
    @field:NotNull val validFrom: ZonedDateTime,
    @field:NotNull val validTo: ZonedDateTime,
)

data class IssueCouponRequest(
    @field:NotNull val memberId: Long,
)

data class UseCouponRequest(
    @field:NotNull val orderId: Long,
    @field:NotNull val memberId: Long,
)

data class CouponResponse(
    val id: Long,
    val name: String,
    val couponType: CouponType,
    val discountValue: BigDecimal,
    val maxDiscountAmount: BigDecimal?,
    val minOrderAmount: BigDecimal,
    val scope: CouponScope,
    val scopeIds: String?,
    val totalQuantity: Int,
    val issuedCount: Int,
    val validFrom: ZonedDateTime,
    val validTo: ZonedDateTime,
    val status: CouponStatus,
) {
    companion object {
        fun from(coupon: Coupon): CouponResponse =
            CouponResponse(
                id = coupon.id,
                name = coupon.name,
                couponType = coupon.couponType,
                discountValue = coupon.discountValue,
                maxDiscountAmount = coupon.maxDiscountAmount,
                minOrderAmount = coupon.minOrderAmount,
                scope = coupon.scope,
                scopeIds = coupon.scopeIds,
                totalQuantity = coupon.totalQuantity,
                issuedCount = coupon.issuedCount,
                validFrom = coupon.validFrom,
                validTo = coupon.validTo,
                status = coupon.status,
            )
    }
}

data class MemberCouponResponse(
    val id: Long,
    val couponId: Long,
    val memberId: Long,
    val status: MemberCouponStatus,
    val usedOrderId: Long?,
    val issuedAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val expiredAt: ZonedDateTime,
    val couponName: String?,
    val couponType: CouponType?,
    val discountValue: BigDecimal?,
) {
    companion object {
        fun from(
            memberCoupon: MemberCoupon,
            coupon: Coupon? = null,
        ): MemberCouponResponse =
            MemberCouponResponse(
                id = memberCoupon.id,
                couponId = memberCoupon.couponId,
                memberId = memberCoupon.memberId,
                status = memberCoupon.status,
                usedOrderId = memberCoupon.usedOrderId,
                issuedAt = memberCoupon.issuedAt,
                usedAt = memberCoupon.usedAt,
                expiredAt = memberCoupon.expiredAt,
                couponName = coupon?.name,
                couponType = coupon?.couponType,
                discountValue = coupon?.discountValue,
            )
    }
}

data class CouponValidationResponse(
    val couponId: Long,
    val couponName: String,
    val couponType: CouponType,
    val discountAmount: BigDecimal,
    val isValid: Boolean,
)
