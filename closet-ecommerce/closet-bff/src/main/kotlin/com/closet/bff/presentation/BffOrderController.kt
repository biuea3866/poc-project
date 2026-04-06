package com.closet.bff.presentation

import com.closet.bff.dto.CancelRequest
import com.closet.bff.dto.ConfirmPaymentBffRequest
import com.closet.bff.dto.CreateOrderBffRequest
import com.closet.bff.facade.OrderBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bff")
class BffOrderController(
    private val orderFacade: OrderBffFacade,
) {
    @GetMapping("/orders/{id}")
    fun getOrderDetail(
        @PathVariable id: Long,
    ) = ApiResponse.ok(orderFacade.getOrderDetail(id))

    @GetMapping("/checkout")
    fun getCheckout(
        @RequestHeader("X-Member-Id") memberId: Long,
    ) = ApiResponse.ok(orderFacade.getCheckout(memberId))

    @PostMapping("/orders")
    fun placeOrder(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: CreateOrderBffRequest,
    ) = ApiResponse.created(orderFacade.placeOrder(memberId, request))

    @PostMapping("/orders/{orderId}/pay")
    fun confirmPayment(
        @RequestBody request: ConfirmPaymentBffRequest,
    ) = ApiResponse.ok(orderFacade.confirmPayment(request))

    @PostMapping("/orders/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: Long,
        @RequestBody request: CancelRequest,
    ) = ApiResponse.ok(orderFacade.cancelOrder(orderId, request.reason))
}
