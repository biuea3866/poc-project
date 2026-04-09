package com.closet.payment.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.payment.application.PaymentService
import com.closet.payment.application.RefundPaymentRequest
import com.closet.payment.consumer.event.ShippingEvent
import com.closet.payment.domain.Payment
import com.closet.payment.domain.PaymentRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class ReturnApprovedConsumerTest : BehaviorSpec({

    val paymentService = mockk<PaymentService>(relaxed = true)
    val paymentRepository = mockk<PaymentRepository>()
    val idempotencyChecker = mockk<IdempotencyChecker>()

    val consumer =
        ReturnApprovedConsumer(
            paymentService = paymentService,
            paymentRepository = paymentRepository,
            idempotencyChecker = idempotencyChecker,
        )

    Given("반품 승인 이벤트가 수신되면") {
        val payment = mockk<Payment>()
        every { payment.id } returns 100L

        every { paymentRepository.findByOrderId(1L) } returns Optional.of(payment)
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        val event =
            ShippingEvent(
                eventType = "ReturnApproved",
                orderId = 1L,
                returnRequestId = 50L,
                refundAmount = 39900L,
            )

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

            Then("PaymentService.refund가 호출된다") {
                verify(exactly = 1) {
                    paymentService.refund(
                        100L,
                        RefundPaymentRequest(
                            amount = 39900L,
                            reason = "반품 승인 환불 (returnRequestId=50)",
                        ),
                        any(),
                    )
                }
            }
        }
    }

    Given("결제 정보를 찾을 수 없는 경우") {
        clearMocks(paymentService)

        every { paymentRepository.findByOrderId(999L) } returns Optional.empty()
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        val event =
            ShippingEvent(
                eventType = "ReturnApproved",
                orderId = 999L,
                returnRequestId = 51L,
                refundAmount = 10000L,
            )

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

            Then("PaymentService.refund가 호출되지 않는다") {
                verify(exactly = 0) { paymentService.refund(any(), any(), any()) }
            }
        }
    }

    Given("처리하지 않는 eventType 수신") {
        val event =
            ShippingEvent(
                eventType = "ShippingStatusChanged",
                orderId = 1L,
            )

        When("ShippingStatusChanged 이벤트 수신") {
            consumer.handle(event)

            Then("무시된다") {
                // eventType 필터에 의해 무시
            }
        }
    }
})
