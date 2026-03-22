package com.closet.settlement.domain

import com.closet.settlement.domain.settlement.Settlement
import com.closet.settlement.domain.settlement.SettlementItem
import com.closet.settlement.domain.settlement.SettlementStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

class SettlementTest : BehaviorSpec({

    Given("정산 생성") {
        When("정상적인 기간으로 생성") {
            val settlement = Settlement.create(
                sellerId = 1L,
                periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
                periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
            )

            Then("PENDING 상태로 생성된다") {
                settlement.status shouldBe SettlementStatus.PENDING
            }

            Then("금액이 모두 0이다") {
                settlement.totalSales shouldBeEqualComparingTo BigDecimal.ZERO
                settlement.totalCommission shouldBeEqualComparingTo BigDecimal.ZERO
                settlement.totalRefund shouldBeEqualComparingTo BigDecimal.ZERO
                settlement.netAmount shouldBeEqualComparingTo BigDecimal.ZERO
            }
        }

        When("시작일이 종료일보다 이후인 경우") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Settlement.create(
                        sellerId = 1L,
                        periodFrom = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
                        periodTo = LocalDateTime.of(2026, 3, 1, 0, 0),
                    )
                }
            }
        }
    }

    Given("정산 계산") {
        val settlement = Settlement.create(
            sellerId = 1L,
            periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
            periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
        )

        val items = listOf(
            SettlementItem.create(
                orderId = 100L,
                orderItemId = 1000L,
                saleAmount = BigDecimal("50000"),
                commissionRate = BigDecimal("0.15"),
            ),
            SettlementItem.create(
                orderId = 101L,
                orderItemId = 1001L,
                saleAmount = BigDecimal("30000"),
                commissionRate = BigDecimal("0.20"),
            ),
        )

        When("calculate 호출") {
            settlement.calculate(items, BigDecimal("3000"))

            Then("총 매출이 올바르다") {
                // 50000 + 30000 = 80000
                settlement.totalSales shouldBeEqualComparingTo BigDecimal("80000")
            }

            Then("총 수수료가 올바르다") {
                // 50000*0.15 + 30000*0.20 = 7500 + 6000 = 13500
                settlement.totalCommission shouldBeEqualComparingTo BigDecimal("13500.00")
            }

            Then("정산 금액이 올바르다") {
                // 80000 - 13500 - 3000 = 63500
                settlement.netAmount shouldBeEqualComparingTo BigDecimal("63500.00")
            }

            Then("상태가 CALCULATED가 된다") {
                settlement.status shouldBe SettlementStatus.CALCULATED
            }
        }
    }

    Given("정산 상태 전이") {
        When("정상 흐름: PENDING -> CALCULATED -> CONFIRMED -> PAID") {
            val settlement = Settlement.create(
                sellerId = 1L,
                periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
                periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
            )

            val items = listOf(
                SettlementItem.create(
                    orderId = 100L,
                    orderItemId = 1000L,
                    saleAmount = BigDecimal("50000"),
                    commissionRate = BigDecimal("0.15"),
                )
            )

            settlement.calculate(items)
            Then("CALCULATED 상태") {
                settlement.status shouldBe SettlementStatus.CALCULATED
            }

            settlement.confirm()
            Then("CONFIRMED 상태") {
                settlement.status shouldBe SettlementStatus.CONFIRMED
            }

            settlement.pay()
            Then("PAID 상태") {
                settlement.status shouldBe SettlementStatus.PAID
            }
        }

        When("PENDING에서 confirm 시도") {
            val settlement = Settlement.create(
                sellerId = 1L,
                periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
                periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
            )

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    settlement.confirm()
                }
            }
        }

        When("PENDING에서 pay 시도") {
            val settlement = Settlement.create(
                sellerId = 1L,
                periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
                periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
            )

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    settlement.pay()
                }
            }
        }
    }

    Given("수수료 계산") {
        When("수수료율 15%인 항목") {
            val item = SettlementItem.create(
                orderId = 100L,
                orderItemId = 1000L,
                saleAmount = BigDecimal("79800"),
                commissionRate = BigDecimal("0.15"),
            )

            Then("수수료가 올바르게 계산된다") {
                // 79800 * 0.15 = 11970
                item.commissionAmount shouldBeEqualComparingTo BigDecimal("11970.00")
            }
        }

        When("수수료율 30%인 항목") {
            val item = SettlementItem.create(
                orderId = 200L,
                orderItemId = 2000L,
                saleAmount = BigDecimal("100000"),
                commissionRate = BigDecimal("0.30"),
            )

            Then("수수료가 올바르게 계산된다") {
                // 100000 * 0.30 = 30000
                item.commissionAmount shouldBeEqualComparingTo BigDecimal("30000.00")
            }
        }

        When("반올림이 필요한 경우") {
            val item = SettlementItem.create(
                orderId = 300L,
                orderItemId = 3000L,
                saleAmount = BigDecimal("33333"),
                commissionRate = BigDecimal("0.15"),
            )

            Then("소수점 2자리로 반올림된다") {
                // 33333 * 0.15 = 4999.95
                item.commissionAmount shouldBeEqualComparingTo BigDecimal("4999.95")
            }
        }
    }
})
