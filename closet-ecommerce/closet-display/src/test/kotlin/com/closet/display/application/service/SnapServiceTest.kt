package com.closet.display.application.service

import com.closet.common.exception.BusinessException
import com.closet.display.application.dto.SnapCreateRequest
import com.closet.display.application.dto.SnapProductTagRequest
import com.closet.display.domain.entity.Snap
import com.closet.display.domain.entity.SnapLike
import com.closet.display.domain.enums.SnapStatus
import com.closet.display.domain.repository.SnapLikeRepository
import com.closet.display.domain.repository.SnapRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Optional

class SnapServiceTest : BehaviorSpec({

    val snapRepository = mockk<SnapRepository>()
    val snapLikeRepository = mockk<SnapLikeRepository>()
    val snapService =
        SnapService(
            snapRepository = snapRepository,
            snapLikeRepository = snapLikeRepository,
        )

    Given("스냅 업로드") {
        val request =
            SnapCreateRequest(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
                description = "오늘의 코디",
                productTags =
                    listOf(
                        SnapProductTagRequest(productId = 100L, positionX = 0.5, positionY = 0.3),
                        SnapProductTagRequest(productId = 200L, positionX = 0.7, positionY = 0.6),
                    ),
            )

        val snapSlot = slot<Snap>()
        every { snapRepository.save(capture(snapSlot)) } answers { snapSlot.captured }

        When("업로드 요청") {
            val response = snapService.upload(request)

            Then("ACTIVE 상태로 생성되고 상품 태그가 포함된다") {
                response.memberId shouldBe 1L
                response.status shouldBe SnapStatus.ACTIVE
                response.productTags.size shouldBe 2
                response.productTags[0].productId shouldBe 100L
            }
        }
    }

    Given("스냅 피드 조회") {
        val snaps =
            listOf(
                Snap.create(memberId = 1L, imageUrl = "https://cdn.closet.com/snap/1.jpg"),
                Snap.create(memberId = 2L, imageUrl = "https://cdn.closet.com/snap/2.jpg"),
            )

        every { snapRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(SnapStatus.ACTIVE) } returns snaps

        When("피드 조회") {
            val response = snapService.getFeed()

            Then("활성 스냅 목록이 반환된다") {
                response.size shouldBe 2
            }
        }
    }

    Given("스냅 좋아요") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )

        every { snapRepository.findById(1L) } returns Optional.of(snap)
        every { snapLikeRepository.existsBySnapIdAndMemberId(1L, 10L) } returns false

        val likeSlot = slot<SnapLike>()
        every { snapLikeRepository.save(capture(likeSlot)) } answers { likeSlot.captured }

        When("좋아요 요청") {
            val response = snapService.like(1L, 10L)

            Then("좋아요 수가 증가한다") {
                response.likeCount shouldBe 1
            }
        }

        When("이미 좋아요한 상태에서 다시 좋아요") {
            every { snapLikeRepository.existsBySnapIdAndMemberId(1L, 10L) } returns true

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    snapService.like(1L, 10L)
                }
            }
        }
    }

    Given("스냅 좋아요 취소") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )
        snap.like() // likeCount = 1

        val snapLike = SnapLike(snapId = 1L, memberId = 10L)

        every { snapRepository.findById(1L) } returns Optional.of(snap)
        every { snapLikeRepository.findBySnapIdAndMemberId(1L, 10L) } returns snapLike
        every { snapLikeRepository.delete(any()) } returns Unit

        When("좋아요 취소") {
            val response = snapService.unlike(1L, 10L)

            Then("좋아요 수가 감소한다") {
                response.likeCount shouldBe 0
            }
        }
    }

    Given("스냅 신고") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )

        every { snapRepository.findById(1L) } returns Optional.of(snap)

        When("신고 요청") {
            val response = snapService.report(1L, 20L)

            Then("신고 수가 증가한다") {
                response.reportCount shouldBe 1
            }
        }
    }

    Given("스냅 5회 이상 신고") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )
        repeat(4) { snap.report() }

        every { snapRepository.findById(2L) } returns Optional.of(snap)

        When("5번째 신고") {
            val response = snapService.report(2L, 30L)

            Then("REPORTED 상태로 변경된다") {
                response.status shouldBe SnapStatus.REPORTED
                response.reportCount shouldBe 5
            }
        }
    }

    Given("스냅 삭제") {
        val snap =
            Snap.create(
                memberId = 1L,
                imageUrl = "https://cdn.closet.com/snap/ootd1.jpg",
            )

        every { snapRepository.findById(3L) } returns Optional.of(snap)

        When("삭제 요청") {
            snapService.delete(3L)

            Then("soft delete 처리된다") {
                snap.isDeleted() shouldBe true
            }
        }
    }

    Given("존재하지 않는 스냅") {
        every { snapRepository.findById(999L) } returns Optional.empty()

        When("조회 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    snapService.getById(999L)
                }
            }
        }
    }
})
