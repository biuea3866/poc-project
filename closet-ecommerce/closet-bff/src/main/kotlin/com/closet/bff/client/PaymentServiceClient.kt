package com.closet.bff.client

import com.closet.bff.dto.PaymentResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class PaymentServiceClient(
    @Value("\${service.payment.url}") private val baseUrl: String,
) {
    private val webClient: WebClient by lazy {
        WebClient.builder().baseUrl(baseUrl).build()
    }

    fun getPaymentByOrderId(orderId: Long): Mono<PaymentResponse> {
        return webClient.get()
            .uri("/payments?orderId={orderId}", orderId)
            .retrieve()
            .bodyToMono(PaymentResponse::class.java)
    }
}
