package com.example.cachepractice.redis.concurrency

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*

/**
 * Redis 동시성 문제 해결 예제 API
 */
@RestController
@RequestMapping("/api/redis/concurrency")
@Profile("redis")
class RedisConcurrencyController(
    private val concurrencyService: RedisConcurrencyService
) {

    /**
     * MULTI/EXEC 예제
     * POST /api/redis/concurrency/multi-exec
     */
    @PostMapping("/multi-exec")
    fun testMultiExec(@RequestBody request: MultiExecRequest): Map<String, Any> {
        val success = concurrencyService.executeWithMultiExec(
            request.key1,
            request.key2,
            request.value1,
            request.value2
        )

        return mapOf(
            "method" to "MULTI/EXEC",
            "success" to success,
            "description" to "여러 명령어를 하나의 트랜잭션으로 묶어 순차 실행"
        )
    }

    /**
     * WATCH 예제
     * POST /api/redis/concurrency/watch
     */
    @PostMapping("/watch")
    fun testWatch(@RequestBody request: WatchRequest): Map<String, Any> {
        // 초기 잔액 설정
        concurrencyService.setBalance(request.balanceKey, request.initialBalance)

        val success = concurrencyService.executeWithWatch(
            request.balanceKey,
            request.decrementAmount
        )

        val finalBalance = concurrencyService.getBalance(request.balanceKey)

        return mapOf(
            "method" to "WATCH",
            "success" to success,
            "initialBalance" to request.initialBalance,
            "decrementAmount" to request.decrementAmount,
            "finalBalance" to finalBalance,
            "description" to "낙관적 락을 이용한 트랜잭션 (키가 변경되면 실패)"
        )
    }

    /**
     * Lua Script 예제 - 재고 차감
     * POST /api/redis/concurrency/lua-script
     */
    @PostMapping("/lua-script")
    fun testLuaScript(@RequestBody request: LuaScriptRequest): Map<String, Any> {
        // 초기 재고 설정
        concurrencyService.setBalance(request.stockKey, request.initialStock)

        val success = concurrencyService.executeWithLuaScript(
            request.stockKey,
            request.quantity
        )

        val finalStock = concurrencyService.getBalance(request.stockKey)

        return mapOf(
            "method" to "Lua Script",
            "success" to success,
            "initialStock" to request.initialStock,
            "requestedQuantity" to request.quantity,
            "finalStock" to finalStock,
            "description" to "Lua 스크립트를 원자적으로 실행하여 재고 차감"
        )
    }

    /**
     * Lua Script 예제 - 잔액 이체
     * POST /api/redis/concurrency/lua-transfer
     */
    @PostMapping("/lua-transfer")
    fun testLuaTransfer(@RequestBody request: TransferRequest): Map<String, Any> {
        // 초기 잔액 설정
        concurrencyService.setBalance(request.fromKey, request.fromBalance)
        concurrencyService.setBalance(request.toKey, request.toBalance)

        val success = concurrencyService.transferWithLuaScript(
            request.fromKey,
            request.toKey,
            request.amount
        )

        val finalFromBalance = concurrencyService.getBalance(request.fromKey)
        val finalToBalance = concurrencyService.getBalance(request.toKey)

        return mapOf(
            "method" to "Lua Script Transfer",
            "success" to success,
            "from" to mapOf(
                "key" to request.fromKey,
                "initialBalance" to request.fromBalance,
                "finalBalance" to finalFromBalance
            ),
            "to" to mapOf(
                "key" to request.toKey,
                "initialBalance" to request.toBalance,
                "finalBalance" to finalToBalance
            ),
            "transferAmount" to request.amount,
            "description" to "Lua 스크립트로 여러 키를 원자적으로 업데이트"
        )
    }

    /**
     * 키 삭제
     * DELETE /api/redis/concurrency/{key}
     */
    @DeleteMapping("/{key}")
    fun deleteKey(@PathVariable key: String): Map<String, String> {
        concurrencyService.deleteKey(key)
        return mapOf("message" to "Key deleted: $key")
    }
}

data class MultiExecRequest(
    val key1: String,
    val key2: String,
    val value1: String,
    val value2: String
)

data class WatchRequest(
    val balanceKey: String,
    val initialBalance: Int,
    val decrementAmount: Int
)

data class LuaScriptRequest(
    val stockKey: String,
    val initialStock: Int,
    val quantity: Int
)

data class TransferRequest(
    val fromKey: String,
    val toKey: String,
    val fromBalance: Int,
    val toBalance: Int,
    val amount: Int
)
