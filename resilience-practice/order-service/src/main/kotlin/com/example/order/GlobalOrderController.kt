package com.example.order

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Redis 기반 글로벌 RateLimiter가 적용된 주문 진입점.
 *
 * Resilience4j 인스턴스 로컬 RateLimiter와 달리, 모든 인스턴스가 같은 카운터를 공유한다.
 *  - 클러스터 전체에 limit-per-second를 일괄 적용
 *  - 인스턴스 N대로 스케일아웃해도 총 허용량은 동일
 */
@RestController
@RequestMapping("/orders/global")
class GlobalOrderController(
    private val orderService: OrderService,
    private val redisRateLimiter: RedisRateLimiter,
) {

    @PostMapping
    fun place(@RequestBody req: PlaceOrderRequest): ResponseEntity<Any> {
        if (!redisRateLimiter.tryAcquire("orders")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                mapOf(
                    "error" to "global rate limit exceeded",
                    "config" to redisRateLimiter.config(),
                )
            )
        }
        return ResponseEntity.ok(orderService.place(req))
    }
}
