package com.closet.content.application.service

import com.closet.common.exception.BusinessException
import com.closet.content.application.dto.MagazineCreateRequest
import com.closet.content.domain.entity.Magazine
import com.closet.content.domain.enums.MagazineStatus
import com.closet.content.domain.repository.MagazineRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Optional

class MagazineServiceTest : BehaviorSpec({

    val magazineRepository = mockk<MagazineRepository>()
    val magazineService = MagazineService(magazineRepository)

    Given("매거진 생성 요청이 주어졌을 때") {
        val request = MagazineCreateRequest(
            title = "2026 SS 트렌드 가이드",
            subtitle = "올 여름 필수 아이템 총정리",
            content = "올 시즌 주목할 트렌드를 소개합니다...",
            thumbnailUrl = "https://cdn.closet.com/magazine/ss-trend.jpg",
            author = "에디터 김",
            tags = listOf("SS시즌", "트렌드", "여름")
        )

        val magazineSlot = slot<Magazine>()
        every { magazineRepository.save(capture(magazineSlot)) } answers {
            magazineSlot.captured
        }

        When("create를 호출하면") {
            val response = magazineService.create(request)

            Then("매거진이 DRAFT 상태로 생성된다") {
                response.title shouldBe "2026 SS 트렌드 가이드"
                response.subtitle shouldBe "올 여름 필수 아이템 총정리"
                response.author shouldBe "에디터 김"
                response.status shouldBe MagazineStatus.DRAFT
                response.tags.size shouldBe 3
                response.tags shouldBe listOf("SS시즌", "트렌드", "여름")
            }
        }
    }

    Given("DRAFT 상태의 매거진이 존재할 때") {
        val magazine = Magazine(
            title = "스트릿 패션 매거진",
            content = "스트릿 패션의 역사와 현재...",
            author = "에디터 박"
        )

        every { magazineRepository.findById(1L) } returns Optional.of(magazine)

        When("publish를 호출하면") {
            val response = magazineService.publish(1L)

            Then("매거진이 PUBLISHED 상태로 변경되고 publishedAt이 설정된다") {
                response.status shouldBe MagazineStatus.PUBLISHED
                response.publishedAt shouldNotBe null
            }
        }
    }

    Given("PUBLISHED 상태의 매거진이 존재할 때") {
        val magazine = Magazine(
            title = "겨울 코디 매거진",
            content = "겨울 코디 추천...",
            author = "에디터 이",
            status = MagazineStatus.PUBLISHED
        )

        every { magazineRepository.findById(2L) } returns Optional.of(magazine)

        When("archive를 호출하면") {
            val response = magazineService.archive(2L)

            Then("매거진이 ARCHIVED 상태로 변경된다") {
                response.status shouldBe MagazineStatus.ARCHIVED
            }
        }
    }

    Given("DRAFT 상태의 매거진에 archive를 시도할 때") {
        val magazine = Magazine(
            title = "미발행 매거진",
            content = "테스트 내용...",
            author = "에디터 최",
            status = MagazineStatus.DRAFT
        )

        every { magazineRepository.findById(3L) } returns Optional.of(magazine)

        When("archive를 호출하면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    magazineService.archive(3L)
                }
            }
        }
    }

    Given("존재하지 않는 매거진 ID로 조회할 때") {
        every { magazineRepository.findById(999L) } returns Optional.empty()

        When("findById를 호출하면") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    magazineService.findById(999L)
                }
            }
        }
    }
})
