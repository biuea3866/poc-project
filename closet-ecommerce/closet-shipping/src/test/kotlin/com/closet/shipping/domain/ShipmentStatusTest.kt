package com.closet.shipping.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ShipmentStatusTest : BehaviorSpec({

    Given("PENDING 상태") {
        val status = ShipmentStatus.PENDING

        When("READY로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(ShipmentStatus.READY) shouldBe true
            }
        }

        When("PICKED_UP으로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(ShipmentStatus.PICKED_UP) shouldBe false
            }
        }

        When("IN_TRANSIT로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(ShipmentStatus.IN_TRANSIT) shouldBe false
            }
        }

        When("DELIVERED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(ShipmentStatus.DELIVERED) shouldBe false
            }
        }
    }

    Given("READY 상태") {
        val status = ShipmentStatus.READY

        When("PICKED_UP으로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(ShipmentStatus.PICKED_UP) shouldBe true
            }
        }

        When("IN_TRANSIT로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(ShipmentStatus.IN_TRANSIT) shouldBe false
            }
        }
    }

    Given("PICKED_UP 상태") {
        val status = ShipmentStatus.PICKED_UP

        When("IN_TRANSIT로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(ShipmentStatus.IN_TRANSIT) shouldBe true
            }
        }

        When("DELIVERED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(ShipmentStatus.DELIVERED) shouldBe false
            }
        }
    }

    Given("IN_TRANSIT 상태") {
        val status = ShipmentStatus.IN_TRANSIT

        When("OUT_FOR_DELIVERY로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY) shouldBe true
            }
        }

        When("DELIVERED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(ShipmentStatus.DELIVERED) shouldBe false
            }
        }
    }

    Given("OUT_FOR_DELIVERY 상태") {
        val status = ShipmentStatus.OUT_FOR_DELIVERY

        When("DELIVERED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(ShipmentStatus.DELIVERED) shouldBe true
            }
        }

        When("PENDING으로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(ShipmentStatus.PENDING) shouldBe false
            }
        }
    }

    Given("DELIVERED 상태 (터미널)") {
        val status = ShipmentStatus.DELIVERED

        When("isTerminal 확인") {
            Then("true") {
                status.isTerminal() shouldBe true
            }
        }

        When("어떤 상태로도 전이 시도") {
            Then("모두 불가") {
                ShipmentStatus.entries.forEach { target ->
                    status.canTransitionTo(target) shouldBe false
                }
            }
        }
    }

    Given("비터미널 상태") {
        When("PENDING") {
            Then("isTerminal이 false") {
                ShipmentStatus.PENDING.isTerminal() shouldBe false
            }
        }

        When("IN_TRANSIT") {
            Then("isTerminal이 false") {
                ShipmentStatus.IN_TRANSIT.isTerminal() shouldBe false
            }
        }
    }
})
