package com.closet.settlement.domain

import com.closet.settlement.domain.settlement.SettlementStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SettlementStatusTest : BehaviorSpec({

    Given("PENDING 상태") {
        val status = SettlementStatus.PENDING

        When("CALCULATED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(SettlementStatus.CALCULATED) shouldBe true
            }
        }

        When("CONFIRMED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(SettlementStatus.CONFIRMED) shouldBe false
            }
        }

        When("PAID로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(SettlementStatus.PAID) shouldBe false
            }
        }
    }

    Given("CALCULATED 상태") {
        val status = SettlementStatus.CALCULATED

        When("CONFIRMED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(SettlementStatus.CONFIRMED) shouldBe true
            }
        }

        When("PAID로 직접 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(SettlementStatus.PAID) shouldBe false
            }
        }

        When("PENDING으로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(SettlementStatus.PENDING) shouldBe false
            }
        }
    }

    Given("CONFIRMED 상태") {
        val status = SettlementStatus.CONFIRMED

        When("PAID로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(SettlementStatus.PAID) shouldBe true
            }
        }

        When("CALCULATED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(SettlementStatus.CALCULATED) shouldBe false
            }
        }
    }

    Given("PAID 상태 (터미널)") {
        val status = SettlementStatus.PAID

        When("isTerminal 확인") {
            Then("true") {
                status.isTerminal() shouldBe true
            }
        }

        When("어떤 상태로도 전이 시도") {
            Then("모두 전이 불가") {
                SettlementStatus.entries.forEach { target ->
                    status.canTransitionTo(target) shouldBe false
                }
            }
        }
    }

    Given("비터미널 상태") {
        When("PENDING") {
            Then("isTerminal이 false") {
                SettlementStatus.PENDING.isTerminal() shouldBe false
            }
        }
        When("CALCULATED") {
            Then("isTerminal이 false") {
                SettlementStatus.CALCULATED.isTerminal() shouldBe false
            }
        }
        When("CONFIRMED") {
            Then("isTerminal이 false") {
                SettlementStatus.CONFIRMED.isTerminal() shouldBe false
            }
        }
    }
})
