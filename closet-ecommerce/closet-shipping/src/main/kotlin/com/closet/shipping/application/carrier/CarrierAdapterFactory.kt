package com.closet.shipping.application.carrier

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import org.springframework.stereotype.Component

/**
 * 택배사 어댑터 팩토리.
 *
 * carrierCode로 적합한 CarrierAdapter 구현체를 반환한다.
 */
@Component
class CarrierAdapterFactory(
    private val adapters: List<CarrierAdapter>,
) {
    fun getAdapter(carrierCode: String): CarrierAdapter {
        return adapters.find { it.getCarrierCode() == carrierCode }
            ?: throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "지원하지 않는 택배사: $carrierCode"
            )
    }

    fun getSupportedCarriers(): List<String> {
        return adapters.map { it.getCarrierCode() }
    }
}
