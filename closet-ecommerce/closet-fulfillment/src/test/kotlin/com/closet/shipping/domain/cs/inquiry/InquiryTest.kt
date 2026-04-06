package com.closet.shipping.domain.cs.inquiry

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class InquiryTest : BehaviorSpec({

    Given("Inquiry 생성") {

        When("상품 관련 문의 생성") {
            val inquiry =
                createInquiry(
                    category = InquiryCategory.PRODUCT,
                    title = "사이즈 문의",
                    content = "이 상품 M사이즈 실제 치수가 어떻게 되나요?",
                )

            Then("초기 상태는 PENDING") {
                inquiry.status shouldBe InquiryStatus.PENDING
            }

            Then("카테고리는 PRODUCT") {
                inquiry.category shouldBe InquiryCategory.PRODUCT
            }

            Then("제목과 내용이 설정됨") {
                inquiry.title shouldBe "사이즈 문의"
                inquiry.content shouldBe "이 상품 M사이즈 실제 치수가 어떻게 되나요?"
            }
        }

        When("주문 연관 배송 문의 생성") {
            val inquiry =
                createInquiry(
                    category = InquiryCategory.DELIVERY,
                    orderId = 100L,
                    title = "배송이 안 와요",
                    content = "주문한지 3일인데 아직 배송이 안 됩니다.",
                )

            Then("orderId가 설정됨") {
                inquiry.orderId shouldBe 100L
            }

            Then("카테고리는 DELIVERY") {
                inquiry.category shouldBe InquiryCategory.DELIVERY
            }
        }

        When("orderId 없이 일반 문의 생성") {
            val inquiry =
                createInquiry(
                    orderId = null,
                    category = InquiryCategory.ETC,
                    title = "일반 문의",
                    content = "회원 탈퇴는 어떻게 하나요?",
                )

            Then("orderId는 null") {
                inquiry.orderId shouldBe null
            }
        }
    }

    Given("Inquiry 답변 처리 (answer)") {

        When("PENDING 상태에서 answer 호출") {
            val inquiry = createInquiry()
            inquiry.answer()

            Then("상태가 ANSWERED로 전이") {
                inquiry.status shouldBe InquiryStatus.ANSWERED
            }
        }

        When("IN_PROGRESS 상태에서 answer 호출") {
            val inquiry = createInquiry()
            inquiry.markInProgress()
            inquiry.answer()

            Then("상태가 ANSWERED로 전이") {
                inquiry.status shouldBe InquiryStatus.ANSWERED
            }
        }

        When("CLOSED 상태에서 answer 호출") {
            val inquiry = createInquiry()
            inquiry.close()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    inquiry.answer()
                }
            }
        }
    }

    Given("Inquiry 닫기 (close)") {

        When("PENDING 상태에서 close 호출") {
            val inquiry = createInquiry()
            inquiry.close()

            Then("상태가 CLOSED로 전이") {
                inquiry.status shouldBe InquiryStatus.CLOSED
            }
        }

        When("ANSWERED 상태에서 close 호출") {
            val inquiry = createInquiry()
            inquiry.answer()
            inquiry.close()

            Then("상태가 CLOSED로 전이") {
                inquiry.status shouldBe InquiryStatus.CLOSED
            }
        }

        When("이미 CLOSED 상태에서 close 호출") {
            val inquiry = createInquiry()
            inquiry.close()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    inquiry.close()
                }
            }
        }
    }

    Given("Inquiry 진행중 (markInProgress)") {

        When("PENDING 상태에서 markInProgress 호출") {
            val inquiry = createInquiry()
            inquiry.markInProgress()

            Then("상태가 IN_PROGRESS로 전이") {
                inquiry.status shouldBe InquiryStatus.IN_PROGRESS
            }
        }

        When("ANSWERED 상태에서 markInProgress 호출 (추가 문의)") {
            val inquiry = createInquiry()
            inquiry.answer()
            inquiry.markInProgress()

            Then("상태가 IN_PROGRESS로 전이") {
                inquiry.status shouldBe InquiryStatus.IN_PROGRESS
            }
        }
    }

    Given("Inquiry 수정 가능 여부 (isEditable)") {

        When("PENDING 상태") {
            val inquiry = createInquiry()

            Then("수정 가능") {
                inquiry.isEditable() shouldBe true
            }
        }

        When("IN_PROGRESS 상태") {
            val inquiry = createInquiry()
            inquiry.markInProgress()

            Then("수정 불가") {
                inquiry.isEditable() shouldBe false
            }
        }

        When("ANSWERED 상태") {
            val inquiry = createInquiry()
            inquiry.answer()

            Then("수정 불가") {
                inquiry.isEditable() shouldBe false
            }
        }

        When("CLOSED 상태") {
            val inquiry = createInquiry()
            inquiry.close()

            Then("수정 불가") {
                inquiry.isEditable() shouldBe false
            }
        }
    }

    Given("Inquiry 소프트 삭제") {

        When("PENDING 상태에서 delete 호출") {
            val inquiry = createInquiry()
            inquiry.delete()

            Then("deletedAt이 설정됨") {
                inquiry.deletedAt shouldNotBe null
            }
        }

        When("이미 삭제된 문의에 delete 호출") {
            val inquiry = createInquiry()
            inquiry.delete()

            Then("IllegalStateException 발생") {
                shouldThrow<IllegalStateException> {
                    inquiry.delete()
                }
            }
        }

        When("CLOSED 상태에서 delete 호출") {
            val inquiry = createInquiry()
            inquiry.close()
            inquiry.delete()

            Then("deletedAt이 설정됨") {
                inquiry.deletedAt shouldNotBe null
            }
        }
    }
})

private fun createInquiry(
    memberId: Long = 1L,
    orderId: Long? = null,
    category: InquiryCategory = InquiryCategory.PRODUCT,
    title: String = "테스트 문의",
    content: String = "테스트 문의 내용입니다.",
): Inquiry {
    return Inquiry.create(
        memberId = memberId,
        orderId = orderId,
        category = category,
        title = title,
        content = content,
    )
}
