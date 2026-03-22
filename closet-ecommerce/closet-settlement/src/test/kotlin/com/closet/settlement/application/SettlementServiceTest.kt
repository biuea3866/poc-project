package com.closet.settlement.application

import com.closet.common.exception.BusinessException
import com.closet.settlement.domain.settlement.Settlement
import com.closet.settlement.domain.settlement.SettlementItem
import com.closet.settlement.domain.settlement.SettlementStatus
import com.closet.settlement.presentation.dto.CalculateSettlementItemRequest
import com.closet.settlement.presentation.dto.CalculateSettlementRequest
import com.closet.settlement.repository.SettlementItemRepository
import com.closet.settlement.repository.SettlementRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class SettlementServiceTest : BehaviorSpec({

    val settlementRepository = mockk<SettlementRepository>()
    val settlementItemRepository = mockk<SettlementItemRepository>()
    val commissionRateService = mockk<CommissionRateService>()

    val settlementService = SettlementService(
        settlementRepository = settlementRepository,
        settlementItemRepository = settlementItemRepository,
        commissionRateService = commissionRateService,
    )

    Given("정산 계산") {
        val periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0)
        val periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59)

        val request = CalculateSettlementRequest(
            sellerId = 1L,
            periodFrom = periodFrom,
            periodTo = periodTo,
            items = listOf(
                CalculateSettlementItemRequest(
                    orderId = 100L,
                    orderItemId = 1000L,
                    saleAmount = BigDecimal("50000"),
                    categoryId = 5L,
                ),
                CalculateSettlementItemRequest(
                    orderId = 101L,
                    orderItemId = 1001L,
                    saleAmount = BigDecimal("30000"),
                    categoryId = 5L,
                ),
            ),
            totalRefund = BigDecimal("5000"),
        )

        val settlementSlot = slot<Settlement>()
        val itemSlot = slot<SettlementItem>()

        every { settlementRepository.save(capture(settlementSlot)) } answers { settlementSlot.captured }
        every { settlementItemRepository.save(capture(itemSlot)) } answers { itemSlot.captured }
        every { commissionRateService.getRateValue(5L) } returns BigDecimal("0.15")

        When("정상 정산 계산 요청") {
            val result = settlementService.calculate(request)

            Then("정산 상태가 CALCULATED가 된다") {
                result.status shouldBe SettlementStatus.CALCULATED
            }

            Then("총 매출이 올바르게 계산된다") {
                // 50000 + 30000 = 80000
                result.totalSales shouldBeEqualComparingTo BigDecimal("80000")
            }

            Then("총 수수료가 올바르게 계산된다") {
                // 50000 * 0.15 = 7500, 30000 * 0.15 = 4500 => 12000
                result.totalCommission shouldBeEqualComparingTo BigDecimal("12000.00")
            }

            Then("정산 금액이 올바르게 계산된다") {
                // 80000 - 12000 - 5000 = 63000
                result.netAmount shouldBeEqualComparingTo BigDecimal("63000.00")
            }

            Then("정산 항목이 2개이다") {
                result.items.size shouldBe 2
            }
        }
    }

    Given("정산 확정") {
        When("CALCULATED 상태 정산 확정") {
            val settlement = Settlement.create(
                sellerId = 1L,
                periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
                periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
            )

            val items = listOf(
                SettlementItem.create(
                    settlementId = 0L,
                    orderId = 100L,
                    orderItemId = 1000L,
                    saleAmount = BigDecimal("50000"),
                    commissionRate = BigDecimal("0.15"),
                )
            )
            settlement.calculate(items)

            every { settlementRepository.findById(any()) } returns Optional.of(settlement)
            every { settlementItemRepository.findBySettlementId(any()) } returns items

            val result = settlementService.confirm(settlement.id)

            Then("정산 상태가 CONFIRMED가 된다") {
                result.status shouldBe SettlementStatus.CONFIRMED
            }
        }

        When("PENDING 상태 정산 확정 시도") {
            val settlement = Settlement.create(
                sellerId = 1L,
                periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
                periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
            )

            every { settlementRepository.findById(any()) } returns Optional.of(settlement)

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    settlementService.confirm(settlement.id)
                }
            }
        }
    }

    Given("정산 지급") {
        When("CONFIRMED 상태 정산 지급") {
            val settlement = Settlement.create(
                sellerId = 1L,
                periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
                periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
            )

            val items = listOf(
                SettlementItem.create(
                    settlementId = 0L,
                    orderId = 100L,
                    orderItemId = 1000L,
                    saleAmount = BigDecimal("50000"),
                    commissionRate = BigDecimal("0.15"),
                )
            )
            settlement.calculate(items)
            settlement.confirm()

            every { settlementRepository.findById(any()) } returns Optional.of(settlement)
            every { settlementItemRepository.findBySettlementId(any()) } returns items

            val result = settlementService.pay(settlement.id)

            Then("정산 상태가 PAID가 된다") {
                result.status shouldBe SettlementStatus.PAID
            }
        }

        When("CALCULATED 상태 정산 지급 시도") {
            val settlement = Settlement.create(
                sellerId = 1L,
                periodFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
                periodTo = LocalDateTime.of(2026, 3, 31, 23, 59, 59),
            )

            val items = listOf(
                SettlementItem.create(
                    settlementId = 0L,
                    orderId = 100L,
                    orderItemId = 1000L,
                    saleAmount = BigDecimal("50000"),
                    commissionRate = BigDecimal("0.15"),
                )
            )
            settlement.calculate(items)

            every { settlementRepository.findById(any()) } returns Optional.of(settlement)

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    settlementService.pay(settlement.id)
                }
            }
        }
    }

    Given("존재하지 않는 정산 조회") {
        every { settlementRepository.findById(999L) } returns Optional.empty()

        When("findById 호출") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    settlementService.findById(999L)
                }
            }
        }
    }
})
