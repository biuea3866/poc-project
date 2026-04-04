package com.closet.bff.presentation

import com.closet.bff.dto.AddCartItemRequest
import com.closet.bff.dto.UpdateQuantityRequest
import com.closet.bff.facade.CartBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bff/cart")
class BffCartController(
    private val cartFacade: CartBffFacade,
) {
    @PostMapping("/items")
    fun addCartItem(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: AddCartItemRequest,
    ) = ApiResponse.created(cartFacade.addCartItem(memberId, request))

    @PutMapping("/items/{itemId}")
    fun updateCartItem(
        @PathVariable itemId: Long,
        @RequestBody request: UpdateQuantityRequest,
    ) = ApiResponse.ok(cartFacade.updateCartItemQuantity(itemId, request.quantity))

    @DeleteMapping("/items/{itemId}")
    fun removeCartItem(@PathVariable itemId: Long): ResponseEntity<Void> {
        cartFacade.removeCartItem(itemId)
        return ResponseEntity.noContent().build()
    }
}
