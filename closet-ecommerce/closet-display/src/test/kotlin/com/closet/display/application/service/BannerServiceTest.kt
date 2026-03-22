package com.closet.display.application.service

import com.closet.common.exception.BusinessException
import com.closet.display.application.dto.BannerCreateRequest
import com.closet.display.domain.entity.Banner
import com.closet.display.domain.enums.BannerPosition
import com.closet.display.domain.repository.BannerRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDateTime
import java.util.Optional

class BannerServiceTest : BehaviorSpec({

    val bannerRepository = mockk<BannerRepository>()
    val bannerService = BannerService(bannerRepository)

    Given("배너 생성 요청이 주어졌을 때") {
        val now = LocalDateTime.now()
        val request = BannerCreateRequest(
            title = "여름 세일 배너",
            imageUrl = "https://cdn.closet.com/banners/summer-sale.jpg",
            linkUrl = "/exhibitions/summer-sale",
            position = BannerPosition.MAIN_TOP,
            sortOrder = 1,
            startAt = now,
            endAt = now.plusDays(30)
        )

        val bannerSlot = slot<Banner>()
        every { bannerRepository.save(capture(bannerSlot)) } answers { bannerSlot.captured }

        When("create를 호출하면") {
            val response = bannerService.create(request)

            Then("배너가 노출 상태로 생성된다") {
                response.title shouldBe "여름 세일 배너"
                response.position shouldBe BannerPosition.MAIN_TOP
                response.isVisible shouldBe true
                response.sortOrder shouldBe 1
            }
        }
    }

    Given("활성화된 배너가 존재할 때") {
        val now = LocalDateTime.now()
        val banner = Banner(
            title = "봄 신상 배너",
            imageUrl = "https://cdn.closet.com/banners/spring.jpg",
            linkUrl = "/exhibitions/spring",
            position = BannerPosition.MAIN_TOP,
            sortOrder = 0,
            isVisible = true,
            startAt = now.minusDays(1),
            endAt = now.plusDays(29)
        )

        When("isActive를 현재 시간으로 확인하면") {
            val result = banner.isActive(now)

            Then("true를 반환한다") {
                result shouldBe true
            }
        }

        When("종료 이후 시간으로 isActive를 확인하면") {
            val result = banner.isActive(now.plusDays(30))

            Then("false를 반환한다") {
                result shouldBe false
            }
        }
    }

    Given("노출 중인 배너가 존재할 때") {
        val now = LocalDateTime.now()
        val banner = Banner(
            title = "테스트 배너",
            imageUrl = "https://cdn.closet.com/banners/test.jpg",
            linkUrl = "/test",
            position = BannerPosition.MAIN_MIDDLE,
            sortOrder = 0,
            isVisible = true,
            startAt = now.minusDays(1),
            endAt = now.plusDays(29)
        )

        every { bannerRepository.findById(1L) } returns Optional.of(banner)

        When("toggleVisibility를 호출하면") {
            val response = bannerService.toggleVisibility(1L)

            Then("노출 상태가 숨김으로 변경된다") {
                response.isVisible shouldBe false
            }
        }
    }

    Given("존재하지 않는 배너 ID로 조회할 때") {
        every { bannerRepository.findById(999L) } returns Optional.empty()

        When("toggleVisibility를 호출하면") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    bannerService.toggleVisibility(999L)
                }
            }
        }
    }
})
