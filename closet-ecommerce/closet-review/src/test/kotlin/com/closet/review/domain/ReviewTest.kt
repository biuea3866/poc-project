package com.closet.review.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZonedDateTime

/**
 * Review 엔티티 단위 테스트.
 *
 * 비즈니스 로직이 엔티티에 캡슐화되어 있으므로 엔티티 레벨에서 검증한다.
 */
class ReviewTest : BehaviorSpec({

    fun setCreatedAt(
        review: Review,
        createdAt: ZonedDateTime,
    ) {
        val field = Review::class.java.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(review, createdAt)
    }

    fun setUpdatedAt(
        review: Review,
        updatedAt: ZonedDateTime,
    ) {
        val field = Review::class.java.getDeclaredField("updatedAt")
        field.isAccessible = true
        field.set(review, updatedAt)
    }

    Given("리뷰 생성 (US-801)") {

        When("유효한 파라미터로 생성하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 10L,
                    memberId = 100L,
                    rating = 5,
                    content = "이 옷 정말 좋아요! 사이즈도 딱 맞고 소재감도 너무 마음에 듭니다.",
                )

            Then("리뷰가 VISIBLE 상태로 생성된다") {
                review.productId shouldBe 1L
                review.orderItemId shouldBe 10L
                review.memberId shouldBe 100L
                review.rating shouldBe 5
                review.status shouldBe ReviewStatus.VISIBLE
                review.editCount shouldBe 0
                review.hasImage shouldBe false
            }
        }

        When("별점이 0이면") {
            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 1L,
                        orderItemId = 1L,
                        memberId = 1L,
                        rating = 0,
                        content = "별점 0은 유효하지 않습니다. 별점은 1에서 5 사이여야 합니다.",
                    )
                }
            }
        }

        When("별점이 6이면") {
            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 1L,
                        orderItemId = 1L,
                        memberId = 1L,
                        rating = 6,
                        content = "별점 6도 유효하지 않습니다. 별점은 1에서 5 사이여야 합니다.",
                    )
                }
            }
        }

        When("내용이 20자 미만이면") {
            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 1L,
                        orderItemId = 1L,
                        memberId = 1L,
                        rating = 5,
                        content = "짧은 내용",
                    )
                }
            }
        }

        When("내용이 1000자 초과이면") {
            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    Review.create(
                        productId = 1L,
                        orderItemId = 1L,
                        memberId = 1L,
                        rating = 5,
                        content = "가".repeat(1001),
                    )
                }
            }
        }

        When("내용이 정확히 20자이면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 5,
                    content = "가".repeat(20),
                )
            Then("정상 생성된다") {
                review.content.length shouldBe 20
            }
        }

        When("내용이 정확히 1000자이면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 5,
                    content = "가".repeat(1000),
                )
            Then("정상 생성된다") {
                review.content.length shouldBe 1000
            }
        }
    }

    Given("이미지 관리 (US-801)") {

        When("이미지를 5장 추가하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 5,
                    content = "이미지 5장을 추가하는 테스트 리뷰입니다. 최대 5장까지 가능합니다.",
                )
            repeat(5) { idx ->
                review.addImage("https://img/$idx.jpg", "https://img/${idx}_thumb.jpg", idx)
            }

            Then("이미지가 5장 등록되고 hasImage가 true이다") {
                review.images.size shouldBe 5
                review.hasImage shouldBe true
                review.isPhotoReview() shouldBe true
            }
        }

        When("이미지를 6장 추가하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 5,
                    content = "이미지 6장을 추가하려는 테스트 리뷰입니다. 최대 5장을 초과합니다.",
                )
            repeat(5) { idx ->
                review.addImage("https://img/$idx.jpg", "https://img/${idx}_thumb.jpg", idx)
            }

            Then("BusinessException 발생") {
                shouldThrow<com.closet.common.exception.BusinessException> {
                    review.addImage("https://img/5.jpg", "https://img/5_thumb.jpg", 5)
                }
            }
        }

        When("이미지 전체 교체 시 6장이면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 5,
                    content = "이미지 교체 테스트 리뷰입니다. 6장 교체를 시도하면 실패해야 합니다.",
                )
            val newImages =
                (0..5).map {
                    ReviewImage(
                        review = review,
                        imageUrl = "https://new/$it.jpg",
                        thumbnailUrl = "https://new/${it}_thumb.jpg",
                        displayOrder = it,
                    )
                }

            Then("BusinessException 발생") {
                shouldThrow<com.closet.common.exception.BusinessException> {
                    review.replaceImages(newImages)
                }
            }
        }

        When("이미지 전체 교체 시 빈 리스트이면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 5,
                    content = "이미지를 모두 제거하는 교체 테스트 리뷰입니다. hasImage가 false가 됩니다.",
                )
            review.addImage("https://img/0.jpg", "https://img/0_thumb.jpg", 0)
            review.replaceImages(emptyList())

            Then("hasImage가 false가 된다") {
                review.images.size shouldBe 0
                review.hasImage shouldBe false
            }
        }
    }

    Given("리뷰 수정 (US-801)") {

        When("7일 이내, 3회 미만일 때 수정하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "원래 리뷰 내용입니다. 이 리뷰를 수정할 예정입니다. 충분한 길이입니다.",
                )
            setCreatedAt(review, ZonedDateTime.now().minusDays(3))

            review.update("수정된 리뷰 내용입니다. 3일 이내에 수정하면 정상 처리됩니다. 길이 충분합니다.")

            Then("내용이 변경되고 editCount가 증가한다") {
                review.content shouldBe "수정된 리뷰 내용입니다. 3일 이내에 수정하면 정상 처리됩니다. 길이 충분합니다."
                review.editCount shouldBe 1
            }
        }

        When("3회 수정 후 추가 수정하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "세 번 수정한 리뷰입니다. 추가 수정은 불가합니다. 충분한 길이입니다.",
                )
            setCreatedAt(review, ZonedDateTime.now())
            review.editCount = 3

            Then("BusinessException 발생") {
                shouldThrow<com.closet.common.exception.BusinessException> {
                    review.update("네 번째 수정 시도. 이 수정은 거부되어야 합니다. 충분한 길이의 내용입니다.")
                }
            }
        }

        When("7일 이후에 수정하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "8일 전에 작성한 리뷰입니다. 수정 기한이 지났습니다. 길이 충분합니다.",
                )
            setCreatedAt(review, ZonedDateTime.now().minusDays(8))

            Then("BusinessException 발생") {
                shouldThrow<com.closet.common.exception.BusinessException> {
                    review.update("기한 초과 수정 시도입니다. 이 수정은 거부되어야 합니다. 충분한 내용입니다.")
                }
            }
        }

        When("삭제된 리뷰를 수정하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "삭제된 리뷰를 수정하는 테스트입니다. 이 작업은 실패합니다. 충분한 길이입니다.",
                )
            setCreatedAt(review, ZonedDateTime.now())
            review.delete()

            Then("BusinessException 발생") {
                shouldThrow<com.closet.common.exception.BusinessException> {
                    review.update("삭제 후 수정 시도입니다. 이 수정은 거부됩니다. 충분한 길이의 내용입니다.")
                }
            }
        }

        When("수정 내용이 20자 미만이면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "수정 내용 길이 검증 테스트입니다. 수정 시에도 내용 길이를 검증합니다.",
                )
            setCreatedAt(review, ZonedDateTime.now())

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    review.update("짧음")
                }
            }
        }
    }

    Given("리뷰 삭제 (US-801)") {

        When("VISIBLE 리뷰를 삭제하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 3,
                    content = "삭제 테스트용 리뷰입니다. VISIBLE에서 DELETED로 전이합니다. 충분합니다.",
                )
            review.delete()

            Then("DELETED 상태가 되고 deletedAt이 설정된다") {
                review.status shouldBe ReviewStatus.DELETED
                review.deletedAt shouldNotBe null
            }
        }

        When("이미 삭제된 리뷰를 다시 삭제하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 3,
                    content = "이미 삭제된 리뷰를 다시 삭제하는 테스트입니다. 상태 전이 불가합니다.",
                )
            review.delete()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    review.delete()
                }
            }
        }
    }

    Given("관리자 블라인드") {

        When("VISIBLE 리뷰를 블라인드하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 3,
                    content = "블라인드 테스트 리뷰입니다. HIDDEN 상태로 변경될 예정입니다. 충분합니다.",
                )
            review.hide()

            Then("HIDDEN 상태가 된다") {
                review.status shouldBe ReviewStatus.HIDDEN
            }
        }

        When("HIDDEN 리뷰를 블라인드 해제하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 3,
                    content = "블라인드 해제 테스트입니다. VISIBLE로 복원될 예정입니다. 충분한 길이입니다.",
                )
            review.hide()
            review.unhide()

            Then("VISIBLE 상태가 된다") {
                review.status shouldBe ReviewStatus.VISIBLE
            }
        }

        When("DELETED 리뷰를 블라인드하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 3,
                    content = "삭제된 리뷰를 블라인드 시도하는 테스트입니다. 전이 불가합니다. 길이 충분.",
                )
            review.delete()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    review.hide()
                }
            }
        }
    }

    Given("사이즈 후기 (US-802)") {

        When("키, 몸무게, 핏 타입을 모두 입력하면") {
            val review =
                Review.create(
                    productId = 1L, orderItemId = 1L, memberId = 1L,
                    rating = 4,
                    content = "사이즈 후기 포함 리뷰입니다. 키 175, 몸무게 70, 핏타입 PERFECT 입니다.",
                    height = 175, weight = 70,
                    normalSize = "M", purchasedSize = "L",
                    fitType = SizeFit.PERFECT,
                )

            Then("hasSizeInfo()가 true이다") {
                review.hasSizeInfo() shouldBe true
                review.height shouldBe 175
                review.weight shouldBe 70
                review.fitType shouldBe SizeFit.PERFECT
            }
        }

        When("키만 입력하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "키만 입력한 리뷰입니다. 사이즈 정보가 불완전하여 false입니다. 충분한 길이.",
                    height = 175,
                )

            Then("hasSizeInfo()가 false이다") {
                review.hasSizeInfo() shouldBe false
            }
        }

        When("fitType만 입력하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "핏 타입만 입력한 리뷰입니다. 키와 몸무게가 없어 false입니다. 충분한 길이.",
                    fitType = SizeFit.LARGE,
                )

            Then("hasSizeInfo()가 false이다") {
                review.hasSizeInfo() shouldBe false
            }
        }
    }

    Given("포인트 계산 (US-803)") {

        When("텍스트 리뷰만 작성하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "텍스트 리뷰입니다. 포인트 100P가 적립되어야 합니다. 충분한 길이의 리뷰입니다.",
                )

            Then("100P") {
                review.calculatePointAmount() shouldBe 100
            }
        }

        When("텍스트 리뷰 + 사이즈 정보") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "텍스트 + 사이즈 리뷰입니다. 포인트 150P가 적립됩니다. 충분한 길이의 리뷰입니다.",
                    height = 170,
                    weight = 65,
                    fitType = SizeFit.SMALL,
                )

            Then("150P (100 + 50)") {
                review.calculatePointAmount() shouldBe 150
            }
        }

        When("포토 리뷰만 작성하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 5,
                    content = "포토 리뷰입니다. 이미지 포함하여 300P가 적립됩니다. 충분한 길이의 리뷰입니다.",
                )
            review.addImage("https://img.jpg", "https://thumb.jpg", 0)

            Then("300P") {
                review.calculatePointAmount() shouldBe 300
            }
        }

        When("포토 리뷰 + 사이즈 정보 (최대 조합)") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 5,
                    content = "포토 + 사이즈 리뷰입니다. 최대 350P가 적립됩니다. 충분한 길이의 리뷰입니다.",
                    height = 180,
                    weight = 75,
                    fitType = SizeFit.LARGE,
                )
            review.addImage("https://img.jpg", "https://thumb.jpg", 0)

            Then("350P (300 + 50)") {
                review.calculatePointAmount() shouldBe 350
            }
        }
    }

    Given("도움이 됐어요 카운트") {

        When("incrementHelpfulCount를 호출하면") {
            val review =
                Review.create(
                    productId = 1L,
                    orderItemId = 1L,
                    memberId = 1L,
                    rating = 4,
                    content = "도움이 됐어요 카운트 테스트 리뷰입니다. 카운트가 증가해야 합니다.",
                )
            review.incrementHelpfulCount()
            review.incrementHelpfulCount()

            Then("helpfulCount가 누적 증가한다") {
                review.helpfulCount shouldBe 2
            }
        }
    }
})
