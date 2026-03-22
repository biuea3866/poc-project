package com.closet.bff.client

import com.closet.bff.dto.CartResponse
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
}
