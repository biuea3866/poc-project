package com.closet.bff.client

import com.closet.bff.dto.AddCartItemRequest
import com.closet.bff.dto.CartItemResponse
import com.closet.bff.dto.CartResponse
import com.closet.bff.dto.CreateOrderBffRequest
import com.closet.bff.dto.OrderResponse
import com.closet.bff.dto.PageResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class OrderServiceClient(
    @Value("\${service.order.url}") private val baseUrl: String,
) {
    private val webClient: WebClient by lazy {
        WebClient.builder().baseUrl(baseUrl).build()
    }

    fun getOrders(memberId: Long, page: Int, size: Int): Mono<PageResponse<OrderResponse>> {
        return webClient.get()
            .uri { builder ->
                builder.path("/orders")
                    .queryParam("memberId", memberId)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build()
            }
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<PageResponse<OrderResponse>>() {})
    }

    fun getOrder(orderId: Long): Mono<OrderResponse> {
        return webClient.get()
            .uri("/orders/{id}", orderId)
            .retrieve()
            .bodyToMono(OrderResponse::class.java)
    }

    fun getCart(memberId: Long): Mono<CartResponse> {
        return webClient.get()
            .uri("/carts/{memberId}", memberId)
            .retrieve()
            .bodyToMono(CartResponse::class.java)
    }

    fun createOrder(memberId: Long, request: CreateOrderBffRequest): Mono<OrderResponse> {
        return webClient.post()
            .uri("/orders")
            .header("X-Member-Id", memberId.toString())
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OrderResponse::class.java)
    }

    fun cancelOrder(orderId: Long, reason: String): Mono<OrderResponse> {
        return webClient.post()
            .uri("/orders/{id}/cancel", orderId)
            .bodyValue(mapOf("reason" to reason))
            .retrieve()
            .bodyToMono(OrderResponse::class.java)
    }

    fun addCartItem(memberId: Long, request: AddCartItemRequest): Mono<CartItemResponse> {
        return webClient.post()
            .uri("/carts/{memberId}/items", memberId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CartItemResponse::class.java)
    }

    fun updateCartItemQuantity(itemId: Long, quantity: Int): Mono<CartItemResponse> {
        return webClient.put()
            .uri("/carts/items/{itemId}", itemId)
            .bodyValue(mapOf("quantity" to quantity))
            .retrieve()
            .bodyToMono(CartItemResponse::class.java)
    }

    fun removeCartItem(itemId: Long): Mono<Void> {
        return webClient.delete()
            .uri("/carts/items/{itemId}", itemId)
            .retrieve()
            .bodyToMono(Void::class.java)
    }
}
