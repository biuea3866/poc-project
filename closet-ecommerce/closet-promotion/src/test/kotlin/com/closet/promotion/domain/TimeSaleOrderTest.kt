package com.closet.promotion.domain

import com.closet.promotion.domain.timesale.TimeSaleOrder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class TimeSaleOrderTest : BehaviorSpec({

    Given("타임세일 주문 생성") {
        When("정상적인 값으로 생성") {
            val order =
                TimeSaleOrder.create(
                    timeSaleId = 1L,
                    orderId = 100L,
                    memberId = 10L,
                    quantity = 2,
                )

            Then("정상적으로 생성된다") {
                order.timeSaleId shouldBe 1L
                order.orderId shouldBe 100L
                order.memberId shouldBe 10L
                order.quantity shouldBe 2
            }
        }

        When("수량이 0 이하일 때") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    TimeSaleOrder.create(
                        timeSaleId = 1L,
                        orderId = 100L,
                        memberId = 10L,
                        quantity = 0,
                    )
                }
            }
        }

        When("음수 수량일 때") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    TimeSaleOrder.create(
                        timeSaleId = 1L,
                        orderId = 100L,
                        memberId = 10L,
                        quantity = -1,
                    )
                }
            }
        }
    }
})
