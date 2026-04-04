package com.closet.order.domain

import com.closet.order.domain.order.OrderStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class OrderStatusTest : BehaviorSpec({

    Given("PENDING 상태") {
        val status = OrderStatus.PENDING

        When("STOCK_RESERVED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.STOCK_RESERVED) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.CANCELLED) shouldBe true
            }
        }

        When("FAILED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.FAILED) shouldBe true
            }
        }

        When("PAID로 직접 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderStatus.PAID) shouldBe false
            }
        }

        When("DELIVERED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderStatus.DELIVERED) shouldBe false
            }
        }
    }

    Given("STOCK_RESERVED 상태") {
        val status = OrderStatus.STOCK_RESERVED

        When("PAID로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.PAID) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.CANCELLED) shouldBe true
            }
        }
    }

    Given("PAID 상태") {
        val status = OrderStatus.PAID

        When("PREPARING으로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.PREPARING) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.CANCELLED) shouldBe true
            }
        }

        When("SHIPPED로 직접 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderStatus.SHIPPED) shouldBe false
            }
        }
    }

    Given("PREPARING 상태") {
        val status = OrderStatus.PREPARING

        When("SHIPPED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.SHIPPED) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.CANCELLED) shouldBe true
            }
        }
    }

    Given("SHIPPED 상태") {
        val status = OrderStatus.SHIPPED

        When("DELIVERED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.DELIVERED) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderStatus.CANCELLED) shouldBe false
            }
        }
    }

    Given("DELIVERED 상태") {
        val status = OrderStatus.DELIVERED

        When("CONFIRMED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.CONFIRMED) shouldBe true
            }
        }

        When("RETURN_REQUESTED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.RETURN_REQUESTED) shouldBe true
            }
        }

        When("EXCHANGE_REQUESTED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderStatus.EXCHANGE_REQUESTED) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderStatus.CANCELLED) shouldBe false
            }
        }
    }

    Given("RETURN_REQUESTED 상태") {
        val status = OrderStatus.RETURN_REQUESTED

        When("어떤 상태로든 전이 시도") {
            Then("전이 불가") {
                OrderStatus.entries.forEach { target ->
                    status.canTransitionTo(target) shouldBe false
                }
            }
        }
    }

    Given("EXCHANGE_REQUESTED 상태") {
        val status = OrderStatus.EXCHANGE_REQUESTED

        When("어떤 상태로든 전이 시도") {
            Then("전이 불가") {
                OrderStatus.entries.forEach { target ->
                    status.canTransitionTo(target) shouldBe false
                }
            }
        }
    }

    Given("터미널 상태") {
        When("CONFIRMED") {
            Then("isTerminal이 true") {
                OrderStatus.CONFIRMED.isTerminal() shouldBe true
            }
            Then("어떤 상태로도 전이 불가") {
                OrderStatus.entries.forEach { target ->
                    OrderStatus.CONFIRMED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("CANCELLED") {
            Then("isTerminal이 true") {
                OrderStatus.CANCELLED.isTerminal() shouldBe true
            }
        }

        When("FAILED") {
            Then("isTerminal이 true") {
                OrderStatus.FAILED.isTerminal() shouldBe true
            }
        }

        When("PENDING") {
            Then("isTerminal이 false") {
                OrderStatus.PENDING.isTerminal() shouldBe false
            }
        }
    }
})
