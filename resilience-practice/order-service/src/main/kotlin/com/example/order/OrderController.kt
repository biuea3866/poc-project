package com.example.order

import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
) {

    @RateLimiter(name = "orderRl")
    @PostMapping
    fun place(@RequestBody req: PlaceOrderRequest): OrderResponse =
        orderService.place(req)

    @ExceptionHandler(RequestNotPermitted::class)
    fun handleRateLimit(ex: RequestNotPermitted): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(mapOf("error" to "rate limit exceeded", "name" to ex.message.orEmpty()))
}
