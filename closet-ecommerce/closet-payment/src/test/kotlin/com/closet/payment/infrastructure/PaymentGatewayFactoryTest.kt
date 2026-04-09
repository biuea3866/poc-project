package com.closet.payment.infrastructure

import com.closet.common.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PaymentGatewayFactoryTest : BehaviorSpec({

    Given("PaymentGatewayFactory에 Toss 어댑터가 등록되어 있을 때") {
        val tossGateway = MockTossPaymentGateway()
        val factory = PaymentGatewayFactory(listOf(tossGateway))

        When("TOSS 타입으로 조회") {
            val gateway = factory.getGateway(PaymentType.TOSS)

            Then("TossPaymentGateway가 반환된다") {
                gateway.getPaymentType() shouldBe PaymentType.TOSS
            }
        }

        When("지원하지 않는 타입으로 조회") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    factory.getGateway(PaymentType.KAKAO_PAY)
                }
            }
        }

        When("지원 PG 목록 조회") {
            val types = factory.getSupportedTypes()

            Then("TOSS가 포함된다") {
                types shouldBe listOf(PaymentType.TOSS)
            }
        }
    }
})

/**
 * 테스트용 Mock Toss Gateway
 */
private class MockTossPaymentGateway : PaymentGateway {
    override fun getPaymentType(): PaymentType = PaymentType.TOSS

    override fun approve(request: PaymentApproveRequest): PaymentApproveResponse =
        PaymentApproveResponse(
            approved = true,
            paymentKey = request.paymentKey,
            approvedAt = "2026-04-09T14:00:00+09:00",
        )

    override fun cancel(request: PaymentCancelRequest): PaymentCancelResponse =
        PaymentCancelResponse(
            cancelled = true,
            cancelledAt = "2026-04-09T14:00:00+09:00",
        )

    override fun refund(request: PaymentRefundRequest): PaymentRefundResponse =
        PaymentRefundResponse(
            refunded = true,
            refundAmount = request.cancelAmount,
            refundedAt = "2026-04-09T14:00:00+09:00",
        )
}
