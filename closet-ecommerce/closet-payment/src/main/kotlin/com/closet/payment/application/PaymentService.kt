package com.closet.payment.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.vo.Money
import com.closet.payment.domain.Payment
import com.closet.payment.domain.PaymentMethod
import com.closet.payment.domain.PaymentRepository
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {
    fun getByOrderId(orderId: Long): PaymentResponse {
        val payment =
            paymentRepository.findByOrderId(orderId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다: orderId=$orderId")
        return PaymentResponse.from(payment)
    }

    @Transactional
    fun confirm(request: ConfirmPaymentRequest): PaymentResponse {
        val payment =
            paymentRepository.findByOrderId(request.orderId)
                ?: Payment.create(
                    orderId = request.orderId,
                    finalAmount = Money(request.amount.toBigDecimal()),
                )
        payment.confirm(
            paymentKey = request.paymentKey,
            method = PaymentMethod.CARD,
        )
        val saved = paymentRepository.save(payment)
        logger.info { "결제 승인 완료: orderId=${request.orderId}, paymentKey=${request.paymentKey}" }
        return PaymentResponse.from(saved)
    }

    @Transactional
    fun cancel(
        id: Long,
        request: CancelPaymentRequest,
    ): PaymentResponse {
        val payment =
            paymentRepository.findByIdOrNull(id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다: id=$id")
        payment.cancel()
        logger.info { "결제 취소 완료: id=$id, reason=${request.reason}" }
        return PaymentResponse.from(payment)
    }

    /**
     * 부분 환불 (PD-12).
     * 반품 승인 시 배송비 공제 후 환불. PG사에 부분 취소(cancelAmount) 요청.
     */
    @Transactional
    fun refund(
        id: Long,
        request: RefundPaymentRequest,
    ): PaymentResponse {
        val payment =
            paymentRepository.findByIdOrNull(id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다: id=$id")
        payment.refund(Money(request.amount.toBigDecimal()))
        logger.info { "부분 환불 완료: id=$id, refundAmount=${request.amount}, reason=${request.reason}" }
        return PaymentResponse.from(payment)
    }

    @Transactional
    fun refundByOrderId(
        orderId: Long,
        request: RefundPaymentRequest,
    ): PaymentResponse? {
        val payment = paymentRepository.findByOrderId(orderId)
        if (payment == null) {
            logger.warn { "결제 정보를 찾을 수 없습니다: orderId=$orderId" }
            return null
        }

        payment.refund(Money(request.amount.toBigDecimal()))
        logger.info { "주문 기준 부분 환불 완료: orderId=$orderId, refundAmount=${request.amount}, reason=${request.reason}" }
        return PaymentResponse.from(payment)
    }
}

data class PaymentResponse(
    val id: Long,
    val orderId: Long,
    val paymentKey: String?,
    val method: String?,
    val finalAmount: Long,
    val refundAmount: Long,
    val status: String,
) {
    companion object {
        fun from(payment: Payment): PaymentResponse {
            return PaymentResponse(
                id = payment.id,
                orderId = payment.orderId,
                paymentKey = payment.paymentKey,
                method = payment.method?.name,
                finalAmount = payment.finalAmount.amount.toLong(),
                refundAmount = payment.refundAmount.amount.toLong(),
                status = payment.status.name,
            )
        }
    }
}

data class ConfirmPaymentRequest(
    val paymentKey: String,
    val orderId: Long,
    val amount: Long,
)

data class CancelPaymentRequest(
    val reason: String,
)

data class RefundPaymentRequest(
    val amount: Long,
    val reason: String? = null,
)
