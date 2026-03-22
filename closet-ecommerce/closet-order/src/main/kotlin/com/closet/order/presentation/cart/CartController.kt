package com.closet.order.presentation.cart

import com.closet.common.response.ApiResponse
import com.closet.order.application.CartService
import com.closet.order.presentation.dto.AddCartItemRequest
import com.closet.order.presentation.dto.CartResponse
import com.closet.order.presentation.dto.UpdateCartItemRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/carts")
class CartController(
    private val cartService: CartService,
) {

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(@RequestBody @Valid request: AddCartItemRequest): ApiResponse<CartResponse> {
        val response = cartService.addItem(request)
        return ApiResponse.created(response)
    }

    @GetMapping
    fun getCart(@RequestParam memberId: Long): ApiResponse<CartResponse> {
        val response = cartService.getCart(memberId)
        return ApiResponse.ok(response)
    }

    @PutMapping("/items/{itemId}")
    fun updateQuantity(
        @PathVariable itemId: Long,
        @RequestBody @Valid request: UpdateCartItemRequest,
    ): ApiResponse<CartResponse> {
        val response = cartService.updateQuantity(itemId, request.quantity)
        return ApiResponse.ok(response)
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeItem(@PathVariable itemId: Long) {
        cartService.removeItem(itemId)
    }
}
