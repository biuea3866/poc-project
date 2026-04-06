package com.closet.display.domain.entity

import com.closet.common.exception.BusinessException
import com.closet.display.domain.enums.SnapStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SnapTest : BehaviorSpec({

    Given("스냅 생성") {
        When("정상적인 값으로 생성") {
            val snap =
                Snap.create(
                    memberId = 1L,
                    imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
                    description = "오늘의 코디",
                )

            Then("ACTIVE 상태로 생성된다") {
                snap.memberId shouldBe 1L
                snap.imageUrl shouldBe "https://cdn.closet.com/snap/ootd1.jpg"
                snap.description shouldBe "오늘의 코디"
                snap.status shouldBe SnapStatus.ACTIVE
                snap.likeCount shouldBe 0
                snap.reportCount shouldBe 0
            }
        }

        When("빈 이미지 URL로 생성 시도") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Snap.create(
                        memberId = 1L,
                        imageUrl = "",
                    )
                }
            }
        }
    }

    Given("스냅 좋아요") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )

        When("좋아요") {
            snap.like()

            Then("좋아요 수가 증가한다") {
                snap.likeCount shouldBe 1
            }
        }

        When("좋아요 취소") {
            snap.unlike()

            Then("좋아요 수가 감소한다") {
                snap.likeCount shouldBe 0
            }
        }

        When("좋아요 수가 0일 때 취소") {
            snap.unlike()

            Then("0 미만으로 내려가지 않는다") {
                snap.likeCount shouldBe 0
            }
        }
    }

    Given("스냅 신고") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )

        When("4번 신고") {
            repeat(4) { snap.report() }

            Then("아직 ACTIVE 상태이다") {
                snap.status shouldBe SnapStatus.ACTIVE
                snap.reportCount shouldBe 4
            }
        }

        When("5번째 신고") {
            snap.report()

            Then("REPORTED 상태로 변경된다") {
                snap.status shouldBe SnapStatus.REPORTED
                snap.reportCount shouldBe 5
            }
        }
    }

    Given("스냅 숨김") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )

        When("숨김 처리") {
            snap.hide()

            Then("HIDDEN 상태가 된다") {
                snap.status shouldBe SnapStatus.HIDDEN
            }
        }
    }

    Given("숨긴 스냅 복구") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )
        snap.hide()

        When("복구") {
            snap.restore()

            Then("ACTIVE 상태가 되고 신고 수가 초기화된다") {
                snap.status shouldBe SnapStatus.ACTIVE
                snap.reportCount shouldBe 0
            }
        }
    }

    Given("HIDDEN 상태에서 좋아요 시도") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )
        snap.hide()

        When("좋아요 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    snap.like()
                }
            }
        }
    }

    Given("스냅 상품 태그 관리") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )

        When("상품 태그 추가") {
            val tag = SnapProductTag(productId = 100L, positionX = 0.5, positionY = 0.3)
            snap.addProductTag(tag)

            Then("태그가 추가된다") {
                snap.productTags.size shouldBe 1
                snap.productTags[0].productId shouldBe 100L
                snap.productTags[0].positionX shouldBe 0.5
                snap.productTags[0].positionY shouldBe 0.3
            }
        }

        When("상품 태그 제거") {
            snap.removeProductTag(100L)

            Then("태그가 제거된다") {
                snap.productTags.size shouldBe 0
            }
        }

        When("존재하지 않는 태그 제거 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    snap.removeProductTag(999L)
                }
            }
        }
    }

    Given("SnapStatus 상태 전이") {
        When("ACTIVE에서 HIDDEN으로 전이") {
            Then("전이 가능") {
                SnapStatus.ACTIVE.canTransitionTo(SnapStatus.HIDDEN) shouldBe true
            }
        }

        When("ACTIVE에서 REPORTED로 전이") {
            Then("전이 가능") {
                SnapStatus.ACTIVE.canTransitionTo(SnapStatus.REPORTED) shouldBe true
            }
        }

        When("HIDDEN에서 ACTIVE로 전이") {
            Then("전이 가능") {
                SnapStatus.HIDDEN.canTransitionTo(SnapStatus.ACTIVE) shouldBe true
            }
        }

        When("REPORTED에서 ACTIVE로 전이") {
            Then("전이 가능") {
                SnapStatus.REPORTED.canTransitionTo(SnapStatus.ACTIVE) shouldBe true
            }
        }
    }
})
