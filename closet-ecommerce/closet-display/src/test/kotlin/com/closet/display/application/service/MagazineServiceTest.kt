package com.closet.display.application.service

import com.closet.common.exception.BusinessException
import com.closet.display.application.dto.MagazineCreateRequest
import com.closet.display.application.dto.MagazineProductCreateRequest
import com.closet.display.application.dto.MagazineTagCreateRequest
import com.closet.display.application.dto.MagazineUpdateRequest
import com.closet.display.domain.entity.Magazine
import com.closet.display.domain.repository.MagazineRepository
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
    val magazineService = MagazineService(magazineRepository = magazineRepository)

    Given("매거진 생성") {
        val request =
            MagazineCreateRequest(
                title = "2026 SS 트렌드 가이드",
                subtitle = "봄 패션 키워드",
                contentBody = "이번 시즌 트렌드를 소개합니다.",
                thumbnailUrl = "https://cdn.closet.com/magazine/ss2026.jpg",
                category = "TREND",
                authorName = "패션에디터",
            )

        val magazineSlot = slot<Magazine>()
        every { magazineRepository.save(capture(magazineSlot)) } answers { magazineSlot.captured }

        When("생성 요청") {
            val response = magazineService.create(request)

            Then("미발행 상태로 생성된다") {
                response.title shouldBe "2026 SS 트렌드 가이드"
                response.isPublished shouldBe false
                response.category shouldBe "TREND"
            }
        }
    }

    Given("매거진 수정") {
        val magazine =
            Magazine(
                title = "원래 제목",
                contentBody = "원래 내용",
                category = "TREND",
                authorName = "에디터",
            )

        every { magazineRepository.findById(1L) } returns Optional.of(magazine)

        val updateRequest =
            MagazineUpdateRequest(
                title = "수정된 제목",
                subtitle = "부제목",
                contentBody = "수정된 내용",
                thumbnailUrl = "https://cdn.closet.com/new.jpg",
                category = "STYLING",
                authorName = "새 에디터",
            )

        When("수정 요청") {
            val response = magazineService.update(1L, updateRequest)

            Then("정보가 변경된다") {
                response.title shouldBe "수정된 제목"
                response.category shouldBe "STYLING"
            }
        }
    }

    Given("매거진 발행") {
        val magazine =
            Magazine(
                title = "발행 대기 매거진",
                contentBody = "내용",
                category = "TREND",
                authorName = "에디터",
            )

        every { magazineRepository.findById(2L) } returns Optional.of(magazine)

        When("발행 요청") {
            val response = magazineService.publish(2L)

            Then("발행 상태가 된다") {
                response.isPublished shouldBe true
                response.publishedAt shouldNotBe null
            }
        }
    }

    Given("발행된 매거진 목록 조회") {
        val magazines =
            listOf(
                Magazine(
                    title = "매거진 1",
                    contentBody = "내용 1",
                    category = "TREND",
                    authorName = "에디터",
                    isPublished = true,
                ),
                Magazine(
                    title = "매거진 2",
                    contentBody = "내용 2",
                    category = "STYLING",
                    authorName = "에디터",
                    isPublished = true,
                ),
            )

        every { magazineRepository.findByIsPublishedTrueAndDeletedAtIsNullOrderByPublishedAtDesc() } returns magazines
        every { magazineRepository.findByCategoryAndIsPublishedTrueAndDeletedAtIsNullOrderByPublishedAtDesc("TREND") } returns
            magazines.filter { it.category == "TREND" }

        When("전체 조회") {
            val response = magazineService.getPublishedList(null)

            Then("발행된 매거진 전체 목록이 반환된다") {
                response.size shouldBe 2
            }
        }

        When("카테고리 필터 조회") {
            val response = magazineService.getPublishedList("TREND")

            Then("해당 카테고리 매거진만 반환된다") {
                response.size shouldBe 1
                response[0].category shouldBe "TREND"
            }
        }
    }

    Given("매거진 상품 연결") {
        val magazine =
            Magazine(
                title = "상품 연결 매거진",
                contentBody = "상품 소개",
                category = "LOOKBOOK",
                authorName = "에디터",
            )

        every { magazineRepository.findById(3L) } returns Optional.of(magazine)
        every { magazineRepository.flush() } returns Unit

        When("상품 추가") {
            val request = MagazineProductCreateRequest(productId = 100L, sortOrder = 1)
            val response = magazineService.addProduct(3L, request)

            Then("상품이 추가된다") {
                response.productId shouldBe 100L
                response.sortOrder shouldBe 1
            }
        }
    }

    Given("매거진 태그 추가") {
        val magazine =
            Magazine(
                title = "태그 매거진",
                contentBody = "내용",
                category = "TREND",
                authorName = "에디터",
            )

        every { magazineRepository.findById(4L) } returns Optional.of(magazine)
        every { magazineRepository.flush() } returns Unit

        When("태그 추가") {
            val request = MagazineTagCreateRequest(tagName = "봄코디")
            val response = magazineService.addTag(4L, request)

            Then("태그가 추가된다") {
                response.tagName shouldBe "봄코디"
            }
        }
    }

    Given("존재하지 않는 매거진") {
        every { magazineRepository.findById(999L) } returns Optional.empty()

        When("조회 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    magazineService.getById(999L)
                }
            }
        }
    }
})
