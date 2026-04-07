package com.closet.shipping.application

import com.closet.common.exception.BusinessException
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.common.vo.Money
import com.closet.shipping.domain.ReturnReason
import com.closet.shipping.domain.ReturnRequest
import com.closet.shipping.domain.ReturnRequestRepository
import com.closet.shipping.domain.ReturnStatus
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

class ReturnServiceTest : BehaviorSpec({

    val returnRequestRepository = mockk<ReturnRequestRepository>(relaxed = true)
    val shipmentRepository = mockk<ShipmentRepository>()
    val shippingFeePolicyRepository = mockk<ShippingFeePolicyRepository>()
    val outboxEventPublisher = mockk<OutboxEventPublisher>(relaxed = true)
    val objectMapper = ObjectMapper()

    val returnService =
        ReturnService(
            returnRequestRepository,
            shipmentRepository,
            shippingFeePolicyRepository,
            outboxEventPublisher,
            objectMapper,
        )

    Given("반품 신청 - 단순변심") {

        When("CHANGE_OF_MIND 사유로 반품 신청") {
            val deliveredShipment = createDeliveredShipment(orderId = 1L)
            every { shipmentRepository.findByOrderId(1L) } returns deliveredShipment

            val policy =
                ShippingFeePolicy(
                    type = "RETURN",
                    reason = "CHANGE_OF_MIND",
                    payer = "BUYER",
                    fee = Money.of(3000),
                )
            every { shippingFeePolicyRepository.findByTypeAndReasonAndIsActiveTrue("RETURN", "CHANGE_OF_MIND") } returns policy
            every { returnRequestRepository.save(any()) } answers { firstArg() }

            val response =
                returnService.createReturnRequest(
                    memberId = 1L,
                    sellerId = 1L,
                    request =
                        CreateReturnRequest(
                            orderId = 1L,
                            orderItemId = 1L,
                            productOptionId = 1L,
                            quantity = 1,
                            reason = ReturnReason.CHANGE_OF_MIND,
                            reasonDetail = "마음이 바뀌었습니다",
                            paymentAmount = 50000L,
                        ),
                )

            Then("배송비 3,000원 BUYER 부담") {
                response.shippingFee shouldBe 3000L
                response.shippingFeePayer shouldBe "BUYER"
            }

            Then("환불금액 47,000원 (50,000 - 3,000)") {
                response.refundAmount shouldBe 47000L
            }

            Then("상태는 REQUESTED") {
                response.status shouldBe "REQUESTED"
            }
        }
    }

    Given("반품 신청 - 불량") {

        When("DEFECTIVE 사유로 반품 신청") {
            val deliveredShipment = createDeliveredShipment(orderId = 1L)
            every { shipmentRepository.findByOrderId(1L) } returns deliveredShipment

            val policy =
                ShippingFeePolicy(
                    type = "RETURN",
                    reason = "DEFECTIVE",
                    payer = "SELLER",
                    fee = Money.ZERO,
                )
            every { shippingFeePolicyRepository.findByTypeAndReasonAndIsActiveTrue("RETURN", "DEFECTIVE") } returns policy
            every { returnRequestRepository.save(any()) } answers { firstArg() }

            val response =
                returnService.createReturnRequest(
                    memberId = 1L,
                    sellerId = 1L,
                    request =
                        CreateReturnRequest(
                            orderId = 1L,
                            orderItemId = 1L,
                            productOptionId = 1L,
                            quantity = 1,
                            reason = ReturnReason.DEFECTIVE,
                            paymentAmount = 50000L,
                        ),
                )

            Then("배송비 0원 SELLER 부담") {
                response.shippingFee shouldBe 0L
                response.shippingFeePayer shouldBe "SELLER"
            }

            Then("환불금액 전액 50,000원") {
                response.refundAmount shouldBe 50000L
            }
        }
    }

    Given("반품 승인") {

        When("INSPECTING 상태에서 approve 호출") {
            val returnRequest =
                ReturnRequest.create(
                    orderId = 1L, orderItemId = 1L,
                    memberId = 1L, sellerId = 1L,
                    productOptionId = 1L, quantity = 1,
                    reason = ReturnReason.CHANGE_OF_MIND,
                    reasonDetail = null,
                    shippingFee = Money.of(3000),
                    shippingFeePayer = "BUYER",
                    refundAmount = Money.of(47000),
                )
            // 상태를 INSPECTING으로 전이
            returnRequest.schedulePickup(null)
            returnRequest.completePickup()
            returnRequest.startInspection()

            every { returnRequestRepository.findById(1L) } returns Optional.of(returnRequest)

            returnService.approve(1L)

            Then("상태가 APPROVED") {
                returnRequest.status shouldBe ReturnStatus.APPROVED
            }

            Then("event.closet.shipping 토픽으로 ReturnApproved 이벤트 발행됨") {
                verify(exactly = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "ReturnRequest",
                        aggregateId = any(),
                        eventType = "ReturnApproved",
                        topic = "event.closet.shipping",
                        partitionKey = any(),
                        payload = any(),
                    )
                }
            }
        }
    }

    Given("반품 조회") {

        When("존재하지 않는 ID 조회") {
            every { returnRequestRepository.findById(999L) } returns Optional.empty()

            Then("BusinessException 발생") {
                shouldThrow<BusinessException> {
                    returnService.findById(999L)
                }
            }
        }
    }

    Given("진행 중인 반품 확인") {

        When("진행 중인 반품이 있는 경우") {
            val activeReturn =
                ReturnRequest.create(
                    orderId = 1L, orderItemId = 1L,
                    memberId = 1L, sellerId = 1L,
                    productOptionId = 1L, quantity = 1,
                    reason = ReturnReason.CHANGE_OF_MIND,
                    reasonDetail = null,
                    shippingFee = Money.of(3000),
                    shippingFeePayer = "BUYER",
                    refundAmount = Money.of(47000),
                )
            every {
                returnRequestRepository.findByOrderIdAndStatusNotIn(1L, listOf(ReturnStatus.COMPLETED, ReturnStatus.REJECTED))
            } returns listOf(activeReturn)

            Then("true 반환") {
                returnService.hasActiveReturnRequest(1L) shouldBe true
            }
        }

        When("진행 중인 반품이 없는 경우") {
            every {
                returnRequestRepository.findByOrderIdAndStatusNotIn(2L, listOf(ReturnStatus.COMPLETED, ReturnStatus.REJECTED))
            } returns emptyList()

            Then("false 반환") {
                returnService.hasActiveReturnRequest(2L) shouldBe false
            }
        }
    }

    Given("반품 신청 - 배송 미완료") {

        When("READY 상태에서 반품 신청") {
            val shipment =
                Shipment.create(
                    orderId = 100L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "홍길동",
                    receiverPhone = "010-1234-5678",
                    zipCode = "06000",
                    address = "서울",
                    detailAddress = "101호",
                )
            every { shipmentRepository.findByOrderId(100L) } returns shipment

            Then("BusinessException 발생 (배송 완료 상태에서만 가능)") {
                shouldThrow<BusinessException> {
                    returnService.createReturnRequest(
                        memberId = 1L,
                        sellerId = 1L,
                        request =
                            CreateReturnRequest(
                                orderId = 100L,
                                orderItemId = 1L,
                                productOptionId = 1L,
                                quantity = 1,
                                reason = ReturnReason.CHANGE_OF_MIND,
                                paymentAmount = 50000L,
                            ),
                    )
                }
            }
        }
    }

    Given("반품 신청 - 기한 경과") {

        When("배송 완료 후 8일 경과") {
            val shipment = createDeliveredShipment(orderId = 101L, daysAgo = 8)
            every { shipmentRepository.findByOrderId(101L) } returns shipment

            Then("BusinessException 발생 (기한 경과)") {
                shouldThrow<BusinessException> {
                    returnService.createReturnRequest(
                        memberId = 1L,
                        sellerId = 1L,
                        request =
                            CreateReturnRequest(
                                orderId = 101L,
                                orderItemId = 1L,
                                productOptionId = 1L,
                                quantity = 1,
                                reason = ReturnReason.CHANGE_OF_MIND,
                                paymentAmount = 50000L,
                            ),
                    )
                }
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
