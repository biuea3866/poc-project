package com.closet.content.application.service

import com.closet.common.exception.BusinessException
import com.closet.content.application.dto.CoordinationCreateRequest
import com.closet.content.application.dto.CoordinationProductAddRequest
import com.closet.content.domain.entity.Coordination
import com.closet.content.domain.enums.CoordinationGender
import com.closet.content.domain.enums.CoordinationSeason
import com.closet.content.domain.enums.CoordinationStatus
import com.closet.content.domain.enums.CoordinationStyle
import com.closet.content.domain.repository.CoordinationRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Optional

class CoordinationServiceTest : BehaviorSpec({

    val coordinationRepository = mockk<CoordinationRepository>()
    val coordinationService = CoordinationService(coordinationRepository)

    Given("코디 생성 요청이 주어졌을 때") {
        val request = CoordinationCreateRequest(
            title = "캐주얼 여름 코디",
            description = "시원하고 편한 여름 캐주얼 룩",
            thumbnailUrl = "https://cdn.closet.com/coord/casual-summer.jpg",
            style = CoordinationStyle.CASUAL,
            season = CoordinationSeason.SS,
            gender = CoordinationGender.UNISEX
        )

        val coordinationSlot = slot<Coordination>()
        every { coordinationRepository.save(capture(coordinationSlot)) } answers {
            coordinationSlot.captured
        }

        When("create를 호출하면") {
            val response = coordinationService.create(request)

            Then("코디가 ACTIVE 상태로 생성된다") {
                response.title shouldBe "캐주얼 여름 코디"
                response.style shouldBe CoordinationStyle.CASUAL
                response.season shouldBe CoordinationSeason.SS
                response.gender shouldBe CoordinationGender.UNISEX
                response.status shouldBe CoordinationStatus.ACTIVE
                response.products.size shouldBe 0
            }
        }
    }

    Given("코디에 상품을 추가할 때") {
        val coordination = Coordination(
            title = "미니멀 코디",
            style = CoordinationStyle.MINIMAL,
            season = CoordinationSeason.ALL,
            gender = CoordinationGender.MALE
        )

        every { coordinationRepository.findById(1L) } returns Optional.of(coordination)
        every { coordinationRepository.flush() } returns Unit

        val productRequest = CoordinationProductAddRequest(
            productId = 100L,
            sortOrder = 1,
            description = "상의 - 화이트 셔츠"
        )

        When("addProduct를 호출하면") {
            val response = coordinationService.addProduct(1L, productRequest)

            Then("상품이 코디에 추가된다") {
                response.productId shouldBe 100L
                response.sortOrder shouldBe 1
                response.description shouldBe "상의 - 화이트 셔츠"
                coordination.products.size shouldBe 1
            }
        }
    }

    Given("이미 추가된 상품을 다시 추가하려 할 때") {
        val coordination = Coordination(
            title = "스트릿 코디",
            style = CoordinationStyle.STREET,
            season = CoordinationSeason.FW,
            gender = CoordinationGender.MALE
        )
        coordination.addProduct(200L, 1, "하의 - 카고 팬츠")

        every { coordinationRepository.findById(2L) } returns Optional.of(coordination)
        every { coordinationRepository.flush() } returns Unit

        val duplicateRequest = CoordinationProductAddRequest(
            productId = 200L,
            sortOrder = 2,
            description = "중복 상품"
        )

        When("addProduct를 호출하면") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    coordinationService.addProduct(2L, duplicateRequest)
                }
            }
        }
    }

    Given("존재하지 않는 코디 ID로 조회할 때") {
        every { coordinationRepository.findById(999L) } returns Optional.empty()

        When("findById를 호출하면") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    coordinationService.findById(999L)
                }
            }
        }
    }
})
