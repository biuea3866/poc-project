package com.closet.promotion.domain

import com.closet.promotion.domain.point.GradeType
import com.closet.promotion.domain.point.PointEventType
import com.closet.promotion.domain.point.PointPolicy
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointPolicyTest : BehaviorSpec({

    Given("정률 적립 정책") {
        val policy =
            PointPolicy(
                eventType = PointEventType.PURCHASE,
                gradeType = GradeType.ALL,
                pointRate = BigDecimal("3"),
                description = "구매 시 3% 적립",
            )

        When("주문 금액으로 적립금 계산") {
            val point = policy.calculatePoint(BigDecimal("100000"), GradeType.NORMAL)

            Then("정률 적립금이 계산된다") {
                point shouldBe 3000
            }
        }
    }

    Given("정액 적립 정책") {
        val policy =
            PointPolicy(
                eventType = PointEventType.REVIEW,
                gradeType = GradeType.ALL,
                pointAmount = 500,
                description = "리뷰 작성 시 500원 적립",
            )

        When("적립금 계산") {
            val point = policy.calculatePoint(BigDecimal("50000"), GradeType.GOLD)

            Then("정액 적립금이 반환된다") {
                point shouldBe 500
            }
        }
    }

    Given("특정 등급 전용 정책") {
        val policy =
            PointPolicy(
                eventType = PointEventType.PURCHASE,
                gradeType = GradeType.PLATINUM,
                pointRate = BigDecimal("5"),
                description = "플래티넘 등급 5% 적립",
            )

        When("해당 등급 회원의 적립금 계산") {
            val point = policy.calculatePoint(BigDecimal("100000"), GradeType.PLATINUM)

            Then("정률 적립금이 계산된다") {
                point shouldBe 5000
            }
        }

        When("다른 등급 회원의 적립금 계산") {
            val point = policy.calculatePoint(BigDecimal("100000"), GradeType.NORMAL)

            Then("적립금이 0이다") {
                point shouldBe 0
            }
        }
    }

    Given("비활성 정책") {
        val policy =
            PointPolicy(
                eventType = PointEventType.PURCHASE,
                gradeType = GradeType.ALL,
                pointRate = BigDecimal("3"),
                description = "비활성 정책",
                isActive = false,
            )

        When("적립금 계산") {
            val point = policy.calculatePoint(BigDecimal("100000"), GradeType.NORMAL)

            Then("적립금이 0이다") {
                point shouldBe 0
            }
        }
    }
})
