package com.closet.content.application.service

import com.closet.common.exception.BusinessException
import com.closet.content.application.dto.OotdSnapCreateRequest
import com.closet.content.domain.entity.OotdSnap
import com.closet.content.domain.enums.OotdSnapStatus
import com.closet.content.domain.repository.OotdSnapRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Optional

class OotdSnapServiceTest : BehaviorSpec({

    val ootdSnapRepository = mockk<OotdSnapRepository>()
    val ootdSnapService = OotdSnapService(ootdSnapRepository)

    Given("OOTD 스냅 생성 요청이 주어졌을 때") {
        val request = OotdSnapCreateRequest(
            imageUrl = "https://cdn.closet.com/snap/ootd-001.jpg",
            content = "오늘의 캐주얼 룩 #ootd"
        )

        val snapSlot = slot<OotdSnap>()
        every { ootdSnapRepository.save(capture(snapSlot)) } answers {
            snapSlot.captured
        }

        When("create를 호출하면") {
            val response = ootdSnapService.create(memberId = 42L, request = request)

            Then("스냅이 ACTIVE 상태로 생성된다") {
                response.memberId shouldBe 42L
                response.imageUrl shouldBe "https://cdn.closet.com/snap/ootd-001.jpg"
                response.content shouldBe "오늘의 캐주얼 룩 #ootd"
                response.likeCount shouldBe 0
                response.status shouldBe OotdSnapStatus.ACTIVE
            }
        }
    }

    Given("ACTIVE 상태의 스냅이 존재할 때") {
        val snap = OotdSnap(
            memberId = 42L,
            imageUrl = "https://cdn.closet.com/snap/ootd-002.jpg",
            content = "미니멀 룩"
        )

        every { ootdSnapRepository.findById(1L) } returns Optional.of(snap)

        When("like를 호출하면") {
            val response = ootdSnapService.like(1L)

            Then("좋아요 수가 증가한다") {
                response.likeCount shouldBe 1
            }
        }

        When("다시 like를 호출하면") {
            val response = ootdSnapService.like(1L)

            Then("좋아요 수가 한 번 더 증가한다") {
                response.likeCount shouldBe 2
            }
        }
    }

    Given("HIDDEN 상태의 스냅에 좋아요를 시도할 때") {
        val snap = OotdSnap(
            memberId = 42L,
            imageUrl = "https://cdn.closet.com/snap/ootd-003.jpg",
            content = "숨김 처리된 스냅",
            status = OotdSnapStatus.HIDDEN
        )

        every { ootdSnapRepository.findById(2L) } returns Optional.of(snap)

        When("like를 호출하면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    ootdSnapService.like(2L)
                }
            }
        }
    }

    Given("ACTIVE 상태의 스냅을 삭제할 때") {
        val snap = OotdSnap(
            memberId = 42L,
            imageUrl = "https://cdn.closet.com/snap/ootd-004.jpg",
            content = "삭제될 스냅"
        )

        every { ootdSnapRepository.findById(3L) } returns Optional.of(snap)

        When("delete를 호출하면") {
            ootdSnapService.delete(3L)

            Then("스냅이 DELETED 상태로 변경되고 soft delete된다") {
                snap.status shouldBe OotdSnapStatus.DELETED
                snap.isDeleted() shouldBe true
            }
        }
    }

    Given("존재하지 않는 스냅 ID로 조회할 때") {
        every { ootdSnapRepository.findById(999L) } returns Optional.empty()

        When("findById를 호출하면") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    ootdSnapService.findById(999L)
                }
            }
        }
    }
})
