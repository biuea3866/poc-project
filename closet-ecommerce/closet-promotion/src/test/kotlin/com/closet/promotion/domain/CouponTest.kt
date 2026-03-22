package com.closet.promotion.domain

import com.closet.common.exception.BusinessException
import com.closet.promotion.domain.coupon.Coupon
import com.closet.promotion.domain.coupon.CouponScope
import com.closet.promotion.domain.coupon.CouponStatus
import com.closet.promotion.domain.coupon.CouponType
import com.closet.promotion.domain.coupon.MemberCouponStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

class CouponTest : BehaviorSpec({

    Given("정액 할인 쿠폰") {
        val coupon = Coupon.create(
            name = "5000원 할인 쿠폰",
            couponType = CouponType.FIXED_AMOUNT,
            discountValue = BigDecimal("5000"),
            minOrderAmount = BigDecimal("30000"),
            totalQuantity = 100,
            validFrom = LocalDateTime.now().minusDays(1),
            validTo = LocalDateTime.now().plusDays(30),
        )

        When("주문 금액이 최소 주문 금액 이상일 때 할인 계산") {
            val discount = coupon.calculateDiscount(BigDecimal("50000"))

            Then("정액 할인이 적용된다") {
                discount.compareTo(BigDecimal("5000")) shouldBe 0
            }
        }

        When("주문 금액이 최소 주문 금액 미만일 때") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    coupon.calculateDiscount(BigDecimal("20000"))
                }
            }
        }

        When("주문 금액이 할인 금액보다 작을 때") {
            val smallCoupon = Coupon.create(
                name = "100000원 할인 쿠폰",
                couponType = CouponType.FIXED_AMOUNT,
                discountValue = BigDecimal("100000"),
                minOrderAmount = BigDecimal.ZERO,
                totalQuantity = 10,
                validFrom = LocalDateTime.now().minusDays(1),
                validTo = LocalDateTime.now().plusDays(30),
            )
            val discount = smallCoupon.calculateDiscount(BigDecimal("50000"))

            Then("주문 금액까지만 할인된다") {
                discount.compareTo(BigDecimal("50000")) shouldBe 0
            }
        }
    }

    Given("정률 할인 쿠폰") {
        val coupon = Coupon.create(
            name = "10% 할인 쿠폰",
            couponType = CouponType.PERCENTAGE,
            discountValue = BigDecimal("10"),
            maxDiscountAmount = BigDecimal("10000"),
            minOrderAmount = BigDecimal("20000"),
            totalQuantity = 50,
            validFrom = LocalDateTime.now().minusDays(1),
            validTo = LocalDateTime.now().plusDays(30),
        )

        When("할인 금액이 최대 할인 금액 이하일 때") {
            val discount = coupon.calculateDiscount(BigDecimal("50000"))

            Then("정률 할인이 적용된다") {
                // 50000 * 10% = 5000
                discount.compareTo(BigDecimal("5000")) shouldBe 0
            }
        }

        When("할인 금액이 최대 할인 금액 초과일 때") {
            val discount = coupon.calculateDiscount(BigDecimal("200000"))

            Then("최대 할인 금액이 적용된다") {
                // 200000 * 10% = 20000 > maxDiscount 10000
                discount.compareTo(BigDecimal("10000")) shouldBe 0
            }
        }
    }

    Given("쿠폰 발급") {
        val coupon = Coupon.create(
            name = "테스트 쿠폰",
            couponType = CouponType.FIXED_AMOUNT,
            discountValue = BigDecimal("3000"),
            totalQuantity = 2,
            validFrom = LocalDateTime.now().minusDays(1),
            validTo = LocalDateTime.now().plusDays(30),
        )

        When("정상 발급") {
            val memberCoupon = coupon.issue(1L)

            Then("MemberCoupon이 AVAILABLE 상태로 생성된다") {
                memberCoupon.status shouldBe MemberCouponStatus.AVAILABLE
                memberCoupon.memberId shouldBe 1L
                coupon.issuedCount shouldBe 1
            }
        }

        When("수량 소진 시") {
            coupon.issue(2L) // 2번째 발급 → issuedCount = 2, totalQuantity = 2

            Then("상태가 EXHAUSTED로 변경된다") {
                coupon.status shouldBe CouponStatus.EXHAUSTED
            }

            Then("추가 발급 시 BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    coupon.issue(3L)
                }
            }
        }
    }

    Given("쿠폰 사용") {
        val coupon = Coupon.create(
            name = "사용 테스트 쿠폰",
            couponType = CouponType.FIXED_AMOUNT,
            discountValue = BigDecimal("5000"),
            totalQuantity = 10,
            validFrom = LocalDateTime.now().minusDays(1),
            validTo = LocalDateTime.now().plusDays(30),
        )
        val memberCoupon = coupon.issue(1L)

        When("정상 사용") {
            memberCoupon.use(100L)

            Then("상태가 USED로 변경된다") {
                memberCoupon.status shouldBe MemberCouponStatus.USED
                memberCoupon.usedOrderId shouldBe 100L
            }
        }

        When("이미 사용된 쿠폰 재사용 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    memberCoupon.use(200L)
                }
            }
        }
    }

    Given("CouponStatus 상태 전이") {
        When("ACTIVE에서 EXPIRED로 전이") {
            Then("전이 가능") {
                CouponStatus.ACTIVE.canTransitionTo(CouponStatus.EXPIRED) shouldBe true
            }
        }

        When("ACTIVE에서 EXHAUSTED로 전이") {
            Then("전이 가능") {
                CouponStatus.ACTIVE.canTransitionTo(CouponStatus.EXHAUSTED) shouldBe true
            }
        }

        When("EXPIRED에서 전이 시도") {
            Then("어떤 상태로도 전이 불가") {
                CouponStatus.entries.forEach { target ->
                    CouponStatus.EXPIRED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("EXHAUSTED에서 전이 시도") {
            Then("어떤 상태로도 전이 불가") {
                CouponStatus.entries.forEach { target ->
                    CouponStatus.EXHAUSTED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("터미널 상태") {
            Then("EXPIRED는 터미널") {
                CouponStatus.EXPIRED.isTerminal() shouldBe true
            }
            Then("EXHAUSTED는 터미널") {
                CouponStatus.EXHAUSTED.isTerminal() shouldBe true
            }
            Then("ACTIVE는 터미널이 아님") {
                CouponStatus.ACTIVE.isTerminal() shouldBe false
            }
        }
    }
})
