package com.closet.shipping.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ExchangeStatusTest : BehaviorSpec({

    Given("ExchangeStatus 상태 전이") {

        When("REQUESTED -> PICKUP_SCHEDULED") {
            Then("전이 가능") {
                ExchangeStatus.REQUESTED.canTransitionTo(ExchangeStatus.PICKUP_SCHEDULED) shouldBe true
            }
        }

        When("REQUESTED -> REJECTED") {
            Then("전이 가능") {
                ExchangeStatus.REQUESTED.canTransitionTo(ExchangeStatus.REJECTED) shouldBe true
            }
        }

        When("PICKUP_SCHEDULED -> PICKUP_COMPLETED") {
            Then("전이 가능") {
                ExchangeStatus.PICKUP_SCHEDULED.canTransitionTo(ExchangeStatus.PICKUP_COMPLETED) shouldBe true
            }
        }

        When("PICKUP_COMPLETED -> RESHIPPING") {
            Then("전이 가능") {
                ExchangeStatus.PICKUP_COMPLETED.canTransitionTo(ExchangeStatus.RESHIPPING) shouldBe true
            }
        }

        When("RESHIPPING -> COMPLETED") {
            Then("전이 가능") {
                ExchangeStatus.RESHIPPING.canTransitionTo(ExchangeStatus.COMPLETED) shouldBe true
            }
        }

        When("REJECTED -> 어디로든") {
            Then("모든 전이 불가 (터미널 상태)") {
                ExchangeStatus.entries.forEach { target ->
                    ExchangeStatus.REJECTED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("COMPLETED -> 어디로든") {
            Then("모든 전이 불가 (터미널 상태)") {
                ExchangeStatus.entries.forEach { target ->
                    ExchangeStatus.COMPLETED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("REQUESTED -> COMPLETED 직접 전이") {
            Then("전이 불가") {
                ExchangeStatus.REQUESTED.canTransitionTo(ExchangeStatus.COMPLETED) shouldBe false
            }
        }

        When("REQUESTED -> RESHIPPING 직접 전이") {
            Then("전이 불가") {
                ExchangeStatus.REQUESTED.canTransitionTo(ExchangeStatus.RESHIPPING) shouldBe false
            }
        }
    }

    Given("ExchangeStatus isTerminal") {

        When("COMPLETED") {
            Then("터미널 상태") {
                ExchangeStatus.COMPLETED.isTerminal() shouldBe true
            }
        }

        When("REJECTED") {
            Then("터미널 상태") {
                ExchangeStatus.REJECTED.isTerminal() shouldBe true
            }
        }

        When("REQUESTED") {
            Then("터미널 상태 아님") {
                ExchangeStatus.REQUESTED.isTerminal() shouldBe false
            }
        }

        When("RESHIPPING") {
            Then("터미널 상태 아님") {
                ExchangeStatus.RESHIPPING.isTerminal() shouldBe false
            }
        }
    }

    Given("ExchangeStatus validateTransitionTo") {

        When("유효하지 않은 전이") {
            Then("IllegalArgumentException 발생") {
                val exception = shouldThrow<IllegalArgumentException> {
                    ExchangeStatus.REJECTED.validateTransitionTo(ExchangeStatus.COMPLETED)
                }
                exception.message shouldContain "교환 상태를 REJECTED에서 COMPLETED(으)로 변경할 수 없습니다"
            }
        }
    }
})
