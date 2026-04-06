package com.closet.promotion.application

import com.closet.common.exception.BusinessException
import com.closet.promotion.domain.timesale.TimeSale
import com.closet.promotion.domain.timesale.TimeSaleOrder
import com.closet.promotion.presentation.dto.PurchaseTimeSaleRequest
import com.closet.promotion.repository.TimeSaleOrderRepository
import com.closet.promotion.repository.TimeSaleRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Optional

class TimeSaleFacadeTest : BehaviorSpec({

    val timeSaleRepository = mockk<TimeSaleRepository>()
    val timeSaleOrderRepository = mockk<TimeSaleOrderRepository>()
    val timeSaleFacade =
        TimeSaleFacade(
            timeSaleRepository = timeSaleRepository,
            timeSaleOrderRepository = timeSaleOrderRepository,
        )

    Given("타임세일 구매 (주문 기록 포함)") {
        val timeSale =
            TimeSale.create(
                productId = 1L,
                salePrice = BigDecimal("29900"),
                limitQuantity = 5,
                startAt = ZonedDateTime.now().minusHours(1),
                endAt = ZonedDateTime.now().plusHours(5),
            )
        timeSale.start()

        every { timeSaleRepository.findById(1L) } returns Optional.of(timeSale)

        When("정상 구매 요청 (수량 1)") {
            val request =
                PurchaseTimeSaleRequest(
                    orderId = 100L,
                    memberId = 10L,
                    quantity = 1,
                )

            val orderSlot = slot<TimeSaleOrder>()
            every { timeSaleOrderRepository.save(capture(orderSlot)) } answers { orderSlot.captured }

            val response = timeSaleFacade.purchaseWithOrder(1L, request)

            Then("주문이 생성되고 soldCount가 증가한다") {
                response.orderId shouldBe 100L
                response.memberId shouldBe 10L
                response.quantity shouldBe 1
                timeSale.soldCount shouldBe 1
            }
        }

        When("수량 2개 구매") {
            // reset
            val timeSale2 =
                TimeSale.create(
                    productId = 2L,
                    salePrice = BigDecimal("19900"),
                    limitQuantity = 10,
                    startAt = ZonedDateTime.now().minusHours(1),
                    endAt = ZonedDateTime.now().plusHours(5),
                )
            timeSale2.start()

            every { timeSaleRepository.findById(2L) } returns Optional.of(timeSale2)

            val request =
                PurchaseTimeSaleRequest(
                    orderId = 101L,
                    memberId = 11L,
                    quantity = 2,
                )

            val orderSlot = slot<TimeSaleOrder>()
            every { timeSaleOrderRepository.save(capture(orderSlot)) } answers { orderSlot.captured }

            val response = timeSaleFacade.purchaseWithOrder(2L, request)

            Then("soldCount가 2 증가한다") {
                response.quantity shouldBe 2
                timeSale2.soldCount shouldBe 2
            }
        }

        When("SCHEDULED 상태에서 구매 시도") {
            val scheduledTimeSale =
                TimeSale.create(
                    productId = 3L,
                    salePrice = BigDecimal("9900"),
                    limitQuantity = 10,
                    startAt = ZonedDateTime.now().plusHours(1),
                    endAt = ZonedDateTime.now().plusHours(5),
                )

            every { timeSaleRepository.findById(3L) } returns Optional.of(scheduledTimeSale)

            val request =
                PurchaseTimeSaleRequest(
                    orderId = 102L,
                    memberId = 12L,
                    quantity = 1,
                )

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    timeSaleFacade.purchaseWithOrder(3L, request)
                }
            }
        }
    }

    Given("타임세일 주문 목록 조회") {
        val orders =
            listOf(
                TimeSaleOrder.create(
                    timeSaleId = 1L,
                    orderId = 100L,
                    memberId = 10L,
                    quantity = 1,
                ),
                TimeSaleOrder.create(
                    timeSaleId = 1L,
                    orderId = 101L,
                    memberId = 11L,
                    quantity = 2,
                ),
            )

        every { timeSaleOrderRepository.findByTimeSaleId(1L) } returns orders

        When("타임세일 ID로 주문 목록 조회") {
            val response = timeSaleFacade.getOrdersByTimeSale(1L)

            Then("해당 타임세일의 주문 목록이 반환된다") {
                response.size shouldBe 2
                response[0].orderId shouldBe 100L
                response[1].orderId shouldBe 101L
            }
        }
    }

    Given("회원별 타임세일 주문 조회") {
        val orders =
            listOf(
                TimeSaleOrder.create(
                    timeSaleId = 1L,
                    orderId = 100L,
                    memberId = 10L,
                    quantity = 1,
                ),
            )

        every { timeSaleOrderRepository.findByMemberId(10L) } returns orders

        When("회원 ID로 주문 목록 조회") {
            val response = timeSaleFacade.getOrdersByMember(10L)

            Then("해당 회원의 주문 목록이 반환된다") {
                response.size shouldBe 1
                response[0].memberId shouldBe 10L
            }
        }
    }
})
