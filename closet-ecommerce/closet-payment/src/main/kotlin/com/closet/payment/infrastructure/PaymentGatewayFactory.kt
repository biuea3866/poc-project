package com.closet.payment.infrastructure

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import org.springframework.stereotype.Component

/**
 * PG사 게이트웨이 팩토리.
 *
 * PaymentType으로 적합한 PaymentGateway 구현체를 반환한다.
 * CarrierAdapterFactory와 동일한 패턴.
 */
@Component
class PaymentGatewayFactory(
    private val gateways: List<PaymentGateway>,
) {
    fun getGateway(paymentType: PaymentType): PaymentGateway {
        return gateways.find { it.getPaymentType() == paymentType }
            ?: throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "지원하지 않는 PG사: $paymentType",
            )
    }

    fun getSupportedTypes(): List<PaymentType> {
        return gateways.map { it.getPaymentType() }
    }
}
