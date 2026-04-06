package com.closet.shipping.domain.cs.inquiry

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class InquiryAnswerTest : BehaviorSpec({

    Given("InquiryAnswer 생성") {

        When("관리자가 답변 작성") {
            val answer =
                InquiryAnswer.create(
                    inquiryId = 1L,
                    adminId = 100L,
                    content = "해당 상품은 M사이즈 기준 가슴단면 55cm입니다.",
                )

            Then("inquiryId가 설정됨") {
                answer.inquiryId shouldBe 1L
            }

            Then("adminId가 설정됨") {
                answer.adminId shouldBe 100L
            }

            Then("답변 내용이 설정됨") {
                answer.content shouldBe "해당 상품은 M사이즈 기준 가슴단면 55cm입니다."
            }
        }
    }

    Given("InquiryAnswer 내용 수정") {

        When("답변 내용을 수정") {
            val answer =
                InquiryAnswer.create(
                    inquiryId = 1L,
                    adminId = 100L,
                    content = "원래 답변 내용",
                )
            answer.updateContent("수정된 답변 내용")

            Then("내용이 변경됨") {
                answer.content shouldBe "수정된 답변 내용"
            }
        }
    }
})
