package com.example.cachepractice.redis.distributedlock

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*

/**
 * Redis 분산락 예제 API
 */
@RestController
@RequestMapping("/api/redis/distributed-lock")
@Profile("redis")
class RedisDistributedLockController(
    private val lockService: RedisDistributedLockService
) {

    /**
     * 기본 분산락 테스트
     * POST /api/redis/distributed-lock/basic
     */
    @PostMapping("/basic")
    fun testBasicLock(@RequestBody request: LockRequest): Map<String, Any> {
        val result = lockService.executeWithLock(
            lockKey = request.lockKey,
            waitTime = request.waitTime,
            leaseTime = request.leaseTime
        ) {
            // 작업 시뮬레이션
            Thread.sleep(request.taskDuration)
            println("작업 실행 중: ${request.lockKey}")
        }

        return mapOf(
            "lockType" to "Basic Lock",
            "lockKey" to request.lockKey,
            "result" to result,
            "description" to "기본 분산락 - tryLock으로 락 획득 시도"
        )
    }

    /**
     * Fair Lock 테스트
     * POST /api/redis/distributed-lock/fair
     */
    @PostMapping("/fair")
    fun testFairLock(@RequestBody request: LockRequest): Map<String, Any> {
        val result = lockService.executeWithFairLock(
            lockKey = request.lockKey,
            waitTime = request.waitTime,
            leaseTime = request.leaseTime
        ) {
            Thread.sleep(request.taskDuration)
            println("Fair Lock 작업 실행 중: ${request.lockKey}")
        }

        return mapOf(
            "lockType" to "Fair Lock",
            "lockKey" to request.lockKey,
            "result" to result,
            "description" to "공정한 락 - 요청 순서대로 락 획득"
        )
    }

    /**
     * Read Lock 테스트
     * POST /api/redis/distributed-lock/read
     */
    @PostMapping("/read")
    fun testReadLock(@RequestBody request: LockRequest): Map<String, Any> {
        val result = lockService.executeWithReadLock(request.lockKey) {
            Thread.sleep(request.taskDuration)
            println("Read Lock 작업 실행 중: ${request.lockKey}")
        }

        return mapOf(
            "lockType" to "Read Lock",
            "lockKey" to request.lockKey,
            "result" to result,
            "description" to "읽기 락 - 여러 스레드가 동시 접근 가능"
        )
    }

    /**
     * Write Lock 테스트
     * POST /api/redis/distributed-lock/write
     */
    @PostMapping("/write")
    fun testWriteLock(@RequestBody request: LockRequest): Map<String, Any> {
        val result = lockService.executeWithWriteLock(request.lockKey) {
            Thread.sleep(request.taskDuration)
            println("Write Lock 작업 실행 중: ${request.lockKey}")
        }

        return mapOf(
            "lockType" to "Write Lock",
            "lockKey" to request.lockKey,
            "result" to result,
            "description" to "쓰기 락 - 배타적 접근만 가능"
        )
    }

    /**
     * 재고 초기화
     * POST /api/redis/distributed-lock/stock/init
     */
    @PostMapping("/stock/init")
    fun initializeStock(@RequestBody request: StockInitRequest): Map<String, Any> {
        lockService.initializeStock(request.productId, request.stock)

        return mapOf(
            "productId" to request.productId,
            "initialStock" to request.stock,
            "message" to "재고 초기화 완료"
        )
    }

    /**
     * 재고 차감 (분산락 사용)
     * POST /api/redis/distributed-lock/stock/decrease
     */
    @PostMapping("/stock/decrease")
    fun decreaseStock(@RequestBody request: StockDecreaseRequest): Map<String, Any> {
        val result = lockService.decreaseStock(request.productId, request.quantity)

        return mapOf(
            "lockType" to "Distributed Lock for Stock",
            "result" to result,
            "description" to "분산락을 이용한 재고 차감 - 동시성 제어"
        )
    }

    /**
     * 현재 재고 조회
     * GET /api/redis/distributed-lock/stock/{productId}
     */
    @GetMapping("/stock/{productId}")
    fun getStock(@PathVariable productId: Long): Map<String, Any> {
        val stock = lockService.getStock(productId)

        return mapOf(
            "productId" to productId,
            "currentStock" to stock
        )
    }

    /**
     * 재고 초기화
     * DELETE /api/redis/distributed-lock/stock
     */
    @DeleteMapping("/stock")
    fun clearStock(): Map<String, String> {
        lockService.clearStock()
        return mapOf("message" to "모든 재고 초기화 완료")
    }
}

data class LockRequest(
    val lockKey: String,
    val waitTime: Long = 5,
    val leaseTime: Long = 10,
    val taskDuration: Long = 1000
)

data class StockInitRequest(
    val productId: Long,
    val stock: Int
)

data class StockDecreaseRequest(
    val productId: Long,
    val quantity: Int
)
