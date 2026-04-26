package com.example.order

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

@Component
class PaymentClient(
    @Qualifier("paymentWebClient") private val webClient: WebClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "paymentCb", fallbackMethod = "fallback")
    @TimeLimiter(name = "paymentTl")
    fun pay(req: PaymentRequest): CompletableFuture<PaymentResponse> =
        webClient.post()
            .uri("/payments")
            .bodyValue(req)
            .retrieve()
            .bodyToMono(PaymentResponse::class.java)
            .toFuture()

    fun fallback(req: PaymentRequest, ex: Throwable): CompletableFuture<PaymentResponse> {
        val cause = (ex as? CompletionException)?.cause ?: ex
        log.warn("payment fallback for orderId={} cause={}", req.orderId, cause.javaClass.simpleName)
        return CompletableFuture.completedFuture(
            PaymentResponse(
                paymentId = "FALLBACK",
                orderId = req.orderId,
                amount = req.amount,
                status = "PENDING",
            )
        )
    }
}

data class PaymentRequest(val orderId: String, val amount: Long)
data class PaymentResponse(val paymentId: String, val orderId: String, val amount: Long, val status: String)
