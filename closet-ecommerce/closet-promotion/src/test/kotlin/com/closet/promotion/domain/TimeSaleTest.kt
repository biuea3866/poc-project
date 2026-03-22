package com.closet.promotion.domain

import com.closet.common.exception.BusinessException
import com.closet.promotion.domain.timesale.TimeSale
import com.closet.promotion.domain.timesale.TimeSaleStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

class TimeSaleTest : BehaviorSpec({

    Given("타임세일") {
        val timeSale = TimeSale.create(
            productId = 1L,
            salePrice = BigDecimal("29900"),
            limitQuantity = 3,
            startAt = LocalDateTime.now().minusHours(1),
            endAt = LocalDateTime.now().plusHours(5),
        )

        When("타임세일 시작") {
            timeSale.start()

            Then("상태가 ACTIVE로 변경된다") {
                timeSale.status shouldBe TimeSaleStatus.ACTIVE
            }
        }

        When("구매") {
            timeSale.purchase()

            Then("soldCount가 증가한다") {
                timeSale.soldCount shouldBe 1
            }
        }

        When("수량 소진") {
            timeSale.purchase() // 2번째
            timeSale.purchase() // 3번째 → limitQuantity(3) 도달

            Then("상태가 EXHAUSTED로 변경된다") {
                timeSale.status shouldBe TimeSaleStatus.EXHAUSTED
            }

            Then("추가 구매 시 BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    timeSale.purchase()
                }
            }
        }
    }

    Given("TimeSaleStatus 상태 전이") {
        When("SCHEDULED에서 ACTIVE로 전이") {
            Then("전이 가능") {
                TimeSaleStatus.SCHEDULED.canTransitionTo(TimeSaleStatus.ACTIVE) shouldBe true
            }
        }

        When("ACTIVE에서 ENDED로 전이") {
            Then("전이 가능") {
                TimeSaleStatus.ACTIVE.canTransitionTo(TimeSaleStatus.ENDED) shouldBe true
            }
        }

        When("ACTIVE에서 EXHAUSTED로 전이") {
            Then("전이 가능") {
                TimeSaleStatus.ACTIVE.canTransitionTo(TimeSaleStatus.EXHAUSTED) shouldBe true
            }
        }

        When("ENDED에서 전이 시도") {
            Then("전이 불가") {
                TimeSaleStatus.entries.forEach { target ->
                    TimeSaleStatus.ENDED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("터미널 상태") {
            Then("ENDED는 터미널") {
                TimeSaleStatus.ENDED.isTerminal() shouldBe true
            }
            Then("EXHAUSTED는 터미널") {
                TimeSaleStatus.EXHAUSTED.isTerminal() shouldBe true
            }
            Then("ACTIVE는 터미널이 아님") {
                TimeSaleStatus.ACTIVE.isTerminal() shouldBe false
            }
        }
    }

    Given("SCHEDULED 상태에서 구매 시도") {
        val timeSale = TimeSale.create(
            productId = 2L,
            salePrice = BigDecimal("19900"),
            limitQuantity = 10,
            startAt = LocalDateTime.now().plusHours(1),
            endAt = LocalDateTime.now().plusHours(5),
        )

        When("시작 전 구매 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    timeSale.purchase()
                }
            }
        }
    }
})
