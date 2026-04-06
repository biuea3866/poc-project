package com.closet.shipping.domain.cs.inquiry

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class InquiryAttachmentTest : BehaviorSpec({

    Given("InquiryAttachment 생성") {

        When("이미지 첨부파일 생성") {
            val attachment =
                InquiryAttachment.create(
                    inquiryId = 1L,
                    fileUrl = "https://s3.amazonaws.com/closet/inquiry/1/image.png",
                    fileName = "image.png",
                    fileSize = 1024L,
                )

            Then("inquiryId가 설정됨") {
                attachment.inquiryId shouldBe 1L
            }

            Then("fileUrl이 설정됨") {
                attachment.fileUrl shouldBe "https://s3.amazonaws.com/closet/inquiry/1/image.png"
            }

            Then("fileName이 설정됨") {
                attachment.fileName shouldBe "image.png"
            }

            Then("fileSize가 설정됨") {
                attachment.fileSize shouldBe 1024L
            }
        }
    }
})
