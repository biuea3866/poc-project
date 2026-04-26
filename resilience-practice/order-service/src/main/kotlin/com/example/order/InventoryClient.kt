package com.example.order

import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class InventoryClient(
    @Qualifier("inventoryWebClient") private val webClient: WebClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bulkhead(name = "inventoryBh", type = Bulkhead.Type.SEMAPHORE)
    @CircuitBreaker(name = "inventoryCb", fallbackMethod = "fallback")
    fun reserve(req: ReserveRequest): ReserveResponse =
        webClient.post()
            .uri("/inventory/reserve")
            .bodyValue(req)
            .retrieve()
            .bodyToMono(ReserveResponse::class.java)
            .block() ?: throw IllegalStateException("inventory empty body")

    fun fallback(req: ReserveRequest, ex: Throwable): ReserveResponse {
        log.warn("inventory fallback orderId={} cause={}", req.orderId, ex.javaClass.simpleName)
        return ReserveResponse(
            orderId = req.orderId,
            sku = req.sku,
            quantity = req.quantity,
            status = "DEFERRED",
        )
    }
}

data class ReserveRequest(val orderId: String, val sku: String, val quantity: Int)
data class ReserveResponse(val orderId: String, val sku: String, val quantity: Int, val status: String)
