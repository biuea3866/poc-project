package com.closet.bff.client

import com.closet.bff.dto.ExchangeRequestResponse
import com.closet.bff.dto.ReturnRequestBffResponse
import com.closet.bff.dto.ShipmentBffResponse
import com.closet.bff.dto.TrackingLogBffResponse
import com.closet.common.response.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

/**
 * Shipping Service Feign Client (CP-30).
 * Port: 8088 (PD-05)
 */
@FeignClient(name = "shipping-service", url = "\${service.shipping.url}")
interface ShippingServiceClient {
    @GetMapping("/api/v1/shippings")
    fun getShipmentByOrderId(
        @RequestParam orderId: Long,
    ): ApiResponse<ShipmentBffResponse>

    @GetMapping("/api/v1/shippings/{id}")
    fun getShipment(
        @PathVariable id: Long,
    ): ApiResponse<ShipmentBffResponse>

    @GetMapping("/api/v1/shippings/{id}/tracking")
    fun getTrackingLogs(
        @PathVariable id: Long,
    ): ApiResponse<List<TrackingLogBffResponse>>

    @GetMapping("/api/v1/returns")
    fun getReturnsByOrderId(
        @RequestParam orderId: Long,
    ): ApiResponse<List<ReturnRequestBffResponse>>

    @GetMapping("/api/v1/returns/{id}")
    fun getReturn(
        @PathVariable id: Long,
    ): ApiResponse<ReturnRequestBffResponse>

    @GetMapping("/api/v1/exchanges")
    fun getExchangesByOrderId(
        @RequestParam orderId: Long,
    ): ApiResponse<List<ExchangeRequestResponse>>

    @GetMapping("/api/v1/exchanges/{id}")
    fun getExchange(
        @PathVariable id: Long,
    ): ApiResponse<ExchangeRequestResponse>
}
