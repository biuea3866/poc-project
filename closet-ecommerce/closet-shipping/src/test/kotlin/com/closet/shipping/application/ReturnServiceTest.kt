package com.closet.shipping.application

import com.closet.common.exception.BusinessException
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.common.vo.Money
import com.closet.shipping.domain.ReturnReason
import com.closet.shipping.domain.ReturnRequest
import com.closet.shipping.domain.ReturnRequestRepository
import com.closet.shipping.domain.ReturnStatus
import com.closet.shipping.domain.ShippingFeePolicy
import com.closet.shipping.domain.ShippingFeePolicyRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class ReturnServiceTest : BehaviorSpec({

    val returnRequestRepository = mockk<ReturnRequestRepository>(relaxed = true)
    val shippingFeePolicyRepository = mockk<ShippingFeePolicyRepository>()
    val outboxEventPublisher = mockk<OutboxEventPublisher>(relaxed = true)
    val objectMapper = ObjectMapper()

    val returnService = ReturnService(
        returnRequestRepository,
        shippingFeePolicyRepository,
        outboxEventPublisher,
        objectMapper,
    )

    Given("반품 신청 - 단순변심") {

        When("CHANGE_OF_MIND 사유로 반품 신청") {
            val policy = ShippingFeePolicy(
                type = "RETURN", reason = "CHANGE_OF_MIND",
                payer = "BUYER", fee = Money.of(3000),
            )
            every { shippingFeePolicyRepository.findByTypeAndReasonAndIsActiveTrue("RETURN", "CHANGE_OF_MIND") } returns Optional.of(policy)
            every { returnRequestRepository.save(any()) } answers { firstArg() }

            val response = returnService.createReturnRequest(
                memberId = 1L,
                sellerId = 1L,
                request = CreateReturnRequest(
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
            val policy = ShippingFeePolicy(
                type = "RETURN", reason = "DEFECTIVE",
                payer = "SELLER", fee = Money.ZERO,
            )
            every { shippingFeePolicyRepository.findByTypeAndReasonAndIsActiveTrue("RETURN", "DEFECTIVE") } returns Optional.of(policy)
            every { returnRequestRepository.save(any()) } answers { firstArg() }

            val response = returnService.createReturnRequest(
                memberId = 1L,
                sellerId = 1L,
                request = CreateReturnRequest(
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
            val returnRequest = ReturnRequest.create(
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

            Then("return.approved 이벤트 발행됨") {
                verify(exactly = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "ReturnRequest",
                        aggregateId = any(),
                        eventType = "ReturnApproved",
                        topic = "return.approved",
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
            val activeReturn = ReturnRequest.create(
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
})
