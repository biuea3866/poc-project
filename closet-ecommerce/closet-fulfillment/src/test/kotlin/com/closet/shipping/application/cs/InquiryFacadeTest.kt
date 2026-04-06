package com.closet.shipping.application.cs

import com.closet.shipping.domain.cs.inquiry.InquiryCategory
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class InquiryFacadeTest : BehaviorSpec({

    val inquiryService = mockk<InquiryService>(relaxed = true)

    val inquiryFacade = InquiryFacade(inquiryService)

    Given("문의 작성 Facade") {

        When("createInquiry 호출") {
            val request =
                CreateInquiryRequest(
                    orderId = null,
                    category = InquiryCategory.PRODUCT,
                    title = "사이즈 문의",
                    content = "M사이즈 실측 치수 알려주세요.",
                    attachments = emptyList(),
                )
            val expectedResponse =
                InquiryResponse(
                    id = 0L,
                    memberId = 1L,
                    orderId = null,
                    category = "PRODUCT",
                    title = "사이즈 문의",
                    content = "M사이즈 실측 치수 알려주세요.",
                    status = "PENDING",
                    answers = emptyList(),
                    attachments = emptyList(),
                    createdAt = null,
                    updatedAt = null,
                )
            every { inquiryService.createInquiry(1L, request) } returns expectedResponse

            val response = inquiryFacade.createInquiry(1L, request)

            Then("InquiryService.createInquiry가 호출됨") {
                verify(exactly = 1) { inquiryService.createInquiry(1L, request) }
            }

            Then("응답이 반환됨") {
                response.title shouldBe "사이즈 문의"
                response.status shouldBe "PENDING"
            }
        }
    }

    Given("문의 상세 조회 Facade") {

        When("findById 호출") {
            val expectedResponse =
                InquiryResponse(
                    id = 1L,
                    memberId = 1L,
                    orderId = null,
                    category = "PRODUCT",
                    title = "사이즈 문의",
                    content = "내용",
                    status = "PENDING",
                    answers = emptyList(),
                    attachments = emptyList(),
                    createdAt = null,
                    updatedAt = null,
                )
            every { inquiryService.findById(1L) } returns expectedResponse

            val response = inquiryFacade.findById(1L)

            Then("InquiryService.findById가 호출됨") {
                verify(exactly = 1) { inquiryService.findById(1L) }
            }

            Then("응답이 반환됨") {
                response.id shouldBe 1L
            }
        }
    }

    Given("내 문의 목록 조회 Facade") {

        When("findByMemberId 호출") {
            every { inquiryService.findByMemberId(1L) } returns
                listOf(
                    InquiryListResponse(
                        id = 1L,
                        category = "PRODUCT",
                        title = "문의 1",
                        status = "PENDING",
                        createdAt = null,
                    ),
                )

            val responses = inquiryFacade.findByMemberId(1L)

            Then("InquiryService.findByMemberId가 호출됨") {
                verify(exactly = 1) { inquiryService.findByMemberId(1L) }
            }

            Then("목록이 반환됨") {
                responses.size shouldBe 1
            }
        }
    }

    Given("답변 작성 Facade") {

        When("createAnswer 호출") {
            val request = CreateAnswerRequest(content = "답변 내용입니다.")
            val expectedResponse =
                InquiryAnswerResponse(
                    id = 0L,
                    inquiryId = 1L,
                    adminId = 100L,
                    content = "답변 내용입니다.",
                    createdAt = null,
                    updatedAt = null,
                )
            every { inquiryService.createAnswer(1L, 100L, request) } returns expectedResponse

            val response = inquiryFacade.createAnswer(1L, 100L, request)

            Then("InquiryService.createAnswer가 호출됨") {
                verify(exactly = 1) { inquiryService.createAnswer(1L, 100L, request) }
            }

            Then("답변 응답이 반환됨") {
                response.content shouldBe "답변 내용입니다."
            }
        }
    }

    Given("문의 닫기 Facade") {

        When("closeInquiry 호출") {
            every { inquiryService.closeInquiry(1L) } returns Unit

            inquiryFacade.closeInquiry(1L)

            Then("InquiryService.closeInquiry가 호출됨") {
                verify(exactly = 1) { inquiryService.closeInquiry(1L) }
            }
        }
    }
})
