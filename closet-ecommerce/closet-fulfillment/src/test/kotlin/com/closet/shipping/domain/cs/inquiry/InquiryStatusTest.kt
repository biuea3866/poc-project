package com.closet.shipping.domain.cs.inquiry

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class InquiryStatusTest : BehaviorSpec({

    Given("InquiryStatus 상태 전이") {

        When("PENDING -> IN_PROGRESS") {
            Then("전이 가능") {
                InquiryStatus.PENDING.canTransitionTo(InquiryStatus.IN_PROGRESS) shouldBe true
            }
        }

        When("PENDING -> ANSWERED") {
            Then("전이 가능") {
                InquiryStatus.PENDING.canTransitionTo(InquiryStatus.ANSWERED) shouldBe true
            }
        }

        When("PENDING -> CLOSED") {
            Then("전이 가능 (사용자가 직접 닫기)") {
                InquiryStatus.PENDING.canTransitionTo(InquiryStatus.CLOSED) shouldBe true
            }
        }

        When("IN_PROGRESS -> ANSWERED") {
            Then("전이 가능") {
                InquiryStatus.IN_PROGRESS.canTransitionTo(InquiryStatus.ANSWERED) shouldBe true
            }
        }

        When("IN_PROGRESS -> CLOSED") {
            Then("전이 가능") {
                InquiryStatus.IN_PROGRESS.canTransitionTo(InquiryStatus.CLOSED) shouldBe true
            }
        }

        When("ANSWERED -> CLOSED") {
            Then("전이 가능") {
                InquiryStatus.ANSWERED.canTransitionTo(InquiryStatus.CLOSED) shouldBe true
            }
        }

        When("ANSWERED -> IN_PROGRESS") {
            Then("전이 가능 (추가 문의 시 재진행)") {
                InquiryStatus.ANSWERED.canTransitionTo(InquiryStatus.IN_PROGRESS) shouldBe true
            }
        }

        When("CLOSED -> 어디로든") {
            Then("모든 전이 불가 (터미널 상태)") {
                InquiryStatus.entries.forEach { target ->
                    InquiryStatus.CLOSED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("PENDING -> PENDING") {
            Then("자기 자신으로 전이 불가") {
                InquiryStatus.PENDING.canTransitionTo(InquiryStatus.PENDING) shouldBe false
            }
        }

        When("ANSWERED -> PENDING") {
            Then("전이 불가") {
                InquiryStatus.ANSWERED.canTransitionTo(InquiryStatus.PENDING) shouldBe false
            }
        }
    }

    Given("InquiryStatus isTerminal") {

        When("CLOSED") {
            Then("터미널 상태") {
                InquiryStatus.CLOSED.isTerminal() shouldBe true
            }
        }

        When("PENDING") {
            Then("터미널 상태 아님") {
                InquiryStatus.PENDING.isTerminal() shouldBe false
            }
        }

        When("IN_PROGRESS") {
            Then("터미널 상태 아님") {
                InquiryStatus.IN_PROGRESS.isTerminal() shouldBe false
            }
        }

        When("ANSWERED") {
            Then("터미널 상태 아님") {
                InquiryStatus.ANSWERED.isTerminal() shouldBe false
            }
        }
    }

    Given("InquiryStatus validateTransitionTo") {

        When("유효하지 않은 전이") {
            Then("IllegalArgumentException 발생") {
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        InquiryStatus.CLOSED.validateTransitionTo(InquiryStatus.PENDING)
                    }
                exception.message shouldContain "문의 상태를 CLOSED에서 PENDING(으)로 변경할 수 없습니다"
            }
        }

        When("유효한 전이") {
            Then("예외 없이 통과") {
                InquiryStatus.PENDING.validateTransitionTo(InquiryStatus.IN_PROGRESS)
            }
        }
    }
})
