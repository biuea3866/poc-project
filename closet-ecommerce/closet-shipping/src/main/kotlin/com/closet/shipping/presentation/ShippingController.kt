package com.closet.shipping.presentation

import com.closet.common.auth.MemberRole
import com.closet.common.auth.RoleRequired
import com.closet.common.response.ApiResponse
import com.closet.shipping.application.RegisterShipmentRequest
import com.closet.shipping.application.ShipmentResponse
import com.closet.shipping.application.ShippingService
import com.closet.shipping.application.TrackingLogResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/shippings")
class ShippingController(
    private val shippingService: ShippingService,
) {

    /**
     * 송장 등록 (배송 생성).
     * SELLER 권한 필요.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun registerShipment(@RequestBody @Valid request: RegisterShipmentRequest): ApiResponse<ShipmentResponse> {
        val response = shippingService.registerShipment(request)
        return ApiResponse.created(response)
    }

    /**
     * 배송 상세 조회.
     */
    @GetMapping("/{id}")
    fun getShipment(@PathVariable id: Long): ApiResponse<ShipmentResponse> {
        val response = shippingService.findById(id)
        return ApiResponse.ok(response)
    }

    /**
     * orderId 기반 배송 조회 (PD-44).
     */
    @GetMapping
    fun getShipmentByOrderId(@RequestParam orderId: Long): ApiResponse<ShipmentResponse> {
        val response = shippingService.findByOrderId(orderId)
        return ApiResponse.ok(response)
    }

    /**
     * 배송 추적 조회 (Redis 캐시 5분 TTL, PD-41).
     */
    @GetMapping("/{id}/tracking")
    fun getTrackingLogs(@PathVariable id: Long): ApiResponse<List<TrackingLogResponse>> {
        val response = shippingService.getTrackingLogs(id)
        return ApiResponse.ok(response)
    }
}
