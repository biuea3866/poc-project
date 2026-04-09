package com.closet.review.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * SizeFit enum 검증 테스트.
 *
 * FitType -> SizeFit 리네이밍 후, enum 값과 DB 저장 문자열이
 * 기존과 동일(SMALL/PERFECT/LARGE)함을 보장한다.
 */
class SizeFitTest : BehaviorSpec({

    Given("SizeFit enum (FitType에서 리네이밍)") {

        When("enum 값이 3개 존재하면") {
            Then("SMALL, PERFECT, LARGE이다") {
                SizeFit.values().size shouldBe 3
                SizeFit.valueOf("SMALL") shouldBe SizeFit.SMALL
                SizeFit.valueOf("PERFECT") shouldBe SizeFit.PERFECT
                SizeFit.valueOf("LARGE") shouldBe SizeFit.LARGE
            }
        }

        When("name()을 호출하면") {
            Then("DB에 저장되는 문자열과 동일하다 (VARCHAR 호환)") {
                SizeFit.SMALL.name shouldBe "SMALL"
                SizeFit.PERFECT.name shouldBe "PERFECT"
                SizeFit.LARGE.name shouldBe "LARGE"
            }
        }

        When("Review 엔티티에서 SizeFit을 사용하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "SizeFit 리네이밍 검증 테스트입니다. 기존 FitType과 동일하게 동작해야 합니다.",
                    height = 175,
                    weight = 70,
                    fitType = SizeFit.PERFECT,
                )

            Then("fitType 필드에 SizeFit 값이 할당된다") {
                review.fitType shouldBe SizeFit.PERFECT
                review.hasSizeInfo() shouldBe true
            }
        }

        When("ReviewSummary에서 SizeFit을 사용하면") {
            val summary = ReviewSummary.create(1L)
            summary.addReview(5, SizeFit.SMALL, false)
            summary.addReview(4, SizeFit.PERFECT, false)
            summary.addReview(3, SizeFit.LARGE, false)

            Then("핏 분포가 올바르게 계산된다") {
                summary.fitSmallCount shouldBe 1
                summary.fitPerfectCount shouldBe 1
                summary.fitLargeCount shouldBe 1
            }
        }
    }
})
