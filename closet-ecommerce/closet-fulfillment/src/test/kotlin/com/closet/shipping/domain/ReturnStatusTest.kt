package com.closet.shipping.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ReturnStatusTest : BehaviorSpec({

    Given("ReturnStatus 상태 전이") {

        When("REQUESTED -> PICKUP_SCHEDULED") {
            Then("전이 가능") {
                ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.PICKUP_SCHEDULED) shouldBe true
            }
        }

        When("REQUESTED -> REJECTED") {
            Then("전이 가능") {
                ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.REJECTED) shouldBe true
            }
        }

        When("PICKUP_SCHEDULED -> PICKUP_COMPLETED") {
            Then("전이 가능") {
                ReturnStatus.PICKUP_SCHEDULED.canTransitionTo(ReturnStatus.PICKUP_COMPLETED) shouldBe true
            }
        }

        When("PICKUP_COMPLETED -> INSPECTING") {
            Then("전이 가능") {
                ReturnStatus.PICKUP_COMPLETED.canTransitionTo(ReturnStatus.INSPECTING) shouldBe true
            }
        }

        When("INSPECTING -> APPROVED") {
            Then("전이 가능") {
                ReturnStatus.INSPECTING.canTransitionTo(ReturnStatus.APPROVED) shouldBe true
            }
        }

        When("INSPECTING -> REJECTED") {
            Then("전이 가능") {
                ReturnStatus.INSPECTING.canTransitionTo(ReturnStatus.REJECTED) shouldBe true
            }
        }

        When("APPROVED -> COMPLETED") {
            Then("전이 가능") {
                ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.COMPLETED) shouldBe true
            }
        }

        When("REJECTED -> 어디로든") {
            Then("모든 전이 불가 (터미널 상태)") {
                ReturnStatus.entries.forEach { target ->
                    ReturnStatus.REJECTED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("COMPLETED -> 어디로든") {
            Then("모든 전이 불가 (터미널 상태)") {
                ReturnStatus.entries.forEach { target ->
                    ReturnStatus.COMPLETED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("REQUESTED -> APPROVED 직접 전이") {
            Then("전이 불가") {
                ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.APPROVED) shouldBe false
            }
        }
    }

    Given("ReturnStatus isTerminal") {

        When("COMPLETED") {
            Then("터미널 상태") {
                ReturnStatus.COMPLETED.isTerminal() shouldBe true
            }
        }

        When("REJECTED") {
            Then("터미널 상태") {
                ReturnStatus.REJECTED.isTerminal() shouldBe true
            }
        }

        When("REQUESTED") {
            Then("터미널 상태 아님") {
                ReturnStatus.REQUESTED.isTerminal() shouldBe false
            }
        }
    }

    Given("ReturnStatus validateTransitionTo") {

        When("유효하지 않은 전이") {
            Then("IllegalArgumentException 발생") {
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        ReturnStatus.REJECTED.validateTransitionTo(ReturnStatus.APPROVED)
                    }
                exception.message shouldContain "반품 상태를 REJECTED에서 APPROVED(으)로 변경할 수 없습니다"
            }
        }
    }
})
