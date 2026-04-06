package com.closet.bff.client

import com.closet.bff.dto.AddCartItemRequest
import com.closet.bff.dto.CartResponse
import com.closet.bff.dto.OrderResponse
import com.closet.bff.dto.PageResponse
import com.closet.common.response.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "order-service", url = "\${service.order.url}")
interface OrderServiceClient {
    @GetMapping("/orders")
    fun getOrders(
        @RequestParam memberId: Long,
        @RequestParam page: Int,
        @RequestParam size: Int,
    ): ApiResponse<PageResponse<OrderResponse>>

    @GetMapping("/orders/{id}")
    fun getOrder(
        @PathVariable id: Long,
    ): ApiResponse<OrderResponse>

    @PostMapping("/orders")
    fun createOrder(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: Any,
    ): ApiResponse<OrderResponse>

    @PostMapping("/orders/{id}/cancel")
    fun cancelOrder(
        @PathVariable id: Long,
        @RequestBody request: Any,
    ): ApiResponse<OrderResponse>

    @GetMapping("/carts")
    fun getCart(
        @RequestHeader("X-Member-Id") memberId: Long,
    ): ApiResponse<CartResponse>

    @PostMapping("/carts/items")
    fun addCartItem(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: AddCartItemRequest,
    ): ApiResponse<Any>

    @PutMapping("/carts/items/{itemId}")
    fun updateCartItemQuantity(
        @PathVariable itemId: Long,
        @RequestBody request: Any,
    ): ApiResponse<Any>

    @DeleteMapping("/carts/items/{itemId}")
    fun removeCartItem(
        @PathVariable itemId: Long,
    )
}
