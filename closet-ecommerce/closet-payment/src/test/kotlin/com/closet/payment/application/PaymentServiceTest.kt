package com.closet.payment.application

import com.closet.common.exception.BusinessException
import com.closet.common.vo.Money
import com.closet.payment.domain.Payment
import com.closet.payment.domain.PaymentRepository
import com.closet.payment.infrastructure.PaymentApproveResponse
import com.closet.payment.infrastructure.PaymentCancelResponse
import com.closet.payment.infrastructure.PaymentGateway
import com.closet.payment.infrastructure.PaymentGatewayFactory
import com.closet.payment.infrastructure.PaymentRefundResponse
import com.closet.payment.infrastructure.PaymentType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional

class PaymentServiceTest : BehaviorSpec({

    val paymentRepository = mockk<PaymentRepository>()
    val paymentGatewayFactory = mockk<PaymentGatewayFactory>()
    val mockGateway = mockk<PaymentGateway>()
    val service = PaymentService(paymentRepository, paymentGatewayFactory)

    Given("결제 승인 — Gateway 호출 후 DB 업데이트") {
        val request =
            ConfirmPaymentRequest(
                paymentKey = "toss_pay_key_123",
                orderId = 100L,
                amount = 50000L,
                paymentType = PaymentType.TOSS,
            )

        every { paymentGatewayFactory.getGateway(PaymentType.TOSS) } returns mockGateway
        every { mockGateway.approve(any()) } returns
            PaymentApproveResponse(
                approved = true,
                paymentKey = "toss_pay_key_123",
                approvedAt = "2026-04-09T14:00:00+09:00",
            )
        every { paymentRepository.findByOrderId(100L) } returns Optional.empty()
        val savedSlot = slot<Payment>()
        every { paymentRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        When("정상 승인") {
            val response = service.confirm(request)

            Then("Gateway approve가 먼저 호출된다") {
                verify(exactly = 1) { mockGateway.approve(any()) }
            }

            Then("DB에 결제가 저장된다") {
                verify(exactly = 1) { paymentRepository.save(any()) }
                response.status shouldBe "PAID"
                response.paymentKey shouldBe "toss_pay_key_123"
            }
        }
    }

    Given("결제 승인 — Gateway 실패 시 DB 미변경") {
        val request =
            ConfirmPaymentRequest(
                paymentKey = "fail_key",
                orderId = 200L,
                amount = 30000L,
                paymentType = PaymentType.TOSS,
            )

        every { paymentGatewayFactory.getGateway(PaymentType.TOSS) } returns mockGateway
        every { mockGateway.approve(any()) } throws
            BusinessException(
                com.closet.common.exception.ErrorCode.EXTERNAL_API_ERROR,
                "PG 승인 실패",
            )

        When("Gateway 승인 실패") {
            Then("BusinessException이 전파되고 DB는 변경되지 않는다") {
                shouldThrow<BusinessException> {
                    service.confirm(request)
                }
                verify(exactly = 0) { paymentRepository.save(any()) }
            }
        }
    }

    Given("결제 취소 — Gateway 호출 후 DB 업데이트") {
        val payment = Payment.create(orderId = 300L, finalAmount = Money(java.math.BigDecimal("50000")))
        payment.confirm("cancel_test_key", com.closet.payment.domain.PaymentMethod.CARD)

        every { paymentRepository.findById(1L) } returns Optional.of(payment)
        every { paymentGatewayFactory.getGateway(PaymentType.TOSS) } returns mockGateway
        every { mockGateway.cancel(any()) } returns
            PaymentCancelResponse(
                cancelled = true,
                cancelledAt = "2026-04-09T14:00:00+09:00",
            )

        When("정상 취소") {
            val response = service.cancel(1L, CancelPaymentRequest("고객 요청"), PaymentType.TOSS)

            Then("Gateway cancel이 호출된다") {
                verify(exactly = 1) { mockGateway.cancel(any()) }
            }

            Then("DB 상태가 CANCELLED로 변경된다") {
                response.status shouldBe "CANCELLED"
            }
        }
    }

    Given("부분 환불 — Gateway 호출 후 DB 업데이트") {
        val payment = Payment.create(orderId = 400L, finalAmount = Money(java.math.BigDecimal("50000")))
        payment.confirm("refund_test_key", com.closet.payment.domain.PaymentMethod.CARD)

        every { paymentRepository.findById(2L) } returns Optional.of(payment)
        every { paymentGatewayFactory.getGateway(PaymentType.TOSS) } returns mockGateway
        every { mockGateway.refund(any()) } returns
            PaymentRefundResponse(
                refunded = true,
                refundAmount = 30000L,
                refundedAt = "2026-04-09T14:00:00+09:00",
            )

        When("30000원 부분 환불") {
            val response =
                service.refund(
                    2L,
                    RefundPaymentRequest(amount = 30000L, reason = "반품"),
                    PaymentType.TOSS,
                )

            Then("Gateway refund가 호출된다") {
                verify(exactly = 1) { mockGateway.refund(any()) }
            }

            Then("DB 환불 금액이 업데이트된다") {
                response.status shouldBe "REFUNDED"
                response.refundAmount shouldBe 30000L
            }
        }
    }
})
