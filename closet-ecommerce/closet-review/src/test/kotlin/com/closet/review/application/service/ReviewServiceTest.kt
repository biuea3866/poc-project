package com.closet.review.application.service

import com.closet.common.exception.BusinessException
import com.closet.review.application.dto.CreateReviewRequest
import com.closet.review.application.dto.UpdateReviewRequest
import com.closet.review.domain.entity.Review
import com.closet.review.domain.enums.ReviewStatus
import com.closet.review.domain.enums.SizeFeeling
import com.closet.review.domain.repository.ReviewRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Optional

class ReviewServiceTest : BehaviorSpec({

    val reviewRepository = mockk<ReviewRepository>()
    val reviewService = ReviewService(reviewRepository)

    Given("리뷰 생성 요청이 주어졌을 때") {
        val request = CreateReviewRequest(
            productId = 1L,
            orderItemId = 100L,
            rating = 5,
            content = "핏이 너무 좋아요! 사이즈도 딱 맞습니다.",
            height = 175,
            weight = 70,
            sizeFeeling = SizeFeeling.JUST_RIGHT,
            imageUrls = listOf("https://s3.example.com/review/img1.jpg")
        )

        every { reviewRepository.existsByOrderItemId(100L) } returns false

        val reviewSlot = slot<Review>()
        every { reviewRepository.save(capture(reviewSlot)) } answers {
            reviewSlot.captured
        }

        When("createReview를 호출하면") {
            val response = reviewService.createReview(memberId = 10L, request = request)

            Then("리뷰가 ACTIVE 상태로 생성된다") {
                response.productId shouldBe 1L
                response.memberId shouldBe 10L
                response.rating shouldBe 5
                response.content shouldBe "핏이 너무 좋아요! 사이즈도 딱 맞습니다."
                response.height shouldBe 175
                response.weight shouldBe 70
                response.sizeFeeling shouldBe SizeFeeling.JUST_RIGHT
                response.images.size shouldBe 1
            }
        }
    }

    Given("이미 해당 주문 상품에 리뷰가 존재할 때") {
        val request = CreateReviewRequest(
            productId = 1L,
            orderItemId = 100L,
            rating = 4,
            content = "좋은 상품입니다"
        )

        every { reviewRepository.existsByOrderItemId(100L) } returns true

        When("createReview를 호출하면") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    reviewService.createReview(memberId = 10L, request = request)
                }
            }
        }
    }

    Given("별점이 범위를 벗어난 리뷰 생성 요청이 주어졌을 때") {
        When("별점 0으로 리뷰를 생성하면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 1L,
                        orderItemId = 200L,
                        memberId = 10L,
                        rating = 0,
                        content = "최악"
                    )
                }
            }
        }

        When("별점 6으로 리뷰를 생성하면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 1L,
                        orderItemId = 201L,
                        memberId = 10L,
                        rating = 6,
                        content = "최고 이상"
                    )
                }
            }
        }
    }

    Given("활성 상태의 리뷰가 존재할 때") {
        val review = Review.create(
            productId = 1L,
            orderItemId = 300L,
            memberId = 10L,
            rating = 3,
            content = "보통이에요",
            height = 168,
            weight = 60,
            sizeFeeling = SizeFeeling.LARGE
        )

        every { reviewRepository.findById(1L) } returns Optional.of(review)

        When("본인이 리뷰를 수정하면") {
            val updateRequest = UpdateReviewRequest(rating = 4, content = "다시 입어보니 괜찮아요")
            val response = reviewService.updateReview(reviewId = 1L, memberId = 10L, request = updateRequest)

            Then("리뷰가 수정된다") {
                response.rating shouldBe 4
                response.content shouldBe "다시 입어보니 괜찮아요"
            }
        }
    }

    Given("다른 사용자의 리뷰를 수정하려 할 때") {
        val review = Review.create(
            productId = 1L,
            orderItemId = 400L,
            memberId = 10L,
            rating = 5,
            content = "좋아요"
        )

        every { reviewRepository.findById(2L) } returns Optional.of(review)

        When("다른 memberId로 수정을 시도하면") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    reviewService.updateReview(
                        reviewId = 2L,
                        memberId = 999L,
                        request = UpdateReviewRequest(rating = 1, content = "수정 시도")
                    )
                }
            }
        }
    }

    Given("리뷰 요약 조회 시") {
        every { reviewRepository.findAverageRatingByProductId(1L) } returns 4.2
        every { reviewRepository.countByProductIdAndStatus(1L) } returns 50L
        every { reviewRepository.findRatingDistribution(1L) } returns listOf(
            arrayOf(5, 20L),
            arrayOf(4, 15L),
            arrayOf(3, 10L),
            arrayOf(2, 3L),
            arrayOf(1, 2L)
        )
        every { reviewRepository.findSizeFeelingDistribution(1L) } returns listOf(
            arrayOf(SizeFeeling.SMALL, 10L),
            arrayOf(SizeFeeling.JUST_RIGHT, 30L),
            arrayOf(SizeFeeling.LARGE, 10L)
        )

        When("getReviewSummary를 호출하면") {
            val summary = reviewService.getReviewSummary(1L)

            Then("평균 별점, 총 개수, 별점 분포, 사이즈 체감 분포가 반환된다") {
                summary.averageRating shouldBe 4.2
                summary.totalCount shouldBe 50L
                summary.ratingDistribution[5] shouldBe 20L
                summary.ratingDistribution[4] shouldBe 15L
                summary.sizeFeelingDistribution[SizeFeeling.JUST_RIGHT] shouldBe 30L
            }
        }
    }

    Given("리뷰에 도움돼요를 누를 때") {
        val review = Review.create(
            productId = 1L,
            orderItemId = 500L,
            memberId = 10L,
            rating = 4,
            content = "유용한 리뷰"
        )

        every { reviewRepository.findById(3L) } returns Optional.of(review)

        When("markHelpful을 호출하면") {
            reviewService.markHelpful(3L)

            Then("helpfulCount가 증가한다") {
                review.helpfulCount shouldBe 1
            }
        }
    }

    Given("존재하지 않는 리뷰를 조회할 때") {
        every { reviewRepository.findById(999L) } returns Optional.empty()

        When("updateReview를 호출하면") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    reviewService.updateReview(
                        reviewId = 999L,
                        memberId = 10L,
                        request = UpdateReviewRequest(rating = 3, content = "수정")
                    )
                }
            }
        }
    }
})
