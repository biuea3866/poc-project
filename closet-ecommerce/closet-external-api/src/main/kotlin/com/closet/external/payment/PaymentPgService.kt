package com.closet.external.payment

import com.closet.external.domain.MockPayment
import com.closet.external.domain.MockPaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.atomic.AtomicLong

@Service
@Transactional(readOnly = true)
class PaymentPgService(
    private val paymentRepository: MockPaymentRepository,
) {
    private val seq = AtomicLong(1)

    fun generateKey(prefix: String): String = "${prefix}_${System.currentTimeMillis()}_${seq.getAndIncrement()}"

    @Transactional
    fun createPayment(
        provider: String,
        paymentKey: String,
        orderId: String,
        amount: Long,
        orderName: String? = null,
        buyerName: String? = null,
        buyerTel: String? = null,
    ): MockPayment {
        val payment = MockPayment(
            provider = provider,
            paymentKey = paymentKey,
            orderId = orderId,
            totalAmount = amount,
            balanceAmount = amount,
            orderName = orderName,
            buyerName = buyerName,
            buyerTel = buyerTel,
        )
        return paymentRepository.save(payment)
    }

    @Transactional
    fun approvePayment(paymentKey: String, method: String): MockPayment {
        val payment = paymentRepository.findByPaymentKey(paymentKey)
            .orElseThrow { IllegalArgumentException("결제 정보를 찾을 수 없습니다: $paymentKey") }
        payment.approve(method, "A${System.currentTimeMillis() % 100000000}")
        return payment
    }

    @Transactional
    fun cancelPayment(paymentKey: String, reason: String, cancelAmount: Long? = null): MockPayment {
        val payment = paymentRepository.findByPaymentKey(paymentKey)
            .orElseThrow { IllegalArgumentException("결제 정보를 찾을 수 없습니다: $paymentKey") }
        payment.cancel(reason, cancelAmount)
        return payment
    }

    fun findByPaymentKey(paymentKey: String): MockPayment? =
        paymentRepository.findByPaymentKey(paymentKey).orElse(null)

    fun findByOrderId(orderId: String): MockPayment? =
        paymentRepository.findByOrderId(orderId).orElse(null)
}
