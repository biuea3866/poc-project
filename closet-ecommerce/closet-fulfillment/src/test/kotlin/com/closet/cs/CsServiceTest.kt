package com.closet.cs

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.cs.application.FaqService
import com.closet.cs.application.InquiryService
import com.closet.cs.domain.Faq
import com.closet.cs.domain.FaqCategory
import com.closet.cs.domain.Inquiry
import com.closet.cs.domain.InquiryCategory
import com.closet.cs.domain.InquiryReply
import com.closet.cs.domain.InquiryStatus
import com.closet.cs.domain.ReplyType
import com.closet.cs.domain.repository.FaqRepository
import com.closet.cs.domain.repository.InquiryReplyRepository
import com.closet.cs.domain.repository.InquiryRepository
import com.closet.cs.presentation.dto.CreateFaqRequest
import com.closet.cs.presentation.dto.CreateInquiryRequest
import com.closet.cs.presentation.dto.CreateReplyRequest
import com.closet.cs.presentation.dto.UpdateFaqRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.ZonedDateTime
import java.util.Optional

class CsServiceTest : BehaviorSpec({

    // --- Inquiry 테스트 ---
    val inquiryRepository = mockk<InquiryRepository>()
    val inquiryReplyRepository = mockk<InquiryReplyRepository>()
    val inquiryService = InquiryService(inquiryRepository, inquiryReplyRepository)

    Given("문의 등록 요청이 주어졌을 때") {
        val memberId = 1L
        val request =
            CreateInquiryRequest(
                orderId = 100L,
                category = InquiryCategory.PRODUCT,
                title = "상품 문의",
                content = "사이즈가 어떻게 되나요?",
            )

        When("정상적으로 등록하면") {
            val inquirySlot = slot<Inquiry>()
            every { inquiryRepository.save(capture(inquirySlot)) } answers {
                inquirySlot.captured.apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            }

            val result = inquiryService.create(memberId, request)

            Then("문의가 OPEN 상태로 생성된다") {
                result.memberId shouldBe memberId
                result.orderId shouldBe 100L
                result.category shouldBe InquiryCategory.PRODUCT
                result.title shouldBe "상품 문의"
                result.status shouldBe InquiryStatus.OPEN
            }
        }
    }

    Given("OPEN 상태의 문의가 있을 때") {
        val inquiry =
            Inquiry.create(
                memberId = 1L,
                orderId = null,
                category = InquiryCategory.SHIPPING,
                title = "배송 문의",
                content = "언제 도착하나요?",
            ).apply {
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
            }

        When("답변을 등록하면") {
            every { inquiryRepository.findById(any()) } returns Optional.of(inquiry)

            val replySlot = slot<InquiryReply>()
            every { inquiryReplyRepository.save(capture(replySlot)) } answers {
                replySlot.captured
            }

            val replyRequest =
                CreateReplyRequest(
                    replyType = ReplyType.ADMIN,
                    content = "내일 도착 예정입니다.",
                )

            val result = inquiryService.addReply(1L, replyRequest)

            Then("답변이 생성되고 상태가 ANSWERED로 변경된다") {
                result.replyType shouldBe ReplyType.ADMIN
                result.content shouldBe "내일 도착 예정입니다."
                inquiry.status shouldBe InquiryStatus.ANSWERED
            }
        }

        When("문의를 닫으면") {
            // Reset to ANSWERED status (from previous test)
            val answeredInquiry =
                Inquiry.create(
                    memberId = 1L,
                    orderId = null,
                    category = InquiryCategory.SHIPPING,
                    title = "배송 문의",
                    content = "언제 도착하나요?",
                ).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                    answer() // OPEN → ANSWERED
                }

            every { inquiryRepository.findById(any()) } returns Optional.of(answeredInquiry)

            inquiryService.close(1L)

            Then("상태가 CLOSED로 변경된다") {
                answeredInquiry.status shouldBe InquiryStatus.CLOSED
            }
        }
    }

    Given("CLOSED 상태의 문의가 있을 때") {
        val closedInquiry =
            Inquiry.create(
                memberId = 1L,
                orderId = null,
                category = InquiryCategory.PAYMENT,
                title = "결제 문의",
                content = "환불 가능한가요?",
            ).apply {
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
                answer()
                close()
            }

        When("다시 닫으려고 하면") {
            every { inquiryRepository.findById(any()) } returns Optional.of(closedInquiry)

            Then("상태 전이 오류가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    inquiryService.close(1L)
                }
            }
        }
    }

    Given("존재하지 않는 문의를 조회할 때") {
        every { inquiryRepository.findById(999L) } returns Optional.empty()

        When("상세 조회하면") {
            Then("ENTITY_NOT_FOUND 예외가 발생한다") {
                val exception =
                    shouldThrow<BusinessException> {
                        inquiryService.findById(999L)
                    }
                exception.errorCode shouldBe ErrorCode.ENTITY_NOT_FOUND
            }
        }
    }

    // --- FAQ 테스트 ---
    val faqRepository = mockk<FaqRepository>()
    val faqService = FaqService(faqRepository)

    Given("FAQ 등록 요청이 주어졌을 때") {
        val request =
            CreateFaqRequest(
                category = FaqCategory.SHIPPING,
                question = "배송은 얼마나 걸리나요?",
                answer = "일반적으로 2~3일 소요됩니다.",
                sortOrder = 1,
            )

        When("정상적으로 등록하면") {
            val faqSlot = slot<Faq>()
            every { faqRepository.save(capture(faqSlot)) } answers {
                faqSlot.captured.apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            }

            val result = faqService.create(request)

            Then("FAQ가 노출 상태로 생성된다") {
                result.category shouldBe FaqCategory.SHIPPING
                result.question shouldBe "배송은 얼마나 걸리나요?"
                result.answer shouldBe "일반적으로 2~3일 소요됩니다."
                result.sortOrder shouldBe 1
                result.isVisible shouldBe true
            }
        }
    }

    Given("기존 FAQ가 있을 때") {
        val existingFaq =
            Faq.create(
                category = FaqCategory.PAYMENT,
                question = "결제 수단은 무엇이 있나요?",
                answer = "신용카드, 체크카드, 계좌이체가 가능합니다.",
                sortOrder = 1,
            ).apply {
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
            }

        When("FAQ를 수정하면") {
            every { faqRepository.findById(any()) } returns Optional.of(existingFaq)

            val updateRequest =
                UpdateFaqRequest(
                    question = "어떤 결제 수단을 사용할 수 있나요?",
                    answer = "신용카드, 체크카드, 계좌이체, 간편결제가 가능합니다.",
                    sortOrder = 2,
                )

            val result = faqService.update(1L, updateRequest)

            Then("내용이 변경된다") {
                result.question shouldBe "어떤 결제 수단을 사용할 수 있나요?"
                result.answer shouldBe "신용카드, 체크카드, 계좌이체, 간편결제가 가능합니다."
                result.sortOrder shouldBe 2
            }
        }

        When("노출 상태를 토글하면") {
            val visibleFaq =
                Faq.create(
                    category = FaqCategory.PAYMENT,
                    question = "결제 수단은?",
                    answer = "카드, 계좌이체",
                    sortOrder = 1,
                ).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }

            every { faqRepository.findById(any()) } returns Optional.of(visibleFaq)

            val beforeToggle = visibleFaq.isVisible
            val result = faqService.toggleVisibility(1L)

            Then("노출 상태가 반전된다") {
                result.isVisible shouldBe !beforeToggle
            }
        }
    }

    Given("카테고리별 FAQ 조회 시") {
        val faqs =
            listOf(
                Faq.create(FaqCategory.SHIPPING, "Q1", "A1", 1).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                },
                Faq.create(FaqCategory.SHIPPING, "Q2", "A2", 2).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                },
            )

        When("SHIPPING 카테고리로 조회하면") {
            every { faqRepository.findByCategoryAndIsVisibleTrueOrderBySortOrderAsc(FaqCategory.SHIPPING) } returns faqs

            val result = faqService.findByCategory(FaqCategory.SHIPPING)

            Then("해당 카테고리의 FAQ만 반환된다") {
                result.size shouldBe 2
                result[0].question shouldBe "Q1"
                result[1].question shouldBe "Q2"
            }
        }
    }

    // --- InquiryStatus 상태 전이 테스트 ---
    Given("InquiryStatus 상태 전이 규칙") {
        When("OPEN에서 ANSWERED로 전이하면") {
            Then("허용된다") {
                InquiryStatus.OPEN.canTransitionTo(InquiryStatus.ANSWERED) shouldBe true
            }
        }

        When("OPEN에서 CLOSED로 전이하면") {
            Then("허용된다") {
                InquiryStatus.OPEN.canTransitionTo(InquiryStatus.CLOSED) shouldBe true
            }
        }

        When("ANSWERED에서 CLOSED로 전이하면") {
            Then("허용된다") {
                InquiryStatus.ANSWERED.canTransitionTo(InquiryStatus.CLOSED) shouldBe true
            }
        }

        When("ANSWERED에서 OPEN으로 전이하면") {
            Then("거부된다") {
                InquiryStatus.ANSWERED.canTransitionTo(InquiryStatus.OPEN) shouldBe false
            }
        }

        When("CLOSED에서 다른 상태로 전이하면") {
            Then("모두 거부된다") {
                InquiryStatus.CLOSED.canTransitionTo(InquiryStatus.OPEN) shouldBe false
                InquiryStatus.CLOSED.canTransitionTo(InquiryStatus.ANSWERED) shouldBe false
            }
        }
    }
})
