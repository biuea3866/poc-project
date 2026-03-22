package com.closet.bff.presentation

import com.closet.bff.client.ShippingServiceClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ShippingProxyController(
    private val shippingClient: ShippingServiceClient,
) {

    @PostMapping("/shipments")
    fun createShipment(@RequestBody request: Any) =
        shippingClient.createShipment(request)

    @GetMapping("/shipments/orders/{orderId}")
    fun getShipmentsByOrderId(@PathVariable orderId: Long) =
        shippingClient.getShipmentsByOrderId(orderId)

    @PatchMapping("/shipments/{id}/tracking")
    fun registerTracking(@PathVariable id: Long, @RequestBody request: Any) =
        shippingClient.registerTracking(id, request)

    @PatchMapping("/shipments/{id}/status")
    fun updateShipmentStatus(@PathVariable id: Long, @RequestBody request: Any) =
        shippingClient.updateShipmentStatus(id, request)

    @PostMapping("/returns")
    fun createReturnRequest(@RequestBody request: Any) =
        shippingClient.createReturnRequest(request)

    @PatchMapping("/returns/{id}/approve")
    fun approveReturn(@PathVariable id: Long) =
        shippingClient.approveReturn(id)

    @PatchMapping("/returns/{id}/reject")
    fun rejectReturn(@PathVariable id: Long) =
        shippingClient.rejectReturn(id)
}
