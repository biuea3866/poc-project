package com.closet.shipping.application.carrier

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

/**
 * 우체국 택배사 어댑터.
 * Mock API: /carrier/epost/api/v1
 */
@Component
class EpostCarrierAdapter(
    private val restTemplate: RestTemplate,
    @Value("\${carrier.mock-api-base-url:http://localhost:9090}")
    private val mockApiBaseUrl: String,
) : CarrierAdapter {

    override fun getCarrierCode(): String = "EPOST"

    override fun registerShipment(request: ShipmentRegistrationRequest): ShipmentRegistrationResponse {
        val url = "$mockApiBaseUrl/carrier/epost/api/v1/shipments"
        val body = mapOf(
            "orderId" to request.orderId.toString(),
            "senderName" to request.senderName,
            "receiverName" to request.receiverName,
            "receiverPhone" to request.receiverPhone,
            "receiverAddress" to request.receiverAddress,
        )

        logger.info { "[EPOST] 배송 등록 요청: orderId=${request.orderId}" }
        val response = restTemplate.postForObject(url, body, Map::class.java)
        val data = response?.get("data") as? Map<*, *>
            ?: throw RuntimeException("EPOST 배송 등록 응답 파싱 실패")

        return ShipmentRegistrationResponse(
            trackingNumber = data["trackingNumber"].toString(),
            carrier = "EPOST",
            status = data["status"].toString(),
        )
    }

    override fun trackShipment(trackingNumber: String): TrackingResponse {
        val url = "$mockApiBaseUrl/carrier/epost/api/v1/tracking/$trackingNumber"
        logger.info { "[EPOST] 배송 추적 요청: trackingNumber=$trackingNumber" }

        val response = restTemplate.exchange(
            url, HttpMethod.GET, null,
            object : ParameterizedTypeReference<Map<String, Any>>() {}
        ).body

        val data = response?.get("data") as? Map<*, *>
            ?: throw RuntimeException("EPOST 배송 추적 응답 파싱 실패")

        return parseTrackingResponse(data, "EPOST", trackingNumber)
    }

    override fun validateTrackingNumber(trackingNumber: String): Boolean {
        return trackingNumber.matches(Regex("[A-Z]{0,4}[0-9]{10,15}"))
    }
}
