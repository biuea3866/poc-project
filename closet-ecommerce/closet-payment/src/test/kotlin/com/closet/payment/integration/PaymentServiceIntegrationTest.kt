package com.closet.payment.integration

import com.closet.common.exception.BusinessException
import com.closet.common.test.ClosetIntegrationTest
import com.closet.common.vo.Money
import com.closet.payment.application.CancelPaymentRequest
import com.closet.payment.application.ConfirmPaymentRequest
import com.closet.payment.application.PaymentService
import com.closet.payment.application.RefundPaymentRequest
import com.closet.payment.domain.Payment
import com.closet.payment.domain.PaymentMethod
import com.closet.payment.domain.PaymentRepository
import com.closet.payment.domain.PaymentStatus
import com.closet.payment.infrastructure.PaymentApproveResponse
import com.closet.payment.infrastructure.PaymentCancelResponse
import com.closet.payment.infrastructure.PaymentGateway
import com.closet.payment.infrastructure.PaymentRefundResponse
import com.closet.payment.infrastructure.PaymentType
import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceIntegrationTest(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository,
    @MockkBean private val mockGateway: PaymentGateway,
) : BehaviorSpec({

        beforeEach {
            paymentRepository.deleteAll()

            // 기본 Mock 설정: 모든 Gateway 호출 성공
            every { mockGateway.getPaymentType() } returns PaymentType.TOSS
            every { mockGateway.approve(any()) } returns
                PaymentApproveResponse(
                    approved = true,
                    paymentKey = "toss_test_key",
                    approvedAt = "2026-04-06T14:00:00+09:00",
                )
            every { mockGateway.cancel(any()) } returns
                PaymentCancelResponse(
                    cancelled = true,
                    cancelledAt = "2026-04-06T15:00:00+09:00",
                )
            every { mockGateway.refund(any()) } returns
                PaymentRefundResponse(
                    refunded = true,
                    refundAmount = 0L,
                    refundedAt = "2026-04-06T16:00:00+09:00",
                )
        }

        Given("결제 승인 (confirm) -- 실제 DB에 저장") {

            When("새 주문에 대해 결제를 승인하면") {
                val request =
                    ConfirmPaymentRequest(
                        paymentKey = "toss_pay_key_001",
                        orderId = 100L,
                        amount = 50000L,
                        paymentType = PaymentType.TOSS,
                    )
                val response = paymentService.confirm(request)

                Then("Payment가 PAID 상태로 DB에 저장된다") {
                    response.status shouldBe "PAID"
                    response.paymentKey shouldBe "toss_pay_key_001"
                    response.orderId shouldBe 100L
                    response.finalAmount shouldBe 50000L

                    val payment = paymentRepository.findByOrderId(100L).orElse(null)
                    payment shouldNotBe null
                    payment.status shouldBe PaymentStatus.PAID
                    payment.paymentKey shouldBe "toss_pay_key_001"
                    payment.method shouldBe PaymentMethod.CARD
                    payment.createdAt shouldNotBe null
                }
            }

            When("Gateway 승인이 실패하면") {
                every { mockGateway.approve(any()) } throws
                    BusinessException(
                        com.closet.common.exception.ErrorCode.EXTERNAL_API_ERROR,
                        "PG 승인 실패",
                    )

                Then("DB에 Payment가 저장되지 않는다") {
                    shouldThrow<BusinessException> {
                        paymentService.confirm(
                            ConfirmPaymentRequest(
                                paymentKey = "fail_key",
                                orderId = 999L,
                                amount = 30000L,
                                paymentType = PaymentType.TOSS,
                            ),
                        )
                    }

                    val payment = paymentRepository.findByOrderId(999L).orElse(null)
                    payment shouldBe null
                }
            }
        }

        Given("결제 취소 (cancel) -- DB 상태 전이 확인") {
            // 사전 조건: 승인된 결제 생성
            val payment = Payment.create(orderId = 200L, finalAmount = Money(java.math.BigDecimal("50000")))
            payment.confirm("cancel_test_key", PaymentMethod.CARD)
            val saved = paymentRepository.save(payment)

            When("결제를 취소하면") {
                val response =
                    paymentService.cancel(
                        saved.id,
                        CancelPaymentRequest(reason = "고객 변심"),
                        PaymentType.TOSS,
                    )

                Then("Payment 상태가 CANCELLED로 변경된다") {
                    response.status shouldBe "CANCELLED"

                    val updated = paymentRepository.findById(saved.id).orElse(null)
                    updated shouldNotBe null
                    updated.status shouldBe PaymentStatus.CANCELLED
                }
            }
        }

        Given("부분 환불 (refund) -- 환불 금액 누적 확인") {
            // 사전 조건: 50000원 결제 승인
            val payment = Payment.create(orderId = 300L, finalAmount = Money(java.math.BigDecimal("50000")))
            payment.confirm("refund_test_key", PaymentMethod.CARD)
            val saved = paymentRepository.save(payment)

            When("30000원 부분 환불하면") {
                every { mockGateway.refund(any()) } returns
                    PaymentRefundResponse(
                        refunded = true,
                        refundAmount = 30000L,
                        refundedAt = "2026-04-06T16:00:00+09:00",
                    )

                val response =
                    paymentService.refund(
                        saved.id,
                        RefundPaymentRequest(amount = 30000L, reason = "반품"),
                        PaymentType.TOSS,
                    )

                Then("환불 금액이 DB에 반영되고 상태가 REFUNDED가 된다") {
                    response.status shouldBe "REFUNDED"
                    response.refundAmount shouldBe 30000L

                    val updated = paymentRepository.findById(saved.id).orElse(null)
                    updated shouldNotBe null
                    updated.status shouldBe PaymentStatus.REFUNDED
                    updated.refundAmount.amount.toLong() shouldBe 30000L
                }
            }

            When("추가로 20000원 환불하면 (누적 환불)") {
                every { mockGateway.refund(any()) } returns
                    PaymentRefundResponse(
                        refunded = true,
                        refundAmount = 20000L,
                        refundedAt = "2026-04-06T17:00:00+09:00",
                    )

                val response =
                    paymentService.refund(
                        saved.id,
                        RefundPaymentRequest(amount = 20000L, reason = "추가 반품"),
                        PaymentType.TOSS,
                    )

                Then("총 환불 금액이 50000원이 된다") {
                    response.status shouldBe "REFUNDED"
                    response.refundAmount shouldBe 50000L

                    val updated = paymentRepository.findById(saved.id).orElse(null)
                    updated.refundAmount.amount.toLong() shouldBe 50000L
                }
            }

            When("결제 금액을 초과하는 환불을 시도하면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        paymentService.refund(
                            saved.id,
                            RefundPaymentRequest(amount = 1L, reason = "초과 환불"),
                            PaymentType.TOSS,
                        )
                    }
                }
            }
        }

        Given("결제 조회 -- orderId로 조회") {
            // 사전 조건: 결제 승인
            val payment = Payment.create(orderId = 400L, finalAmount = Money(java.math.BigDecimal("25000")))
            payment.confirm("query_test_key", PaymentMethod.CARD)
            paymentRepository.save(payment)

            When("orderId로 조회하면") {
                val response = paymentService.getByOrderId(400L)

                Then("결제 정보가 반환된다") {
                    response.orderId shouldBe 400L
                    response.paymentKey shouldBe "query_test_key"
                    response.status shouldBe "PAID"
                    response.finalAmount shouldBe 25000L
                }
            }

            When("존재하지 않는 orderId로 조회하면") {
                Then("BusinessException이 발생한다") {
                    shouldThrow<BusinessException> {
                        paymentService.getByOrderId(99999L)
                    }
                }
            }
        }
    }) {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            ClosetIntegrationTest.overrideProperties(registry)
        }
    }
}
