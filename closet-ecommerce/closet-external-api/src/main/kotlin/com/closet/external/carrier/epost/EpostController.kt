package com.closet.external.carrier.epost

import com.closet.external.carrier.CarrierService
import com.closet.external.domain.MockShipment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/carrier/epost/api/v1")
class EpostController(private val carrierService: CarrierService) {
    @PostMapping("/shipments")
    fun register(
        @RequestBody request: Map<String, Any>,
    ): ResponseEntity<*> {
        val shipment = carrierService.registerShipment("EPOST", "EP", request)
        return ResponseEntity.ok(resp(shipment))
    }

    @GetMapping("/tracking/{trackingNumber}")
    fun track(
        @PathVariable trackingNumber: String,
    ): ResponseEntity<*> {
        val shipment =
            carrierService.getTracking("EPOST", trackingNumber)
                ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(resp(shipment))
    }

    @PostMapping("/tracking/{trackingNumber}/advance")
    fun advance(
        @PathVariable trackingNumber: String,
    ): ResponseEntity<*> {
        val shipment =
            carrierService.advanceStatus(trackingNumber)
                ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(resp(shipment))
    }

    private fun resp(s: MockShipment) = mapOf("resultCode" to "0000", "data" to shipmentBody(s))

    private fun shipmentBody(s: MockShipment) =
        mapOf(
            "trackingNumber" to s.trackingNumber, "carrier" to s.carrier, "orderId" to s.orderId,
            "status" to s.status, "senderName" to s.senderName,
            "receiverName" to s.receiverName, "receiverAddress" to s.receiverAddress,
            "deliveredAt" to s.deliveredAt?.toString(), "registeredAt" to s.createdAt.toString(),
            "trackingHistory" to
                s.trackingHistory.map {
                    mapOf(
                        "status" to it.status,
                        "description" to it.description,
                        "location" to it.location,
                        "time" to it.trackedAt.toString(),
                    )
                },
        )
}
