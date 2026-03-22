package com.closet.shipping.presentation

import com.closet.common.response.ApiResponse
import com.closet.shipping.application.ReturnRequestService
import com.closet.shipping.application.ShipmentService
import com.closet.shipping.presentation.dto.CreateReturnRequest
import com.closet.shipping.presentation.dto.CreateShipmentRequest
import com.closet.shipping.presentation.dto.RegisterTrackingRequest
import com.closet.shipping.presentation.dto.ReturnRequestResponse
import com.closet.shipping.presentation.dto.ShipmentResponse
import com.closet.shipping.presentation.dto.UpdateShipmentStatusRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ShipmentController(
    private val shipmentService: ShipmentService,
    private val returnRequestService: ReturnRequestService,
) {

    @PostMapping("/shipments")
    @ResponseStatus(HttpStatus.CREATED)
    fun createShipment(@RequestBody @Valid request: CreateShipmentRequest): ApiResponse<ShipmentResponse> {
        val response = shipmentService.createShipment(request)
        return ApiResponse.created(response)
    }

    @GetMapping("/shipments/orders/{orderId}")
    fun getShipmentsByOrderId(@PathVariable orderId: Long): ApiResponse<List<ShipmentResponse>> {
        val response = shipmentService.findByOrderId(orderId)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/shipments/{id}/tracking")
    fun registerTracking(
        @PathVariable id: Long,
        @RequestBody @Valid request: RegisterTrackingRequest,
    ): ApiResponse<ShipmentResponse> {
        val response = shipmentService.registerTracking(id, request.carrier, request.trackingNumber)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/shipments/{id}/status")
    fun updateShipmentStatus(
        @PathVariable id: Long,
        @RequestBody @Valid request: UpdateShipmentStatusRequest,
    ): ApiResponse<ShipmentResponse> {
        val response = shipmentService.updateStatus(id, request.status)
        return ApiResponse.ok(response)
    }

    @PostMapping("/returns")
    @ResponseStatus(HttpStatus.CREATED)
    fun createReturnRequest(@RequestBody @Valid request: CreateReturnRequest): ApiResponse<ReturnRequestResponse> {
        val response = returnRequestService.requestReturn(request)
        return ApiResponse.created(response)
    }

    @PatchMapping("/returns/{id}/approve")
    fun approveReturn(@PathVariable id: Long): ApiResponse<ReturnRequestResponse> {
        val response = returnRequestService.approveReturn(id)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/returns/{id}/reject")
    fun rejectReturn(@PathVariable id: Long): ApiResponse<ReturnRequestResponse> {
        val response = returnRequestService.rejectReturn(id)
        return ApiResponse.ok(response)
    }
}
