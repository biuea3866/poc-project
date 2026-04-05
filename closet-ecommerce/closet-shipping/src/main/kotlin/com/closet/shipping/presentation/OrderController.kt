package com.closet.shipping.presentation

import com.closet.common.auth.MemberRole
import com.closet.common.auth.RoleRequired
import com.closet.common.response.ApiResponse
import com.closet.shipping.application.ShipmentResponse
import com.closet.shipping.application.ShippingService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 주문 관련 Shipping API 컨트롤러 (US-503).
 *
 * POST /api/v1/orders/{orderId}/confirm - 수동 구매확정 (BUYER)
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val shippingService: ShippingService,
) {

    /**
     * 수동 구매확정 (BUYER).
     * 배송 완료 상태에서만 가능.
     * order.confirmed Kafka 이벤트를 발행한다.
     */
    @PostMapping("/{orderId}/confirm")
    @RoleRequired(MemberRole.BUYER)
    fun confirmOrder(@PathVariable orderId: Long): ApiResponse<ShipmentResponse> {
        val response = shippingService.confirmOrder(orderId)
        return ApiResponse.ok(response)
    }
}
