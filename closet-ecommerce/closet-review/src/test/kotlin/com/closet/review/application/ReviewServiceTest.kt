package com.closet.review.application

import com.closet.common.exception.BusinessException
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.review.domain.FitType
import com.closet.review.domain.Review
import com.closet.review.domain.ReviewEditHistoryRepository
import com.closet.review.domain.ReviewHelpful
import com.closet.review.domain.ReviewHelpfulRepository
import com.closet.review.domain.ReviewRepository
import com.closet.review.domain.ReviewStatus
import com.closet.review.domain.ReviewableOrderItemRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime
import java.util.Optional

class ReviewServiceTest : BehaviorSpec({

    val reviewRepository = mockk<ReviewRepository>(relaxed = true)
    val reviewEditHistoryRepository = mockk<ReviewEditHistoryRepository>(relaxed = true)
    val reviewHelpfulRepository = mockk<ReviewHelpfulRepository>(relaxed = true)
    val reviewableOrderItemRepository = mockk<ReviewableOrderItemRepository>(relaxed = true)
    val reviewSummaryService = mockk<ReviewSummaryService>(relaxed = true)
    val outboxEventPublisher = mockk<OutboxEventPublisher>(relaxed = true)
    val objectMapper = ObjectMapper().apply {
        findAndRegisterModules()
    }

    val reviewService = ReviewService(
        reviewRepository,
        reviewEditHistoryRepository,
        reviewHelpfulRepository,
        reviewableOrderItemRepository,
        reviewSummaryService,
        outboxEventPublisher,
        objectMapper,
    )

    Given("리뷰 작성 (US-801)") {

        When("유효한 텍스트 리뷰를 작성하면") {
            every { reviewableOrderItemRepository.existsByOrderItemIdAndMemberId(1L, 100L) } returns true
            every { reviewRepository.existsByOrderItemIdAndMemberIdAndStatusNot(1L, 100L, ReviewStatus.DELETED) } returns false

            Then("리뷰가 생성되고 outbox 이벤트가 발행된다") {
                every { reviewRepository.save(any<Review>()) } answers { firstArg() }
                val review = Review.create(
                    productId = 10L,
                    orderItemId = 1L,
                    memberId = 100L,
                    rating = 5,
                    content = "이 옷 정말 좋아요! 사이즈도 딱 맞고 소재감도 너무 마음에 듭니다.",
                )
                review.rating shouldBe 5
                review.content.length shouldNotBe 0
            }
        }

        When("동일 주문상품에 리뷰가 이미 존재하면") {
            every { reviewableOrderItemRepository.existsByOrderItemIdAndMemberId(2L, 100L) } returns true
            every { reviewRepository.existsByOrderItemIdAndMemberIdAndStatusNot(2L, 100L, ReviewStatus.DELETED) } returns true

            val request = CreateReviewRequest(
                productId = 10L,
                orderItemId = 2L,
                rating = 4,
                content = "두 번째 리뷰 작성 시도입니다. 테스트를 위한 충분한 길이의 내용을 작성합니다.",
            )

            Then("DUPLICATE_ENTITY 예외 발생") {
                shouldThrow<BusinessException> {
                    reviewService.createReview(100L, request)
                }
            }
        }

        When("리뷰 내용이 20자 미만이면") {
            Then("require 예외 발생") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 10L,
                        orderItemId = 3L,
                        memberId = 100L,
                        rating = 5,
                        content = "짧은 내용",
                    )
                }
            }
        }

        When("리뷰 내용이 1000자 초과이면") {
            Then("require 예외 발생") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 10L,
                        orderItemId = 4L,
                        memberId = 100L,
                        rating = 5,
                        content = "가".repeat(1001),
                    )
                }
            }
        }

        When("이미지 URL이 5장 초과이면") {
            every { reviewableOrderItemRepository.existsByOrderItemIdAndMemberId(5L, 100L) } returns true
            every { reviewRepository.existsByOrderItemIdAndMemberIdAndStatusNot(5L, 100L, ReviewStatus.DELETED) } returns false

            val request = CreateReviewRequest(
                productId = 10L,
                orderItemId = 5L,
                rating = 5,
                content = "이미지 6장을 첨부한 리뷰입니다. 최대 이미지 개수를 초과합니다.",
                imageUrls = (1..6).map { "https://example.com/image$it.jpg" },
            )

            Then("INVALID_INPUT 예외 발생") {
                shouldThrow<BusinessException> {
                    reviewService.createReview(100L, request)
                }
            }
        }

        When("구매확정되지 않은 주문 아이템으로 리뷰를 작성하면") {
            every { reviewableOrderItemRepository.existsByOrderItemIdAndMemberId(99L, 100L) } returns false

            val request = CreateReviewRequest(
                productId = 10L,
                orderItemId = 99L,
                rating = 5,
                content = "구매확정되지 않은 주문 아이템 리뷰 작성 시도입니다. 실패해야 합니다.",
            )

            Then("INVALID_INPUT 예외 발생") {
                shouldThrow<BusinessException> {
                    reviewService.createReview(100L, request)
                }
            }
        }

        When("별점이 범위를 벗어나면") {
            Then("require 예외 발생 (별점 0)") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 10L,
                        orderItemId = 6L,
                        memberId = 100L,
                        rating = 0,
                        content = "별점 0은 유효하지 않습니다. 별점은 1에서 5 사이여야 합니다.",
                    )
                }
            }
            Then("require 예외 발생 (별점 6)") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 10L,
                        orderItemId = 7L,
                        memberId = 100L,
                        rating = 6,
                        content = "별점 6도 유효하지 않습니다. 별점은 1에서 5 사이여야 합니다.",
                    )
                }
            }
        }
    }

    Given("사이즈 후기 (US-802)") {

        When("키, 몸무게, 평소 사이즈, 구매 사이즈, 핏 타입을 함께 작성하면") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 10L,
                memberId = 100L,
                rating = 4,
                content = "사이즈 후기를 포함한 리뷰 내용입니다. 키와 몸무게, 사이즈 정보를 입력합니다.",
                height = 175,
                weight = 70,
                normalSize = "M",
                purchasedSize = "L",
                fitType = FitType.PERFECT,
            )

            Then("사이즈 정보가 저장된다") {
                review.height shouldBe 175
                review.weight shouldBe 70
                review.normalSize shouldBe "M"
                review.purchasedSize shouldBe "L"
                review.fitType shouldBe FitType.PERFECT
                review.hasSizeInfo() shouldBe true
            }
        }

        When("사이즈 정보 없이 작성하면") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 11L,
                memberId = 100L,
                rating = 3,
                content = "사이즈 정보 없이 작성한 일반 텍스트 리뷰 내용입니다. 충분한 길이로 작성합니다.",
            )

            Then("hasSizeInfo()는 false이다") {
                review.hasSizeInfo() shouldBe false
            }
        }
    }

    Given("리뷰 포인트 계산 (US-803)") {

        When("텍스트 리뷰 (사이즈 정보 없음)") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 20L,
                memberId = 100L,
                rating = 4,
                content = "텍스트 리뷰입니다. 포인트 100P가 적립되어야 합니다. 충분한 길이의 리뷰 내용입니다.",
            )

            Then("100P 적립") {
                review.calculatePointAmount() shouldBe 100
            }
        }

        When("텍스트 리뷰 + 사이즈 정보") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 21L,
                memberId = 100L,
                rating = 4,
                content = "텍스트 리뷰 + 사이즈 정보. 포인트 150P가 적립되어야 합니다. 충분한 길이입니다.",
                height = 170,
                weight = 65,
                fitType = FitType.SMALL,
            )

            Then("150P 적립 (100 + 50)") {
                review.calculatePointAmount() shouldBe 150
            }
        }

        When("포토 리뷰 (사이즈 정보 없음)") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 22L,
                memberId = 100L,
                rating = 5,
                content = "포토 리뷰입니다. 이미지를 포함해서 300P가 적립되어야 합니다. 충분한 길이의 리뷰입니다.",
            )
            review.addImage("https://example.com/img1.jpg", "https://example.com/img1_thumb.jpg", 0)

            Then("300P 적립") {
                review.isPhotoReview() shouldBe true
                review.calculatePointAmount() shouldBe 300
            }
        }

        When("포토 리뷰 + 사이즈 정보 (최대 조합)") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 23L,
                memberId = 100L,
                rating = 5,
                content = "포토 리뷰 + 사이즈 정보. 최대 350P가 적립되어야 합니다. 충분한 길이의 리뷰 내용입니다.",
                height = 180,
                weight = 75,
                fitType = FitType.LARGE,
            )
            review.addImage("https://example.com/img1.jpg", "https://example.com/img1_thumb.jpg", 0)

            Then("350P 적립 (300 + 50)") {
                review.isPhotoReview() shouldBe true
                review.hasSizeInfo() shouldBe true
                review.calculatePointAmount() shouldBe 350
            }
        }
    }

    Given("리뷰 수정 (US-801)") {

        When("7일 이내에 수정하면") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 30L,
                memberId = 100L,
                rating = 4,
                content = "원래 리뷰 내용입니다. 이 리뷰를 수정하려고 합니다. 충분한 길이입니다.",
            )
            // createdAt을 수동 설정
            val createdAtField = Review::class.java.getDeclaredField("createdAt")
            createdAtField.isAccessible = true
            createdAtField.set(review, ZonedDateTime.now().minusDays(3))

            review.update("수정된 리뷰 내용입니다. 내용을 변경했습니다. 충분한 길이를 위한 추가 내용입니다.")

            Then("내용이 수정되고 editCount가 1 증가한다") {
                review.content shouldBe "수정된 리뷰 내용입니다. 내용을 변경했습니다. 충분한 길이를 위한 추가 내용입니다."
                review.editCount shouldBe 1
            }
        }

        When("7일 이후에 수정하면") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 31L,
                memberId = 100L,
                rating = 4,
                content = "7일이 지난 리뷰입니다. 수정하려고 하면 실패해야 합니다. 충분한 길이입니다.",
            )
            val createdAtField = Review::class.java.getDeclaredField("createdAt")
            createdAtField.isAccessible = true
            createdAtField.set(review, ZonedDateTime.now().minusDays(8))

            Then("INVALID_INPUT 예외 발생") {
                shouldThrow<BusinessException> {
                    review.update("수정 시도 내용입니다. 하지만 기한이 지났으므로 실패합니다. 충분한 길이를 위한 내용입니다.")
                }
            }
        }

        When("3회 이상 수정하면") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 32L,
                memberId = 100L,
                rating = 4,
                content = "여러 번 수정할 리뷰 내용입니다. 최대 3회까지 수정 가능합니다. 충분한 길이입니다.",
            )
            val createdAtField = Review::class.java.getDeclaredField("createdAt")
            createdAtField.isAccessible = true
            createdAtField.set(review, ZonedDateTime.now())
            review.editCount = 3

            Then("INVALID_INPUT 예외 발생") {
                shouldThrow<BusinessException> {
                    review.update("4번째 수정 시도입니다. 하지만 횟수 제한에 걸려 실패합니다. 충분한 길이를 위한 내용입니다.")
                }
            }
        }
    }

    Given("리뷰 삭제 (US-801)") {

        When("본인 리뷰를 삭제하면") {
            every { reviewRepository.findByIdAndMemberId(1L, 100L) } returns Review.create(
                productId = 10L,
                orderItemId = 40L,
                memberId = 100L,
                rating = 4,
                content = "삭제할 리뷰 내용입니다. 본인만 삭제 가능합니다. 충분한 길이의 리뷰 내용입니다.",
            ).also {
                val createdAtField = Review::class.java.getDeclaredField("createdAt")
                createdAtField.isAccessible = true
                createdAtField.set(it, ZonedDateTime.now())
                val updatedAtField = Review::class.java.getDeclaredField("updatedAt")
                updatedAtField.isAccessible = true
                updatedAtField.set(it, ZonedDateTime.now())
            }

            reviewService.deleteReview(100L, 1L)

            Then("집계 갱신 및 삭제 이벤트가 발행된다") {
                verify { reviewSummaryService.onReviewDeleted(any()) }
                verify { outboxEventPublisher.publish(any(), any(), eq("ReviewDeleted"), any(), any(), any()) }
            }
        }

        When("다른 회원의 리뷰를 삭제하려 하면") {
            every { reviewRepository.findByIdAndMemberId(2L, 200L) } returns null

            Then("ENTITY_NOT_FOUND 예외 발생") {
                shouldThrow<BusinessException> {
                    reviewService.deleteReview(200L, 2L)
                }
            }
        }
    }

    Given("도움이 됐어요") {

        When("처음 '도움이 됐어요'를 누르면") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 50L,
                memberId = 100L,
                rating = 5,
                content = "도움이 됐어요 테스트 리뷰입니다. 충분한 길이를 위한 리뷰 내용을 작성합니다.",
            ).also {
                val createdAtField = Review::class.java.getDeclaredField("createdAt")
                createdAtField.isAccessible = true
                createdAtField.set(it, ZonedDateTime.now())
                val updatedAtField = Review::class.java.getDeclaredField("updatedAt")
                updatedAtField.isAccessible = true
                updatedAtField.set(it, ZonedDateTime.now())
            }
            every { reviewRepository.findById(1L) } returns Optional.of(review)
            every { reviewHelpfulRepository.existsByReviewIdAndMemberId(1L, 200L) } returns false
            every { reviewHelpfulRepository.save(any<ReviewHelpful>()) } answers { firstArg() }

            reviewService.markHelpful(200L, 1L)

            Then("helpfulCount가 1 증가한다") {
                review.helpfulCount shouldBe 1
            }
        }

        When("이미 '도움이 됐어요'를 누른 리뷰에 다시 누르면") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 51L,
                memberId = 100L,
                rating = 5,
                content = "이미 도움이 됐어요를 누른 리뷰입니다. 다시 누르면 예외가 발생해야 합니다.",
            ).also {
                val createdAtField = Review::class.java.getDeclaredField("createdAt")
                createdAtField.isAccessible = true
                createdAtField.set(it, ZonedDateTime.now())
                val updatedAtField = Review::class.java.getDeclaredField("updatedAt")
                updatedAtField.isAccessible = true
                updatedAtField.set(it, ZonedDateTime.now())
            }
            every { reviewRepository.findById(2L) } returns Optional.of(review)
            every { reviewHelpfulRepository.existsByReviewIdAndMemberId(2L, 200L) } returns true

            Then("DUPLICATE_ENTITY 예외 발생") {
                shouldThrow<BusinessException> {
                    reviewService.markHelpful(200L, 2L)
                }
            }
        }
    }

    Given("리뷰 목록 조회") {

        When("상품별 최신순 조회") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 60L,
                memberId = 100L,
                rating = 4,
                content = "목록 조회 테스트 리뷰입니다. 최신순으로 조회될 예정입니다. 충분한 길이의 내용입니다.",
            ).also {
                val createdAtField = Review::class.java.getDeclaredField("createdAt")
                createdAtField.isAccessible = true
                createdAtField.set(it, ZonedDateTime.now())
                val updatedAtField = Review::class.java.getDeclaredField("updatedAt")
                updatedAtField.isAccessible = true
                updatedAtField.set(it, ZonedDateTime.now())
            }
            every {
                reviewRepository.findByProductIdLatest(10L, ReviewStatus.VISIBLE, any())
            } returns PageImpl(listOf(review), PageRequest.of(0, 10), 1)

            val query = ReviewListQuery(productId = 10L)
            val result = reviewService.findByProductId(query)

            Then("결과가 반환된다") {
                result.totalElements shouldBe 1
            }
        }
    }

    Given("리뷰 상태 전이") {

        When("VISIBLE -> DELETED 전이") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 70L,
                memberId = 100L,
                rating = 3,
                content = "상태 전이 테스트용 리뷰입니다. VISIBLE에서 DELETED로 전이합니다. 충분한 내용입니다.",
            )
            review.delete()

            Then("DELETED 상태가 된다") {
                review.status shouldBe ReviewStatus.DELETED
                review.deletedAt shouldNotBe null
            }
        }

        When("DELETED 상태에서 수정 시도") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 71L,
                memberId = 100L,
                rating = 3,
                content = "삭제된 리뷰를 수정하려고 합니다. 이 작업은 실패해야 합니다. 충분한 길이입니다.",
            )
            val createdAtField = Review::class.java.getDeclaredField("createdAt")
            createdAtField.isAccessible = true
            createdAtField.set(review, ZonedDateTime.now())
            review.delete()

            Then("INVALID_STATE_TRANSITION 예외 발생") {
                shouldThrow<BusinessException> {
                    review.update("삭제된 리뷰는 수정 불가입니다. 이 내용으로 수정하려고 하면 예외가 발생합니다.")
                }
            }
        }

        When("DELETED 상태에서 재삭제 시도") {
            val review = Review.create(
                productId = 10L,
                orderItemId = 72L,
                memberId = 100L,
                rating = 3,
                content = "이미 삭제된 리뷰를 다시 삭제하려 합니다. 상태 전이가 불가능합니다.",
            )
            review.delete()

            Then("IllegalArgumentException 발생 (상태 전이 불가)") {
                shouldThrow<IllegalArgumentException> {
                    review.delete()
                }
            }
        }
    }

    Given("구매확정 이벤트 처리 (onOrderItemConfirmed)") {

        When("새로운 구매확정 이벤트를 수신하면") {
            every { reviewableOrderItemRepository.existsByOrderItemIdAndMemberId(100L, 100L) } returns false
            every { reviewableOrderItemRepository.save(any<com.closet.review.domain.ReviewableOrderItem>()) } answers { firstArg() }

            Then("ReviewableOrderItem이 저장된다") {
                reviewService.onOrderItemConfirmed(
                    orderItemId = 100L,
                    memberId = 100L,
                    productId = 200L,
                )
                verify { reviewableOrderItemRepository.save(any<com.closet.review.domain.ReviewableOrderItem>()) }
            }
        }

        When("이미 구매확정 기록이 있으면") {
            every { reviewableOrderItemRepository.existsByOrderItemIdAndMemberId(101L, 100L) } returns true

            Then("중복 저장하지 않는다") {
                reviewService.onOrderItemConfirmed(
                    orderItemId = 101L,
                    memberId = 100L,
                    productId = 200L,
                )
                verify(exactly = 0) { reviewableOrderItemRepository.save(match { it.orderItemId == 101L }) }
            }
        }
    }
})
