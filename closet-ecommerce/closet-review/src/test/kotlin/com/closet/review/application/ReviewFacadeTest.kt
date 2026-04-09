package com.closet.review.application

import com.closet.review.application.facade.ReviewFacade
import com.closet.review.domain.ReviewSortType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import java.time.ZonedDateTime

/**
 * ReviewFacade 테스트.
 *
 * Facade는 Controller -> Facade -> Service의 중간 레이어로,
 * Service 호출을 오케스트레이션한다.
 */
class ReviewFacadeTest : BehaviorSpec({

    val reviewService = mockk<ReviewService>(relaxed = true)
    val reviewSummaryService = mockk<ReviewSummaryService>(relaxed = true)
    val reviewImageService = mockk<ReviewImageService>(relaxed = true)

    val facade =
        ReviewFacade(
            reviewService = reviewService,
            reviewSummaryService = reviewSummaryService,
            reviewImageService = reviewImageService,
        )

    Given("리뷰 작성 Facade (US-801)") {

        When("유효한 요청으로 리뷰를 작성하면") {
            val request =
                CreateReviewRequest(
                    productId = 10L,
                    orderItemId = 1L,
                    rating = 5,
                    content = "퍼사드를 통한 리뷰 작성 테스트입니다. 서비스에 위임되어야 합니다. 충분한 길이.",
                )
            val expectedResponse =
                ReviewResponse(
                    id = 1L, productId = 10L, orderItemId = 1L, memberId = 100L,
                    rating = 5,
                    content = "퍼사드를 통한 리뷰 작성 테스트입니다. 서비스에 위임되어야 합니다. 충분한 길이.",
                    status = "VISIBLE", editCount = 0, hasImage = false,
                    images = emptyList(),
                    height = null, weight = null, normalSize = null, purchasedSize = null,
                    fitType = null, helpfulCount = 0,
                    createdAt = ZonedDateTime.now(), updatedAt = ZonedDateTime.now(),
                )
            every { reviewService.createReview(100L, request) } returns expectedResponse

            val result = facade.createReview(100L, request)

            Then("ReviewService.createReview에 위임된다") {
                result.id shouldBe 1L
                result.productId shouldBe 10L
                verify { reviewService.createReview(100L, request) }
            }
        }
    }

    Given("리뷰 수정 Facade (US-801)") {

        When("유효한 수정 요청이면") {
            val request =
                UpdateReviewRequest(
                    content = "퍼사드를 통한 리뷰 수정 테스트입니다. 서비스에 위임됩니다. 충분한 길이의 내용.",
                )
            val expectedResponse =
                ReviewResponse(
                    id = 1L, productId = 10L, orderItemId = 1L, memberId = 100L,
                    rating = 5,
                    content = "퍼사드를 통한 리뷰 수정 테스트입니다. 서비스에 위임됩니다. 충분한 길이의 내용.",
                    status = "VISIBLE", editCount = 1, hasImage = false,
                    images = emptyList(),
                    height = null, weight = null, normalSize = null, purchasedSize = null,
                    fitType = null, helpfulCount = 0,
                    createdAt = ZonedDateTime.now(), updatedAt = ZonedDateTime.now(),
                )
            every { reviewService.updateReview(100L, 1L, request) } returns expectedResponse

            val result = facade.updateReview(100L, 1L, request)

            Then("ReviewService.updateReview에 위임된다") {
                result.editCount shouldBe 1
                verify { reviewService.updateReview(100L, 1L, request) }
            }
        }
    }

    Given("리뷰 삭제 Facade (US-801)") {

        When("본인 리뷰를 삭제하면") {
            facade.deleteReview(100L, 1L)

            Then("ReviewService.deleteReview에 위임된다") {
                verify { reviewService.deleteReview(100L, 1L) }
            }
        }
    }

    Given("리뷰 목록 조회 Facade") {

        When("상품별 리뷰 목록을 조회하면") {
            val query = ReviewListQuery(productId = 10L, sort = ReviewSortType.LATEST)
            every { reviewService.findByProductId(query) } returns PageImpl(emptyList())

            val result = facade.getReviews(query)

            Then("ReviewService.findByProductId에 위임된다") {
                result.totalElements shouldBe 0
                verify { reviewService.findByProductId(query) }
            }
        }

        When("포토 리뷰만 조회하면") {
            val query = ReviewListQuery(productId = 10L, photoOnly = true)
            every { reviewService.findByProductId(query) } returns PageImpl(emptyList())

            facade.getReviews(query)

            Then("photoOnly 파라미터가 전달된다") {
                verify { reviewService.findByProductId(match { it.photoOnly }) }
            }
        }

        When("비슷한 체형 필터로 조회하면") {
            val query = ReviewListQuery(productId = 10L, myHeight = 175, myWeight = 70)
            every { reviewService.findByProductId(query) } returns PageImpl(emptyList())

            facade.getReviews(query)

            Then("myHeight, myWeight가 전달된다") {
                verify { reviewService.findByProductId(match { it.myHeight == 175 && it.myWeight == 70 }) }
            }
        }
    }

    Given("리뷰 집계 조회 Facade (US-804)") {

        When("상품별 리뷰 집계를 조회하면") {
            val expectedSummary =
                ReviewSummaryResponse(
                    productId = 10L,
                    totalCount = 50,
                    avgRating = 4.3,
                    ratingDistribution = mapOf(1 to 2, 2 to 3, 3 to 5, 4 to 20, 5 to 20),
                    fitDistribution = mapOf("SMALL" to 5, "PERFECT" to 30, "LARGE" to 10),
                    photoReviewCount = 15,
                )
            every { reviewSummaryService.getSummary(10L) } returns expectedSummary

            val result = facade.getSummary(10L)

            Then("ReviewSummaryService.getSummary에 위임된다") {
                result shouldNotBe null
                result?.totalCount shouldBe 50
                result?.avgRating shouldBe 4.3
                verify { reviewSummaryService.getSummary(10L) }
            }
        }
    }

    Given("도움이 됐어요 Facade") {

        When("markHelpful을 호출하면") {
            facade.markHelpful(200L, 1L)

            Then("ReviewService.markHelpful에 위임된다") {
                verify { reviewService.markHelpful(200L, 1L) }
            }
        }
    }

    Given("Presigned URL 발급 Facade") {

        When("이미지 업로드 URL을 요청하면") {
            val request = PresignedUploadUrlRequest(fileName = "photo.jpg", contentType = "image/jpeg")
            val expectedResponse =
                PresignedUploadUrlResponse(
                    uploadUrl = "https://minio/upload?signed",
                    imageUrl = "https://minio/reviews/100/uuid.jpg",
                    key = "reviews/100/uuid.jpg",
                )
            every { reviewImageService.generatePresignedUploadUrl(100L, request) } returns expectedResponse

            val result = facade.getPresignedUploadUrl(100L, request)

            Then("ReviewImageService에 위임된다") {
                result.uploadUrl shouldBe "https://minio/upload?signed"
                verify { reviewImageService.generatePresignedUploadUrl(100L, request) }
            }
        }
    }
})
