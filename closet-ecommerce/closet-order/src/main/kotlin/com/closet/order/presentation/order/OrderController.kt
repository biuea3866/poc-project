package com.closet.order.presentation.order

import com.closet.common.response.ApiResponse
import com.closet.order.application.OrderService
import com.closet.order.presentation.dto.CancelOrderRequest
import com.closet.order.presentation.dto.CreateOrderRequest
import com.closet.order.presentation.dto.OrderResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @RequestBody @Valid request: CreateOrderRequest,
    ): ApiResponse<OrderResponse> {
        val response = orderService.createOrder(request)
        return ApiResponse.created(response)
    }

    @GetMapping("/{id}")
    fun getOrder(
        @PathVariable id: Long,
    ): ApiResponse<OrderResponse> {
        val response = orderService.findById(id)
        return ApiResponse.ok(response)
    }

    @GetMapping
    fun getOrders(
        @RequestParam memberId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<OrderResponse>> {
        val response = orderService.findByMemberId(memberId, pageable)
        return ApiResponse.ok(response)
    }

    @PostMapping("/{id}/cancel")
    fun cancelOrder(
        @PathVariable id: Long,
        @RequestBody @Valid request: CancelOrderRequest,
    ): ApiResponse<OrderResponse> {
        val response = orderService.cancelOrder(id, request.reason)
        return ApiResponse.ok(response)
    }
}
