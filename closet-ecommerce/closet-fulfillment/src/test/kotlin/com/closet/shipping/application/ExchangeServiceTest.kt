package com.closet.shipping.application

import com.closet.common.exception.BusinessException
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.common.vo.Money
import com.closet.shipping.domain.ExchangeRequest
import com.closet.shipping.domain.ExchangeRequestRepository
import com.closet.shipping.domain.ExchangeStatus
import com.closet.shipping.domain.ReturnReason
import com.closet.shipping.domain.Shipment
import com.closet.shipping.domain.ShipmentRepository
import com.closet.shipping.domain.ShippingFeePolicy
import com.closet.shipping.domain.ShippingFeePolicyRepository
import com.closet.shipping.domain.ShippingStatus
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime
import java.util.Optional

class ExchangeServiceTest : BehaviorSpec({

    val exchangeRequestRepository = mockk<ExchangeRequestRepository>(relaxed = true)
    val shipmentRepository = mockk<ShipmentRepository>()
    val shippingFeePolicyRepository = mockk<ShippingFeePolicyRepository>()
    val outboxEventPublisher = mockk<OutboxEventPublisher>(relaxed = true)
    val objectMapper = ObjectMapper()

    val exchangeService =
        ExchangeService(
            exchangeRequestRepository,
            shipmentRepository,
            shippingFeePolicyRepository,
            outboxEventPublisher,
            objectMapper,
        )

    Given("교환 신청 - 단순변심") {

        When("CHANGE_OF_MIND 사유로 교환 신청") {
            val shipment = createDeliveredShipment(orderId = 1L)
            every { shipmentRepository.findByOrderId(1L) } returns shipment

            val policy =
                ShippingFeePolicy(
                    type = "EXCHANGE",
                    reason = "CHANGE_OF_MIND",
                    payer = "BUYER",
                    fee = Money.of(6000),
                )
            every {
                shippingFeePolicyRepository.findByTypeAndReasonAndIsActiveTrue(
                    "EXCHANGE",
                    "CHANGE_OF_MIND",
                )
            } returns policy
            every { exchangeRequestRepository.save(any()) } answers { firstArg() }

            val response =
                exchangeService.createExchangeRequest(
                    memberId = 1L,
                    sellerId = 1L,
                    request =
                        CreateExchangeRequest(
                            orderId = 1L,
                            orderItemId = 1L,
                            originalProductOptionId = 100L,
                            newProductOptionId = 200L,
                            quantity = 1,
                            reason = ReturnReason.CHANGE_OF_MIND,
                            reasonDetail = "다른 색상으로 교환 원합니다",
                        ),
                )

            Then("배송비 6,000원 BUYER 부담 (왕복)") {
                response.shippingFee shouldBe 6000L
                response.shippingFeePayer shouldBe "BUYER"
            }

            Then("상태는 REQUESTED") {
                response.status shouldBe "REQUESTED"
            }

            Then("exchange.requested 이벤트 발행됨") {
                verify(exactly = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "ExchangeRequest",
                        aggregateId = any(),
                        eventType = "ExchangeRequested",
                        topic = "event.closet.shipping",
                        partitionKey = any(),
                        payload = any(),
                    )
                }
            }
        }
    }

    Given("교환 신청 - 불량") {

        When("DEFECTIVE 사유로 교환 신청") {
            val shipment = createDeliveredShipment(orderId = 2L)
            every { shipmentRepository.findByOrderId(2L) } returns shipment

            val policy =
                ShippingFeePolicy(
                    type = "EXCHANGE",
                    reason = "DEFECTIVE",
                    payer = "SELLER",
                    fee = Money.ZERO,
                )
            every { shippingFeePolicyRepository.findByTypeAndReasonAndIsActiveTrue("EXCHANGE", "DEFECTIVE") } returns policy
            every { exchangeRequestRepository.save(any()) } answers { firstArg() }

            val response =
                exchangeService.createExchangeRequest(
                    memberId = 1L,
                    sellerId = 1L,
                    request =
                        CreateExchangeRequest(
                            orderId = 2L,
                            orderItemId = 1L,
                            originalProductOptionId = 100L,
                            newProductOptionId = 200L,
                            quantity = 1,
                            reason = ReturnReason.DEFECTIVE,
                        ),
                )

            Then("배송비 0원 SELLER 부담") {
                response.shippingFee shouldBe 0L
                response.shippingFeePayer shouldBe "SELLER"
            }
        }
    }

    Given("교환 신청 - 동일 옵션 거절") {

        When("원본과 동일한 옵션 ID로 교환 신청") {
            val shipment = createDeliveredShipment(orderId = 3L)
            every { shipmentRepository.findByOrderId(3L) } returns shipment

            Then("BusinessException 발생") {
                shouldThrow<BusinessException> {
                    exchangeService.createExchangeRequest(
                        memberId = 1L,
                        sellerId = 1L,
                        request =
                            CreateExchangeRequest(
                                orderId = 3L,
                                orderItemId = 1L,
                                originalProductOptionId = 100L,
                                // 동일 옵션
                                newProductOptionId = 100L,
                                quantity = 1,
                                reason = ReturnReason.CHANGE_OF_MIND,
                            ),
                    )
                }
            }
        }
    }

    Given("교환 신청 - 배송 미완료") {

        When("READY 상태에서 교환 신청") {
            val shipment =
                Shipment.create(
                    orderId = 4L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "홍길동",
                    receiverPhone = "010-1234-5678",
                    zipCode = "06000",
                    address = "서울",
                    detailAddress = "101호",
                )
            every { shipmentRepository.findByOrderId(4L) } returns shipment

            Then("BusinessException 발생 (배송 완료 상태에서만 가능)") {
                shouldThrow<BusinessException> {
                    exchangeService.createExchangeRequest(
                        memberId = 1L,
                        sellerId = 1L,
                        request =
                            CreateExchangeRequest(
                                orderId = 4L,
                                orderItemId = 1L,
                                originalProductOptionId = 100L,
                                newProductOptionId = 200L,
                                quantity = 1,
                                reason = ReturnReason.CHANGE_OF_MIND,
                            ),
                    )
                }
            }
        }
    }

    Given("교환 신청 - 기한 경과") {

        When("배송 완료 후 8일 경과") {
            val shipment = createDeliveredShipment(orderId = 5L, daysAgo = 8)
            every { shipmentRepository.findByOrderId(5L) } returns shipment

            Then("BusinessException 발생 (기한 경과)") {
                shouldThrow<BusinessException> {
                    exchangeService.createExchangeRequest(
                        memberId = 1L,
                        sellerId = 1L,
                        request =
                            CreateExchangeRequest(
                                orderId = 5L,
                                orderItemId = 1L,
                                originalProductOptionId = 100L,
                                newProductOptionId = 200L,
                                quantity = 1,
                                reason = ReturnReason.CHANGE_OF_MIND,
                            ),
                    )
                }
            }
        }
    }

    Given("교환 조회") {

        When("존재하지 않는 ID 조회") {
            every { exchangeRequestRepository.findById(999L) } returns Optional.empty()

            Then("BusinessException 발생") {
                shouldThrow<BusinessException> {
                    exchangeService.findById(999L)
                }
            }
        }
    }

    Given("교환 상태 전이 - 수거 예약") {

        When("REQUESTED 상태에서 수거 예약") {
            val exchangeRequest = createExchangeRequest()
            every { exchangeRequestRepository.findById(1L) } returns Optional.of(exchangeRequest)

            val response = exchangeService.schedulePickup(1L, "CJ1234567890")

            Then("PICKUP_SCHEDULED") {
                response.status shouldBe "PICKUP_SCHEDULED"
            }

            Then("수거 송장번호 설정됨") {
                response.pickupTrackingNumber shouldBe "CJ1234567890"
            }
        }
    }

    Given("교환 수거 완료") {

        When("PICKUP_SCHEDULED 상태에서 수거 완료") {
            val exchangeRequest = createExchangeRequest()
            exchangeRequest.schedulePickup("CJ1234567890")
            every { exchangeRequestRepository.findById(1L) } returns Optional.of(exchangeRequest)

            exchangeService.completePickup(1L)

            Then("PICKUP_COMPLETED") {
                exchangeRequest.status shouldBe ExchangeStatus.PICKUP_COMPLETED
            }

            Then("exchange.pickup.completed 이벤트 발행됨") {
                verify(atLeast = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "ExchangeRequest",
                        aggregateId = any(),
                        eventType = "ExchangePickupCompleted",
                        topic = "event.closet.shipping",
                        partitionKey = any(),
                        payload = any(),
                    )
                }
            }
        }
    }

    Given("교환 거절") {

        When("REQUESTED 상태에서 거절") {
            val exchangeRequest = createExchangeRequest()
            every { exchangeRequestRepository.findById(1L) } returns Optional.of(exchangeRequest)

            exchangeService.reject(1L)

            Then("REJECTED") {
                exchangeRequest.status shouldBe ExchangeStatus.REJECTED
            }

            Then("exchange.rejected 이벤트 발행됨 (새 옵션 재고 해제)") {
                verify(atLeast = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "ExchangeRequest",
                        aggregateId = any(),
                        eventType = "ExchangeRejected",
                        topic = "event.closet.shipping",
                        partitionKey = any(),
                        payload = any(),
                    )
                }
            }
        }
    }

    Given("진행 중인 교환 확인") {

        When("진행 중인 교환이 있는 경우") {
            val activeExchange = createExchangeRequest()
            every {
                exchangeRequestRepository.findByOrderIdAndStatusNotIn(1L, listOf(ExchangeStatus.COMPLETED, ExchangeStatus.REJECTED))
            } returns listOf(activeExchange)

            Then("true 반환") {
                exchangeService.hasActiveExchangeRequest(1L) shouldBe true
            }
        }

        When("진행 중인 교환이 없는 경우") {
            every {
                exchangeRequestRepository.findByOrderIdAndStatusNotIn(2L, listOf(ExchangeStatus.COMPLETED, ExchangeStatus.REJECTED))
            } returns emptyList()

            Then("false 반환") {
                exchangeService.hasActiveExchangeRequest(2L) shouldBe false
            }
        }
    }
})

private fun createDeliveredShipment(
    orderId: Long,
    daysAgo: Int = 1,
): Shipment {
    val shipment =
        Shipment.create(
            orderId = orderId,
            sellerId = 1L,
            memberId = 1L,
            receiverName = "홍길동",
            receiverPhone = "010-1234-5678",
            zipCode = "06000",
            address = "서울시 강남구",
            detailAddress = "101동 201호",
        )
    shipment.updateStatus(ShippingStatus.IN_TRANSIT)
    shipment.updateStatus(ShippingStatus.DELIVERED)

    // deliveredAt을 daysAgo일 전으로 설정
    val deliveredField = Shipment::class.java.getDeclaredField("deliveredAt")
    deliveredField.isAccessible = true
    deliveredField.set(shipment, ZonedDateTime.now().minusDays(daysAgo.toLong()))

    return shipment
}

private fun createExchangeRequest(): ExchangeRequest {
    return ExchangeRequest.create(
        orderId = 1L,
        orderItemId = 1L,
        memberId = 1L,
        sellerId = 1L,
        originalProductOptionId = 100L,
        newProductOptionId = 200L,
        quantity = 1,
        reason = ReturnReason.CHANGE_OF_MIND,
        reasonDetail = "사이즈가 맞지 않습니다",
        shippingFee = Money.of(6000),
        shippingFeePayer = "BUYER",
    )
}
