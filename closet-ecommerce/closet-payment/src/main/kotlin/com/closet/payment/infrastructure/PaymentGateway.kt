package com.closet.payment.infrastructure

/**
 * PG사 결제 게이트웨이 인터페이스 (Port-Adapter 패턴).
 *
 * CarrierAdapter와 동일한 패턴으로, PG사별 구현체를 제공한다.
 * - TossPaymentGateway
 * - (향후) KakaoPayGateway, NaverPayGateway, DanalGateway
 */
interface PaymentGateway {
    fun getPaymentType(): PaymentType

    fun approve(request: PaymentApproveRequest): PaymentApproveResponse

    fun cancel(request: PaymentCancelRequest): PaymentCancelResponse

    fun refund(request: PaymentRefundRequest): PaymentRefundResponse
}

enum class PaymentType {
    TOSS,
    NAVER_PAY,
    KAKAO_PAY,
    DANAL,
}

data class PaymentApproveRequest(
    val paymentKey: String,
    val orderId: Long,
    val amount: Long,
)

data class PaymentApproveResponse(
    val approved: Boolean,
    val paymentKey: String,
    val approvedAt: String,
)

data class PaymentCancelRequest(
    val paymentKey: String,
    val reason: String,
)

data class PaymentCancelResponse(
    val cancelled: Boolean,
    val cancelledAt: String,
)

data class PaymentRefundRequest(
    val paymentKey: String,
    val cancelAmount: Long,
    val reason: String? = null,
)

data class PaymentRefundResponse(
    val refunded: Boolean,
    val refundAmount: Long,
    val refundedAt: String,
)
