package com.closet.payment.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.vo.Money
import com.closet.payment.domain.Payment
import com.closet.payment.domain.PaymentMethod
import com.closet.payment.domain.PaymentRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {

    fun getByOrderId(orderId: Long): PaymentResponse {
        val payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다: orderId=$orderId") }
        return PaymentResponse.from(payment)
    }

    @Transactional
    fun confirm(request: ConfirmPaymentRequest): PaymentResponse {
        val payment = paymentRepository.findByOrderId(request.orderId)
            .orElseGet {
                Payment.create(
                    orderId = request.orderId,
                    finalAmount = Money(request.amount.toBigDecimal()),
                )
            }
        payment.confirm(
            paymentKey = request.paymentKey,
            method = PaymentMethod.CARD,
        )
        val saved = paymentRepository.save(payment)
        logger.info { "결제 승인 완료: orderId=${request.orderId}, paymentKey=${request.paymentKey}" }
        return PaymentResponse.from(saved)
    }

    @Transactional
    fun cancel(id: Long, request: CancelPaymentRequest): PaymentResponse {
        val payment = paymentRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다: id=$id") }
        payment.cancel()
        logger.info { "결제 취소 완료: id=$id, reason=${request.reason}" }
        return PaymentResponse.from(payment)
    }
}

data class PaymentResponse(
    val id: Long,
    val orderId: Long,
    val paymentKey: String?,
    val method: String?,
    val finalAmount: Long,
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
