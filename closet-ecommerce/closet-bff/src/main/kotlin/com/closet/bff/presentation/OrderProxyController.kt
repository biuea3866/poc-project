package com.closet.bff.presentation

import com.closet.bff.client.OrderServiceClient
import com.closet.bff.dto.AddCartItemRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class OrderProxyController(
    private val orderClient: OrderServiceClient,
) {

    @PostMapping("/orders")
    fun createOrder(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: Any,
    ) = orderClient.createOrder(memberId, request)

    @GetMapping("/orders")
    fun getOrders(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = orderClient.getOrders(memberId, page, size)

    @GetMapping("/orders/{id}")
    fun getOrder(@PathVariable id: Long) =
        orderClient.getOrder(id)

    @PostMapping("/orders/{id}/cancel")
    fun cancelOrder(@PathVariable id: Long, @RequestBody request: Any) =
        orderClient.cancelOrder(id, request)

    @PostMapping("/carts/items")
    fun addCartItem(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: AddCartItemRequest,
    ) = orderClient.addCartItem(memberId, request)

    @GetMapping("/carts")
    fun getCart(@RequestHeader("X-Member-Id") memberId: Long) =
        orderClient.getCart(memberId)

    @PutMapping("/carts/items/{itemId}")
    fun updateCartItem(@PathVariable itemId: Long, @RequestBody request: Any) =
        orderClient.updateCartItemQuantity(itemId, request)

    @DeleteMapping("/carts/items/{itemId}")
    fun removeCartItem(@PathVariable itemId: Long): ResponseEntity<Void> {
        orderClient.removeCartItem(itemId)
        return ResponseEntity.noContent().build()
    }
}
