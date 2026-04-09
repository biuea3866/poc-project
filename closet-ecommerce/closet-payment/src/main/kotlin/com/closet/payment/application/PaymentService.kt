package com.closet.payment.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.vo.Money
import com.closet.payment.domain.Payment
import com.closet.payment.domain.PaymentMethod
import com.closet.payment.domain.PaymentRepository
import com.closet.payment.infrastructure.PaymentApproveRequest
import com.closet.payment.infrastructure.PaymentCancelRequest
import com.closet.payment.infrastructure.PaymentGatewayFactory
import com.closet.payment.infrastructure.PaymentRefundRequest
import com.closet.payment.infrastructure.PaymentType
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentGatewayFactory: PaymentGatewayFactory,
) {
    fun getByOrderId(orderId: Long): PaymentResponse {
        val payment =
            paymentRepository.findByOrderId(orderId)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다: orderId=$orderId") }
        return PaymentResponse.from(payment)
    }

    @Transactional
    fun confirm(request: ConfirmPaymentRequest): PaymentResponse {
        // 1. Gateway 승인 먼저 (실패 시 DB 미변경)
        val gateway = paymentGatewayFactory.getGateway(request.paymentType)
        gateway.approve(
            PaymentApproveRequest(
                paymentKey = request.paymentKey,
                orderId = request.orderId,
                amount = request.amount,
            ),
        )

        // 2. DB 업데이트
        val payment =
            paymentRepository.findByOrderId(request.orderId)
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
    fun cancel(
        id: Long,
        request: CancelPaymentRequest,
        paymentType: PaymentType,
    ): PaymentResponse {
        val payment =
            paymentRepository.findById(id)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다: id=$id") }

        // 1. Gateway 취소 먼저
        val gateway = paymentGatewayFactory.getGateway(paymentType)
        gateway.cancel(
            PaymentCancelRequest(
                paymentKey = payment.paymentKey ?: "",
                reason = request.reason,
            ),
        )

        // 2. DB 업데이트
        payment.cancel()
        logger.info { "결제 취소 완료: id=$id, reason=${request.reason}" }
        return PaymentResponse.from(payment)
    }

    @Transactional
    fun refund(
        id: Long,
        request: RefundPaymentRequest,
        paymentType: PaymentType,
    ): PaymentResponse {
        val payment =
            paymentRepository.findById(id)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다: id=$id") }

        // 1. Gateway 환불 먼저
        val gateway = paymentGatewayFactory.getGateway(paymentType)
        gateway.refund(
            PaymentRefundRequest(
                paymentKey = payment.paymentKey ?: "",
                cancelAmount = request.amount,
                reason = request.reason,
            ),
        )

        // 2. DB 업데이트
        payment.refund(Money(request.amount.toBigDecimal()))
        logger.info { "부분 환불 완료: id=$id, refundAmount=${request.amount}, reason=${request.reason}" }
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
    val paymentType: PaymentType = PaymentType.TOSS,
)

data class CancelPaymentRequest(
    val reason: String,
)

data class RefundPaymentRequest(
    val amount: Long,
    val reason: String? = null,
)
