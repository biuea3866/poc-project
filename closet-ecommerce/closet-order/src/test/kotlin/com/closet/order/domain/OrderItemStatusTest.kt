package com.closet.order.domain

import com.closet.order.domain.order.OrderItemStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class OrderItemStatusTest : BehaviorSpec({

    Given("ORDERED 상태") {
        val status = OrderItemStatus.ORDERED

        When("PREPARING으로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderItemStatus.PREPARING) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderItemStatus.CANCELLED) shouldBe true
            }
        }

        When("DELIVERED로 직접 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderItemStatus.DELIVERED) shouldBe false
            }
        }
    }

    Given("PREPARING 상태") {
        val status = OrderItemStatus.PREPARING

        When("SHIPPED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderItemStatus.SHIPPED) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderItemStatus.CANCELLED) shouldBe true
            }
        }
    }

    Given("SHIPPED 상태") {
        val status = OrderItemStatus.SHIPPED

        When("DELIVERED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderItemStatus.DELIVERED) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderItemStatus.CANCELLED) shouldBe false
            }
        }
    }

    Given("DELIVERED 상태") {
        val status = OrderItemStatus.DELIVERED

        When("RETURN_REQUESTED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderItemStatus.RETURN_REQUESTED) shouldBe true
            }
        }

        When("EXCHANGE_REQUESTED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderItemStatus.EXCHANGE_REQUESTED) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderItemStatus.CANCELLED) shouldBe false
            }
        }
    }

    Given("RETURN_REQUESTED 상태") {
        val status = OrderItemStatus.RETURN_REQUESTED

        When("RETURNED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderItemStatus.RETURNED) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderItemStatus.CANCELLED) shouldBe false
            }
        }
    }

    Given("EXCHANGE_REQUESTED 상태") {
        val status = OrderItemStatus.EXCHANGE_REQUESTED

        When("EXCHANGE_COMPLETED로 전이 시도") {
            Then("전이 가능") {
                status.canTransitionTo(OrderItemStatus.EXCHANGE_COMPLETED) shouldBe true
            }
        }

        When("CANCELLED로 전이 시도") {
            Then("전이 불가") {
                status.canTransitionTo(OrderItemStatus.CANCELLED) shouldBe false
            }
        }
    }

    Given("EXCHANGE_COMPLETED 상태") {
        val status = OrderItemStatus.EXCHANGE_COMPLETED

        When("어떤 상태로든 전이 시도") {
            Then("전이 불가") {
                OrderItemStatus.entries.forEach { target ->
                    status.canTransitionTo(target) shouldBe false
                }
            }
        }
    }

    Given("RETURNED 상태") {
        val status = OrderItemStatus.RETURNED

        When("어떤 상태로든 전이 시도") {
            Then("전이 불가") {
                OrderItemStatus.entries.forEach { target ->
                    status.canTransitionTo(target) shouldBe false
                }
            }
        }
    }

    Given("CANCELLED 상태") {
        val status = OrderItemStatus.CANCELLED

        When("어떤 상태로든 전이 시도") {
            Then("전이 불가") {
                OrderItemStatus.entries.forEach { target ->
                    status.canTransitionTo(target) shouldBe false
                }
            }
        }
    }
})
