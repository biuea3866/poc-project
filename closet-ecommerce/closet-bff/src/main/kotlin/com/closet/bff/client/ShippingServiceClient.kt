package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "shipping-service", url = "\${service.shipping.url}")
interface ShippingServiceClient {

    @PostMapping("/shipments")
    fun createShipment(@RequestBody request: Any): Any

    @GetMapping("/shipments/orders/{orderId}")
    fun getShipmentsByOrderId(@PathVariable orderId: Long): Any

    @PatchMapping("/shipments/{id}/tracking")
    fun registerTracking(@PathVariable id: Long, @RequestBody request: Any): Any

    @PatchMapping("/shipments/{id}/status")
    fun updateShipmentStatus(@PathVariable id: Long, @RequestBody request: Any): Any

    @PostMapping("/returns")
    fun createReturnRequest(@RequestBody request: Any): Any

    @PatchMapping("/returns/{id}/approve")
    fun approveReturn(@PathVariable id: Long): Any

    @PatchMapping("/returns/{id}/reject")
    fun rejectReturn(@PathVariable id: Long): Any
}
