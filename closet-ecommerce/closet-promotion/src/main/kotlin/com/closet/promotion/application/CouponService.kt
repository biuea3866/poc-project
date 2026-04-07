package com.closet.promotion.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.promotion.domain.coupon.Coupon
import com.closet.promotion.presentation.dto.CouponResponse
import com.closet.promotion.presentation.dto.CouponValidationResponse
import com.closet.promotion.presentation.dto.CreateCouponRequest
import com.closet.promotion.presentation.dto.MemberCouponResponse
import com.closet.promotion.repository.CouponRepository
import com.closet.promotion.repository.MemberCouponRepository
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class CouponService(
    private val couponRepository: CouponRepository,
    private val memberCouponRepository: MemberCouponRepository,
    private val redisTemplate: StringRedisTemplate,
) {
    @Transactional
    fun createCoupon(request: CreateCouponRequest): CouponResponse {
        val coupon =
            Coupon.create(
                name = request.name,
                couponType = request.couponType,
                discountValue = request.discountValue,
                maxDiscountAmount = request.maxDiscountAmount,
                minOrderAmount = request.minOrderAmount,
                scope = request.scope,
                scopeIds = request.scopeIds,
                totalQuantity = request.totalQuantity,
                validFrom = request.validFrom,
                validTo = request.validTo,
            )

        val saved = couponRepository.save(coupon)

        // Redis에 선착순 발급용 재고 세팅
        val redisKey = couponStockKey(saved.id)
        redisTemplate.opsForValue().set(redisKey, saved.totalQuantity.toString())

        logger.info { "쿠폰 생성 완료: couponId=${saved.id}, name=${saved.name}" }
        return CouponResponse.from(saved)
    }

    @Transactional
    fun issueCoupon(
        couponId: Long,
        memberId: Long,
    ): MemberCouponResponse {
        // Redis DECR로 선착순 동시성 제어
        val redisKey = couponStockKey(couponId)
        val remaining =
            redisTemplate.opsForValue().decrement(redisKey)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "쿠폰 재고 정보를 찾을 수 없습니다. couponId=$couponId")

        if (remaining < 0) {
            redisTemplate.opsForValue().increment(redisKey)
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "쿠폰 수량이 소진되었습니다")
        }

        // 중복 발급 체크
        if (memberCouponRepository.existsByCouponIdAndMemberId(couponId, memberId)) {
            redisTemplate.opsForValue().increment(redisKey)
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 발급받은 쿠폰입니다")
        }

        val coupon =
            couponRepository.findByIdOrNull(couponId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "쿠폰을 찾을 수 없습니다. id=$couponId")

        val memberCoupon = coupon.issue(memberId)
        val saved = memberCouponRepository.save(memberCoupon)

        couponRepository.save(coupon)

        logger.info { "쿠폰 발급 완료: couponId=$couponId, memberId=$memberId" }
        return MemberCouponResponse.from(saved, coupon)
    }

    @Transactional
    fun useCoupon(
        couponId: Long,
        orderId: Long,
        memberId: Long,
    ): MemberCouponResponse {
        val memberCoupons = memberCouponRepository.findByMemberId(memberId)
        val memberCoupon =
            memberCoupons.find { it.couponId == couponId }
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "해당 쿠폰을 보유하고 있지 않습니다")

        memberCoupon.use(orderId)

        val coupon =
            couponRepository.findByIdOrNull(couponId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "쿠폰을 찾을 수 없습니다. id=$couponId")

        logger.info { "쿠폰 사용 완료: couponId=$couponId, memberId=$memberId, orderId=$orderId" }
        return MemberCouponResponse.from(memberCoupon, coupon)
    }

    fun getMyCoupons(memberId: Long): List<MemberCouponResponse> {
        val memberCoupons = memberCouponRepository.findByMemberId(memberId)
        val couponIds = memberCoupons.map { it.couponId }.distinct()
        val coupons = couponRepository.findAllById(couponIds).associateBy { it.id }

        return memberCoupons.map { mc ->
            MemberCouponResponse.from(mc, coupons[mc.couponId])
        }
    }

    fun validateCoupon(
        couponId: Long,
        orderAmount: BigDecimal,
    ): CouponValidationResponse {
        val coupon =
            couponRepository.findByIdOrNull(couponId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "쿠폰을 찾을 수 없습니다. id=$couponId")

        val isValid = coupon.isValid() && orderAmount >= coupon.minOrderAmount
        val discountAmount =
            if (isValid) {
                coupon.calculateDiscount(orderAmount)
            } else {
                BigDecimal.ZERO
            }

        return CouponValidationResponse(
            couponId = coupon.id,
            couponName = coupon.name,
            couponType = coupon.couponType,
            discountAmount = discountAmount,
            isValid = isValid,
        )
    }

    private fun couponStockKey(couponId: Long): String = "coupon:stock:$couponId"
}
