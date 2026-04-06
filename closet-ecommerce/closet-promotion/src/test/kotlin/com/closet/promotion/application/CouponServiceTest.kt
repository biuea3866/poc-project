package com.closet.promotion.application

import com.closet.common.exception.BusinessException
import com.closet.promotion.domain.coupon.Coupon
import com.closet.promotion.domain.coupon.CouponType
import com.closet.promotion.domain.coupon.MemberCoupon
import com.closet.promotion.domain.coupon.MemberCouponStatus
import com.closet.promotion.presentation.dto.CreateCouponRequest
import com.closet.promotion.repository.CouponRepository
import com.closet.promotion.repository.MemberCouponRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Optional

class CouponServiceTest : BehaviorSpec({

    val couponRepository = mockk<CouponRepository>()
    val memberCouponRepository = mockk<MemberCouponRepository>()
    val redisTemplate = mockk<StringRedisTemplate>()
    val valueOps = mockk<ValueOperations<String, String>>()

    every { redisTemplate.opsForValue() } returns valueOps

    val couponService =
        CouponService(
            couponRepository = couponRepository,
            memberCouponRepository = memberCouponRepository,
            redisTemplate = redisTemplate,
        )

    Given("쿠폰 생성") {
        val request =
            CreateCouponRequest(
                name = "신규 가입 쿠폰",
                couponType = CouponType.FIXED_AMOUNT,
                discountValue = BigDecimal("5000"),
                minOrderAmount = BigDecimal("30000"),
                totalQuantity = 100,
                validFrom = ZonedDateTime.now(),
                validTo = ZonedDateTime.now().plusDays(30),
            )

        val couponSlot = slot<Coupon>()
        every { couponRepository.save(capture(couponSlot)) } answers { couponSlot.captured }
        every { valueOps.set(any(), any()) } returns Unit

        When("정상 생성 요청") {
            val response = couponService.createCoupon(request)

            Then("쿠폰이 생성된다") {
                response.name shouldBe "신규 가입 쿠폰"
                response.couponType shouldBe CouponType.FIXED_AMOUNT
                response.discountValue.compareTo(BigDecimal("5000")) shouldBe 0
                response.totalQuantity shouldBe 100
            }
        }
    }

    Given("쿠폰 발급") {
        val coupon =
            Coupon.create(
                name = "선착순 쿠폰",
                couponType = CouponType.FIXED_AMOUNT,
                discountValue = BigDecimal("3000"),
                totalQuantity = 10,
                validFrom = ZonedDateTime.now().minusDays(1),
                validTo = ZonedDateTime.now().plusDays(30),
            )

        When("정상 발급") {
            every { valueOps.decrement(any()) } returns 9L
            every { memberCouponRepository.existsByCouponIdAndMemberId(any(), any()) } returns false
            every { couponRepository.findById(any()) } returns Optional.of(coupon)

            val memberCouponSlot = slot<MemberCoupon>()
            every { memberCouponRepository.save(capture(memberCouponSlot)) } answers { memberCouponSlot.captured }
            every { couponRepository.save(any()) } answers { firstArg() }

            val response = couponService.issueCoupon(coupon.id, 1L)

            Then("MemberCoupon이 AVAILABLE 상태로 생성된다") {
                response.status shouldBe MemberCouponStatus.AVAILABLE
                response.memberId shouldBe 1L
            }
        }

        When("Redis 재고 소진") {
            every { valueOps.decrement(any()) } returns -1L
            every { valueOps.increment(any()) } returns 0L

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    couponService.issueCoupon(coupon.id, 2L)
                }
            }
        }

        When("중복 발급 시도") {
            every { valueOps.decrement(any()) } returns 5L
            every { memberCouponRepository.existsByCouponIdAndMemberId(any(), any()) } returns true
            every { valueOps.increment(any()) } returns 6L

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    couponService.issueCoupon(coupon.id, 1L)
                }
            }
        }
    }

    Given("쿠폰 검증") {
        val coupon =
            Coupon.create(
                name = "검증 테스트 쿠폰",
                couponType = CouponType.PERCENTAGE,
                discountValue = BigDecimal("10"),
                maxDiscountAmount = BigDecimal("10000"),
                minOrderAmount = BigDecimal("30000"),
                totalQuantity = 50,
                validFrom = ZonedDateTime.now().minusDays(1),
                validTo = ZonedDateTime.now().plusDays(30),
            )

        every { couponRepository.findById(any()) } returns Optional.of(coupon)

        When("유효한 주문 금액으로 검증") {
            val response = couponService.validateCoupon(coupon.id, BigDecimal("50000"))

            Then("할인 금액이 계산된다") {
                response.isValid shouldBe true
                // 50000 * 10% = 5000
                response.discountAmount.compareTo(BigDecimal("5000")) shouldBe 0
            }
        }

        When("최소 주문 금액 미달로 검증") {
            val response = couponService.validateCoupon(coupon.id, BigDecimal("20000"))

            Then("유효하지 않음") {
                response.isValid shouldBe false
                response.discountAmount.compareTo(BigDecimal.ZERO) shouldBe 0
            }
        }
    }
})
