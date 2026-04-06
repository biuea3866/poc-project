package com.closet.shipping.application.cs

import com.closet.common.exception.BusinessException
import com.closet.shipping.domain.cs.inquiry.Inquiry
import com.closet.shipping.domain.cs.inquiry.InquiryAnswerRepository
import com.closet.shipping.domain.cs.inquiry.InquiryAttachment
import com.closet.shipping.domain.cs.inquiry.InquiryAttachmentRepository
import com.closet.shipping.domain.cs.inquiry.InquiryCategory
import com.closet.shipping.domain.cs.inquiry.InquiryRepository
import com.closet.shipping.domain.cs.inquiry.InquiryStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class InquiryServiceTest : BehaviorSpec({

    val inquiryRepository = mockk<InquiryRepository>(relaxed = true)
    val inquiryAnswerRepository = mockk<InquiryAnswerRepository>(relaxed = true)
    val inquiryAttachmentRepository = mockk<InquiryAttachmentRepository>(relaxed = true)

    val inquiryService =
        InquiryService(
            inquiryRepository,
            inquiryAnswerRepository,
            inquiryAttachmentRepository,
        )

    Given("문의 작성") {

        When("상품 문의 작성 요청") {
            every { inquiryRepository.save(any()) } answers { firstArg() }
            every { inquiryAttachmentRepository.saveAll(any<List<InquiryAttachment>>()) } answers { firstArg() }

            val response =
                inquiryService.createInquiry(
                    memberId = 1L,
                    request =
                        CreateInquiryRequest(
                            orderId = null,
                            category = InquiryCategory.PRODUCT,
                            title = "사이즈 문의",
                            content = "M사이즈 실측 치수 알려주세요.",
                            attachments = emptyList(),
                        ),
                )

            Then("상태는 PENDING") {
                response.status shouldBe "PENDING"
            }

            Then("카테고리는 PRODUCT") {
                response.category shouldBe "PRODUCT"
            }

            Then("제목과 내용이 설정됨") {
                response.title shouldBe "사이즈 문의"
                response.content shouldBe "M사이즈 실측 치수 알려주세요."
            }
        }

        When("첨부파일 포함 문의 작성") {
            every { inquiryRepository.save(any()) } answers { firstArg() }
            every { inquiryAttachmentRepository.saveAll(any<List<InquiryAttachment>>()) } answers { firstArg() }

            val response =
                inquiryService.createInquiry(
                    memberId = 1L,
                    request =
                        CreateInquiryRequest(
                            orderId = 10L,
                            category = InquiryCategory.DELIVERY,
                            title = "배송 지연 문의",
                            content = "주문한지 5일인데 아직 배송이 안 됩니다.",
                            attachments =
                                listOf(
                                    AttachmentRequest(
                                        fileUrl = "https://s3.amazonaws.com/closet/inquiry/1/capture.png",
                                        fileName = "capture.png",
                                        fileSize = 2048L,
                                    ),
                                ),
                        ),
                )

            Then("문의가 저장됨") {
                verify(exactly = 1) { inquiryRepository.save(any()) }
            }

            Then("첨부파일이 저장됨") {
                verify(exactly = 1) { inquiryAttachmentRepository.saveAll(any<List<InquiryAttachment>>()) }
            }
        }
    }

    Given("문의 상세 조회") {

        When("존재하는 문의 조회") {
            val inquiry =
                Inquiry.create(
                    memberId = 1L,
                    orderId = null,
                    category = InquiryCategory.PRODUCT,
                    title = "사이즈 문의",
                    content = "M사이즈 실측 치수 알려주세요.",
                )
            every { inquiryRepository.findById(1L) } returns Optional.of(inquiry)
            every { inquiryAnswerRepository.findByInquiryIdOrderByCreatedAtAsc(any()) } returns emptyList()
            every { inquiryAttachmentRepository.findByInquiryId(any()) } returns emptyList()

            val response = inquiryService.findById(1L)

            Then("문의 정보가 반환됨") {
                response.title shouldBe "사이즈 문의"
            }
        }

        When("존재하지 않는 문의 조회") {
            every { inquiryRepository.findById(999L) } returns Optional.empty()

            Then("BusinessException 발생") {
                shouldThrow<BusinessException> {
                    inquiryService.findById(999L)
                }
            }
        }
    }

    Given("내 문의 목록 조회") {

        When("회원의 문의 목록 조회") {
            val inquiry1 =
                Inquiry.create(
                    memberId = 1L,
                    orderId = null,
                    category = InquiryCategory.PRODUCT,
                    title = "문의 1",
                    content = "내용 1",
                )
            val inquiry2 =
                Inquiry.create(
                    memberId = 1L,
                    orderId = 10L,
                    category = InquiryCategory.DELIVERY,
                    title = "문의 2",
                    content = "내용 2",
                )
            every { inquiryRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L) } returns listOf(inquiry1, inquiry2)

            val responses = inquiryService.findByMemberId(1L)

            Then("2건 반환") {
                responses.size shouldBe 2
            }
        }

        When("문의가 없는 회원 조회") {
            every { inquiryRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(999L) } returns emptyList()

            val responses = inquiryService.findByMemberId(999L)

            Then("빈 목록 반환") {
                responses.size shouldBe 0
            }
        }
    }

    Given("답변 작성") {

        When("PENDING 상태 문의에 답변 작성") {
            val inquiry =
                Inquiry.create(
                    memberId = 1L,
                    orderId = null,
                    category = InquiryCategory.PRODUCT,
                    title = "사이즈 문의",
                    content = "M사이즈 실측 치수 알려주세요.",
                )
            every { inquiryRepository.findById(1L) } returns Optional.of(inquiry)
            every { inquiryAnswerRepository.save(any()) } answers { firstArg() }

            val response =
                inquiryService.createAnswer(
                    inquiryId = 1L,
                    adminId = 100L,
                    request =
                        CreateAnswerRequest(
                            content = "M사이즈 기준 가슴단면 55cm, 총장 72cm입니다.",
                        ),
                )

            Then("답변 내용이 설정됨") {
                response.content shouldBe "M사이즈 기준 가슴단면 55cm, 총장 72cm입니다."
            }

            Then("문의 상태가 ANSWERED로 전이") {
                inquiry.status shouldBe InquiryStatus.ANSWERED
            }
        }

        When("CLOSED 상태 문의에 답변 작성") {
            val inquiry =
                Inquiry.create(
                    memberId = 1L,
                    orderId = null,
                    category = InquiryCategory.PRODUCT,
                    title = "닫힌 문의",
                    content = "이미 해결됨",
                )
            inquiry.close()
            every { inquiryRepository.findById(2L) } returns Optional.of(inquiry)

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    inquiryService.createAnswer(
                        inquiryId = 2L,
                        adminId = 100L,
                        request = CreateAnswerRequest(content = "답변 시도"),
                    )
                }
            }
        }
    }

    Given("문의 닫기") {

        When("PENDING 상태 문의 닫기") {
            val inquiry =
                Inquiry.create(
                    memberId = 1L,
                    orderId = null,
                    category = InquiryCategory.PRODUCT,
                    title = "문의",
                    content = "내용",
                )
            every { inquiryRepository.findById(1L) } returns Optional.of(inquiry)

            inquiryService.closeInquiry(1L)

            Then("상태가 CLOSED로 전이") {
                inquiry.status shouldBe InquiryStatus.CLOSED
            }
        }

        When("이미 CLOSED 상태 문의 닫기") {
            val inquiry =
                Inquiry.create(
                    memberId = 1L,
                    orderId = null,
                    category = InquiryCategory.PRODUCT,
                    title = "닫힌 문의",
                    content = "내용",
                )
            inquiry.close()
            every { inquiryRepository.findById(3L) } returns Optional.of(inquiry)

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    inquiryService.closeInquiry(3L)
                }
            }
        }
    }
})
