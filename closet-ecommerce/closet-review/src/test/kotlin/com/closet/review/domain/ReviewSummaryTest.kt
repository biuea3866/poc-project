package com.closet.review.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * ReviewSummary 엔티티 단위 테스트 (US-804).
 *
 * 리뷰 추가/삭제 시 집계(별점 분포, 핏 분포, 포토 리뷰 수)가 올바르게 갱신되는지 검증한다.
 */
class ReviewSummaryTest : BehaviorSpec({

    Given("리뷰 추가 시 집계 갱신") {

        When("첫 리뷰(5점, 포토, PERFECT)를 추가하면") {
            val summary = ReviewSummary.create(1L)
            summary.addReview(5, FitType.PERFECT, true)

            Then("totalCount=1, avgRating=5.0, rating5Count=1, fitPerfectCount=1, photoReviewCount=1") {
                summary.totalCount shouldBe 1
                summary.avgRating shouldBe 5.0
                summary.totalRatingSum shouldBe 5
                summary.rating5Count shouldBe 1
                summary.fitPerfectCount shouldBe 1
                summary.photoReviewCount shouldBe 1
            }
        }

        When("다양한 별점의 리뷰를 추가하면") {
            val summary = ReviewSummary.create(2L)
            summary.addReview(5, null, false)
            summary.addReview(4, null, false)
            summary.addReview(3, FitType.SMALL, true)
            summary.addReview(2, FitType.LARGE, false)
            summary.addReview(1, null, false)

            Then("별점 분포가 올바르게 계산된다") {
                summary.totalCount shouldBe 5
                summary.totalRatingSum shouldBe 15
                summary.avgRating shouldBe 3.0
                summary.rating1Count shouldBe 1
                summary.rating2Count shouldBe 1
                summary.rating3Count shouldBe 1
                summary.rating4Count shouldBe 1
                summary.rating5Count shouldBe 1
                summary.fitSmallCount shouldBe 1
                summary.fitLargeCount shouldBe 1
                summary.photoReviewCount shouldBe 1
            }
        }

        When("핏 타입이 null인 리뷰를 추가하면") {
            val summary = ReviewSummary.create(3L)
            summary.addReview(4, null, false)

            Then("핏 카운트는 변하지 않는다") {
                summary.fitSmallCount shouldBe 0
                summary.fitPerfectCount shouldBe 0
                summary.fitLargeCount shouldBe 0
            }
        }
    }

    Given("리뷰 삭제 시 집계 갱신") {

        When("리뷰를 삭제하면") {
            val summary =
                ReviewSummary.create(10L).apply {
                    addReview(5, FitType.PERFECT, true)
                    addReview(4, FitType.SMALL, false)
                    addReview(3, null, true)
                }
            summary.removeReview(5, FitType.PERFECT, true)

            Then("해당 리뷰의 정보가 차감된다") {
                summary.totalCount shouldBe 2
                summary.totalRatingSum shouldBe 7
                summary.avgRating shouldBe 3.5
                summary.rating5Count shouldBe 0
                summary.fitPerfectCount shouldBe 0
                summary.photoReviewCount shouldBe 1
            }
        }

        When("모든 리뷰를 삭제하면") {
            val summary = ReviewSummary.create(11L)
            summary.addReview(5, FitType.PERFECT, true)
            summary.removeReview(5, FitType.PERFECT, true)

            Then("모든 카운트가 0이 된다") {
                summary.totalCount shouldBe 0
                summary.avgRating shouldBe 0.0
                summary.totalRatingSum shouldBe 0
                summary.rating5Count shouldBe 0
                summary.fitPerfectCount shouldBe 0
                summary.photoReviewCount shouldBe 0
            }
        }

        When("totalCount가 0인 상태에서 삭제하면") {
            val summary = ReviewSummary.create(12L)
            summary.removeReview(3, null, false)

            Then("카운트가 음수로 내려가지 않는다") {
                summary.totalCount shouldBe 0
                summary.avgRating shouldBe 0.0
                summary.rating3Count shouldBe 0
            }
        }
    }

    Given("사이즈핏 분포 (US-802)") {

        When("세 가지 핏 타입이 모두 기록되면") {
            val summary = ReviewSummary.create(20L)
            summary.addReview(4, FitType.SMALL, false)
            summary.addReview(4, FitType.SMALL, false)
            summary.addReview(5, FitType.PERFECT, false)
            summary.addReview(5, FitType.PERFECT, false)
            summary.addReview(5, FitType.PERFECT, false)
            summary.addReview(3, FitType.LARGE, false)

            Then("각 핏 분포가 올바르다") {
                summary.fitSmallCount shouldBe 2
                summary.fitPerfectCount shouldBe 3
                summary.fitLargeCount shouldBe 1
                summary.totalCount shouldBe 6
                summary.avgRating shouldBeGreaterThan 4.0
            }
        }

        When("핏 타입 삭제 시 coerceAtLeast(0)") {
            val summary = ReviewSummary.create(21L)
            summary.removeReview(4, FitType.SMALL, false)

            Then("fitSmallCount가 0 미만으로 내려가지 않는다") {
                summary.fitSmallCount shouldBe 0
            }
        }
    }
})
