package com.closet.shipping.application.carrier

/**
 * 택배사 어댑터 인터페이스 (Strategy 패턴, PD-02).
 *
 * 각 택배사 개별 API를 직접 호출한다.
 * CJ, 로젠, 롯데, 우체국 4개 구현체.
 */
interface CarrierAdapter {

    fun getCarrierCode(): String

    fun registerShipment(request: ShipmentRegistrationRequest): ShipmentRegistrationResponse

    fun trackShipment(trackingNumber: String): TrackingResponse

    fun validateTrackingNumber(trackingNumber: String): Boolean
}

data class ShipmentRegistrationRequest(
    val orderId: Long,
    val senderName: String,
    val receiverName: String,
    val receiverPhone: String,
    val receiverAddress: String,
)

data class ShipmentRegistrationResponse(
    val trackingNumber: String,
    val carrier: String,
    val status: String,
)

data class TrackingResponse(
    val trackingNumber: String,
    val carrier: String,
    val status: String,
    val events: List<TrackingEvent>,
)

data class TrackingEvent(
    val status: String,
    val description: String,
    val location: String,
    val time: String,
)
