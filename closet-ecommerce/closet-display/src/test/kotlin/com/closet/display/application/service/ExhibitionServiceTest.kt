package com.closet.display.application.service

import com.closet.display.application.dto.ExhibitionCreateRequest
import com.closet.display.application.dto.ExhibitionProductCreateRequest
import com.closet.display.domain.entity.Exhibition
import com.closet.display.domain.enums.ExhibitionStatus
import com.closet.display.domain.repository.ExhibitionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.ZonedDateTime
import java.util.Optional

class ExhibitionServiceTest : BehaviorSpec({

    val exhibitionRepository = mockk<ExhibitionRepository>()
    val exhibitionService = ExhibitionService(exhibitionRepository)

    Given("기획전 생성 요청이 주어졌을 때") {
        val now = ZonedDateTime.now()
        val request =
            ExhibitionCreateRequest(
                title = "여름 특가 기획전",
                description = "여름 맞이 최대 50% 할인",
                thumbnailUrl = "https://cdn.closet.com/exhibitions/summer.jpg",
                startAt = now,
                endAt = now.plusDays(14),
            )

        val exhibitionSlot = slot<Exhibition>()
        every { exhibitionRepository.save(capture(exhibitionSlot)) } answers { exhibitionSlot.captured }

        When("create를 호출하면") {
            val response = exhibitionService.create(request)

            Then("기획전이 DRAFT 상태로 생성된다") {
                response.title shouldBe "여름 특가 기획전"
                response.status shouldBe ExhibitionStatus.DRAFT
            }
        }
    }

    Given("DRAFT 상태의 기획전이 존재할 때") {
        val now = ZonedDateTime.now()
        val exhibition =
            Exhibition(
                title = "봄 신상 기획전",
                description = "봄 시즌 신상품 모음",
                status = ExhibitionStatus.DRAFT,
                startAt = now,
                endAt = now.plusDays(7),
            )

        every { exhibitionRepository.findById(1L) } returns Optional.of(exhibition)

        When("activate를 호출하면") {
            val response = exhibitionService.activate(1L)

            Then("상태가 ACTIVE로 변경된다") {
                response.status shouldBe ExhibitionStatus.ACTIVE
            }
        }
    }

    Given("ACTIVE 상태의 기획전이 존재할 때") {
        val now = ZonedDateTime.now()
        val exhibition =
            Exhibition(
                title = "겨울 아우터 기획전",
                description = "겨울 아우터 모음",
                status = ExhibitionStatus.ACTIVE,
                startAt = now.minusDays(7),
                endAt = now.plusDays(7),
            )

        every { exhibitionRepository.findById(2L) } returns Optional.of(exhibition)

        When("end를 호출하면") {
            val response = exhibitionService.end(2L)

            Then("상태가 ENDED로 변경된다") {
                response.status shouldBe ExhibitionStatus.ENDED
            }
        }
    }

    Given("ENDED 상태의 기획전이 존재할 때") {
        val now = ZonedDateTime.now()
        val exhibition =
            Exhibition(
                title = "종료된 기획전",
                description = "이미 종료됨",
                status = ExhibitionStatus.ENDED,
                startAt = now.minusDays(14),
                endAt = now.minusDays(7),
            )

        every { exhibitionRepository.findById(3L) } returns Optional.of(exhibition)

        When("activate를 시도하면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    exhibitionService.activate(3L)
                }
            }
        }
    }

    Given("ACTIVE 상태의 기획전에 상품을 추가할 때") {
        val now = ZonedDateTime.now()
        val exhibition =
            Exhibition(
                title = "상품 추가 테스트 기획전",
                description = "테스트",
                status = ExhibitionStatus.ACTIVE,
                startAt = now,
                endAt = now.plusDays(7),
            )

        every { exhibitionRepository.findById(4L) } returns Optional.of(exhibition)
        every { exhibitionRepository.flush() } returns Unit

        val request =
            ExhibitionProductCreateRequest(
                productId = 100L,
                sortOrder = 1,
                discountRate = 20,
            )

        When("addProduct를 호출하면") {
            val response = exhibitionService.addProduct(4L, request)

            Then("상품이 기획전에 추가된다") {
                response.productId shouldBe 100L
                response.sortOrder shouldBe 1
                response.discountRate shouldBe 20
                exhibition.products.size shouldBe 1
            }
        }
    }

    Given("ENDED 상태의 기획전에 상품을 추가하려 할 때") {
        val now = ZonedDateTime.now()
        val exhibition =
            Exhibition(
                title = "종료된 기획전",
                description = "종료됨",
                status = ExhibitionStatus.ENDED,
                startAt = now.minusDays(14),
                endAt = now.minusDays(7),
            )

        every { exhibitionRepository.findById(5L) } returns Optional.of(exhibition)
        every { exhibitionRepository.flush() } returns Unit

        val request =
            ExhibitionProductCreateRequest(
                productId = 200L,
                sortOrder = 1,
                discountRate = 10,
            )

        When("addProduct를 호출하면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    exhibitionService.addProduct(5L, request)
                }
            }
        }
    }
})
