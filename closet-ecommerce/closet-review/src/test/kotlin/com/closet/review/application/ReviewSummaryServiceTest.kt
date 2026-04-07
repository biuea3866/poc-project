package com.closet.review.application

import com.closet.common.outbox.OutboxEventPublisher
import com.closet.review.domain.FitType
import com.closet.review.domain.Review
import com.closet.review.domain.ReviewSummary
import com.closet.review.domain.ReviewSummaryRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.ZonedDateTime

class ReviewSummaryServiceTest : BehaviorSpec({

    val reviewSummaryRepository = mockk<ReviewSummaryRepository>(relaxed = true)
    val outboxEventPublisher = mockk<OutboxEventPublisher>(relaxed = true)
    val objectMapper =
        ObjectMapper().apply {
            findAndRegisterModules()
        }
    val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    val valueOps = mockk<ValueOperations<String, String>>(relaxed = true)

    every { redisTemplate.opsForValue() } returns valueOps

    val reviewSummaryService =
        ReviewSummaryService(
            reviewSummaryRepository,
            outboxEventPublisher,
            objectMapper,
            redisTemplate,
        )

    fun createTestReview(
        productId: Long,
        orderItemId: Long,
        rating: Int,
        hasPhoto: Boolean = false,
        fitType: FitType? = null,
        height: Int? = null,
        weight: Int? = null,
    ): Review {
        val review =
            Review.create(
                productId = productId,
                orderItemId = orderItemId,
                memberId = 100L,
                rating = rating,
                content = "테스트 리뷰입니다. 테스트를 위한 충분한 길이의 내용을 작성합니다. 길이 제한을 충족합니다.",
                height = height,
                weight = weight,
                fitType = fitType,
            )
        if (hasPhoto) {
            review.addImage("https://example.com/img.jpg", "https://example.com/thumb.jpg", 0)
        }
        val createdAtField = Review::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(review, ZonedDateTime.now())
        val updatedAtField = Review::class.java.getDeclaredField("updatedAt")
        updatedAtField.isAccessible = true
        updatedAtField.set(review, ZonedDateTime.now())
        return review
    }

    Given("리뷰 집계 갱신 (US-804)") {

        When("첫 리뷰가 생성되면") {
            val summarySlot = slot<ReviewSummary>()
            every { reviewSummaryRepository.findByProductId(10L) } returns null
            every { reviewSummaryRepository.save(capture(summarySlot)) } answers { summarySlot.captured }

            val review = createTestReview(productId = 10L, orderItemId = 1L, rating = 5)
            reviewSummaryService.onReviewCreated(review)

            Then("집계가 생성되고 리뷰 수 1, 평균 5.0") {
                val saved = summarySlot.captured
                saved.totalCount shouldBe 1
                saved.avgRating shouldBe 5.0
                saved.rating5Count shouldBe 1
            }
        }

        When("포토 리뷰가 생성되면") {
            val summary =
                ReviewSummary.create(10L).apply {
                    totalCount = 1
                    totalRatingSum = 5
                    avgRating = 5.0
                    rating5Count = 1
                }
            every { reviewSummaryRepository.findByProductId(10L) } returns summary
            every { reviewSummaryRepository.save(any<ReviewSummary>()) } answers { firstArg() }

            val review = createTestReview(productId = 10L, orderItemId = 2L, rating = 4, hasPhoto = true)
            reviewSummaryService.onReviewCreated(review)

            Then("포토 리뷰 수가 1 증가한다") {
                summary.totalCount shouldBe 2
                summary.photoReviewCount shouldBe 1
                summary.rating4Count shouldBe 1
                summary.avgRating shouldBe 4.5
            }
        }

        When("사이즈핏 정보가 포함된 리뷰가 생성되면") {
            val summary = ReviewSummary.create(20L)
            every { reviewSummaryRepository.findByProductId(20L) } returns summary
            every { reviewSummaryRepository.save(any<ReviewSummary>()) } answers { firstArg() }

            val review =
                createTestReview(
                    productId = 20L,
                    orderItemId = 3L,
                    rating = 4,
                    fitType = FitType.PERFECT,
                    height = 175,
                    weight = 70,
                )
            reviewSummaryService.onReviewCreated(review)

            Then("사이즈핏 분포가 갱신된다") {
                summary.fitPerfectCount shouldBe 1
                summary.fitSmallCount shouldBe 0
                summary.fitLargeCount shouldBe 0
            }
        }
    }

    Given("리뷰 삭제 시 집계 갱신") {

        When("리뷰가 삭제되면") {
            val summary =
                ReviewSummary.create(30L).apply {
                    totalCount = 3
                    totalRatingSum = 12
                    avgRating = 4.0
                    rating4Count = 2
                    rating5Count = 1
                    photoReviewCount = 1
                    fitPerfectCount = 1
                }
            every { reviewSummaryRepository.findByProductId(30L) } returns summary
            every { reviewSummaryRepository.save(any<ReviewSummary>()) } answers { firstArg() }

            val review =
                createTestReview(
                    productId = 30L,
                    orderItemId = 4L,
                    rating = 4,
                    hasPhoto = true,
                    fitType = FitType.PERFECT,
                    height = 175,
                    weight = 70,
                )
            reviewSummaryService.onReviewDeleted(review)

            Then("집계가 감소한다") {
                summary.totalCount shouldBe 2
                summary.totalRatingSum shouldBe 8
                summary.avgRating shouldBe 4.0
                summary.rating4Count shouldBe 1
                summary.photoReviewCount shouldBe 0
                summary.fitPerfectCount shouldBe 0
            }
        }

        When("totalCount가 0인 상태에서 삭제하면") {
            val summary = ReviewSummary.create(40L)
            every { reviewSummaryRepository.findByProductId(40L) } returns summary
            every { reviewSummaryRepository.save(any<ReviewSummary>()) } answers { firstArg() }

            val review = createTestReview(productId = 40L, orderItemId = 5L, rating = 3)
            reviewSummaryService.onReviewDeleted(review)

            Then("totalCount는 0 이하로 내려가지 않는다") {
                summary.totalCount shouldBe 0
                summary.avgRating shouldBe 0.0
            }
        }
    }

    Given("Redis 캐시 조회 (US-804)") {

        When("캐시 히트") {
            val cachedJson =
                """{"productId":50,"totalCount":10,"avgRating":4.5,""" +
                    """"ratingDistribution":{"1":0,"2":1,"3":2,"4":3,"5":4},""" +
                    """"fitDistribution":{"SMALL":1,"PERFECT":5,"LARGE":2},""" +
                    """"photoReviewCount":3}"""
            every { valueOps.get("review_summary:50") } returns cachedJson

            val result = reviewSummaryService.getSummary(50L)

            Then("Redis 캐시에서 반환한다") {
                result shouldBe
                    ReviewSummaryResponse(
                        productId = 50L,
                        totalCount = 10,
                        avgRating = 4.5,
                        ratingDistribution = mapOf(1 to 0, 2 to 1, 3 to 2, 4 to 3, 5 to 4),
                        fitDistribution = mapOf("SMALL" to 1, "PERFECT" to 5, "LARGE" to 2),
                        photoReviewCount = 3,
                    )
            }
        }

        When("캐시 미스") {
            every { valueOps.get("review_summary:60") } returns null

            val summary =
                ReviewSummary.create(60L).apply {
                    totalCount = 5
                    totalRatingSum = 20
                    avgRating = 4.0
                    rating4Count = 3
                    rating5Count = 2
                    val createdAtField = ReviewSummary::class.java.getDeclaredField("createdAt")
                    createdAtField.isAccessible = true
                    createdAtField.set(this, ZonedDateTime.now())
                    val updatedAtField = ReviewSummary::class.java.getDeclaredField("updatedAt")
                    updatedAtField.isAccessible = true
                    updatedAtField.set(this, ZonedDateTime.now())
                }
            every { reviewSummaryRepository.findByProductId(60L) } returns summary

            val result = reviewSummaryService.getSummary(60L)

            Then("DB에서 조회 후 캐시에 저장한다") {
                result?.productId shouldBe 60L
                result?.totalCount shouldBe 5
                result?.avgRating shouldBe 4.0
                verify { valueOps.set(eq("review_summary:60"), any(), any<java.time.Duration>()) }
            }
        }

        When("존재하지 않는 상품") {
            every { valueOps.get("review_summary:999") } returns null
            every { reviewSummaryRepository.findByProductId(999L) } returns null

            val result = reviewSummaryService.getSummary(999L)

            Then("null을 반환한다") {
                result shouldBe null
            }
        }
    }

    Given("이벤트 발행") {

        When("집계 갱신 후") {
            val summary = ReviewSummary.create(70L)
            every { reviewSummaryRepository.findByProductId(70L) } returns summary
            every { reviewSummaryRepository.save(any<ReviewSummary>()) } answers { firstArg() }

            val review = createTestReview(productId = 70L, orderItemId = 6L, rating = 5)
            reviewSummaryService.onReviewCreated(review)

            Then("ReviewSummaryUpdated outbox 이벤트가 발행된다") {
                verify {
                    outboxEventPublisher.publish(
                        aggregateType = "ReviewSummary",
                        aggregateId = "70",
                        eventType = "ReviewSummaryUpdated",
                        topic = "event.closet.review",
                        partitionKey = "70",
                        payload = any(),
                    )
                }
            }
        }
    }

    Given("ReviewSummary 엔티티 단위 테스트") {

        When("여러 별점의 리뷰가 추가되면") {
            val summary = ReviewSummary.create(100L)
            summary.addReview(5, null, false)
            summary.addReview(4, null, false)
            summary.addReview(3, FitType.SMALL, true)

            Then("별점 분포가 올바르게 계산된다") {
                summary.totalCount shouldBe 3
                summary.totalRatingSum shouldBe 12
                summary.avgRating shouldBe 4.0
                summary.rating5Count shouldBe 1
                summary.rating4Count shouldBe 1
                summary.rating3Count shouldBe 1
                summary.fitSmallCount shouldBe 1
                summary.photoReviewCount shouldBe 1
            }
        }

        When("모든 리뷰를 삭제하면") {
            val summary = ReviewSummary.create(101L)
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

        When("3가지 핏 타입이 모두 기록되면") {
            val summary = ReviewSummary.create(102L)
            summary.addReview(4, FitType.SMALL, false)
            summary.addReview(4, FitType.SMALL, false)
            summary.addReview(5, FitType.PERFECT, false)
            summary.addReview(5, FitType.PERFECT, false)
            summary.addReview(5, FitType.PERFECT, false)
            summary.addReview(3, FitType.LARGE, false)

            Then("사이즈핏 분포가 올바르다") {
                summary.fitSmallCount shouldBe 2
                summary.fitPerfectCount shouldBe 3
                summary.fitLargeCount shouldBe 1
                summary.totalCount shouldBe 6
                summary.avgRating shouldBeGreaterThan 4.0
            }
        }
    }
})
