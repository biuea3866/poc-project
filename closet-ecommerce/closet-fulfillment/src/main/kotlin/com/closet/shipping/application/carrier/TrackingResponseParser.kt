package com.closet.shipping.application.carrier

/**
 * Mock 택배사 API 응답 공통 파서.
 */
fun parseTrackingResponse(
    data: Map<*, *>,
    carrier: String,
    trackingNumber: String,
): TrackingResponse {
    val history =
        (data["trackingHistory"] as? List<*>)?.mapNotNull { event ->
            val eventMap = event as? Map<*, *> ?: return@mapNotNull null
            TrackingEvent(
                status = eventMap["status"]?.toString() ?: "",
                description = eventMap["description"]?.toString() ?: "",
                location = eventMap["location"]?.toString() ?: "",
                time = eventMap["time"]?.toString() ?: "",
            )
        } ?: emptyList()

    return TrackingResponse(
        trackingNumber = trackingNumber,
        carrier = carrier,
        status = data["status"]?.toString() ?: "UNKNOWN",
        events = history,
    )
}
