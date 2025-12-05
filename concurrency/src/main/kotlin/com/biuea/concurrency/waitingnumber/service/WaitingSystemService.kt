package com.biuea.concurrency.waitingnumber.service

import com.biuea.concurrency.waitingnumber.domain.WaitingNumberEntity
import com.biuea.concurrency.waitingnumber.domain.WaitingNumberResult
import com.biuea.concurrency.waitingnumber.repository.WaitingNumberRepository
import org.redisson.api.RedissonClient
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.concurrent.TimeUnit

@Service
class WaitingSystemService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val waitingNumberRepository: WaitingNumberRepository,
    private val productWaitingInitializer: ProductWaitingInitializer,
    private val redissonClient: RedissonClient
) {
    @Transactional
    fun issueWaitingNumber(productId: Long, userId: Long): WaitingNumberResult {
        val key = "${WAITING_NUMBER_KEY_PREFIX}${productId}"

        // redis에 대기번호가 존재하지 않는다면 캐시를 업데이트한다.
        if (redisTemplate.hasKey(key).not()) {
            initialize(productId, key)
        }

        // INCR 요청으로 원자적으로 대기번호가 발급된다.
        val waitingNumber = redisTemplate.opsForValue().increment(key)
            ?: throw IllegalStateException()

        // 유저에게 대기번호를 발급한다.
        val entity = waitingNumberRepository.save(
            WaitingNumberEntity(
                productId = productId,
                waitingNumber = "${productId}-${waitingNumber}",
                userId = userId
            )
        )

        return WaitingNumberResult(
            productId = productId,
            waitingNumber = "${productId}-${waitingNumber}",
            userId = userId,
            timestamp = entity.createdAt
        )
    }

    // redis에 대기 번호가 없다면, mysql로부터 데이터를 읽어온다.
    // mysql에 멀티 스레드가 동시 접근할 수 있기 때문에 원천 데이터를 읽을 때 분산 락으로 원자성을 보장한다.
    private fun initialize(productId: Long, key: String) {
        // 상품에 접근하는 멀티 스레드를 차단한다.
        val lockKey = "lock:init:$productId"
        val lock = redissonClient.getLock(lockKey)
        var lockAcquired = false

        try {
            // 1초 동안 대기, 10초 후에 자동 해제
            lockAcquired = lock.tryLock(1, 10, TimeUnit.SECONDS)

            if (redisTemplate.hasKey(key)) {
                return  // 이미 다른 스레드가 초기화함
            }

            // Redis 재초기화: 유저에게 발급된 대기번호 중 최댓값을 찾아서 복원
            val currentNumber = waitingNumberRepository.findMaxWaitingNumberByProductId(productId)
                ?.split("-")?.get(1)?.toIntOrNull()?: 1

            // Redis에 현재 대기 번호를 설정 (TTL 10초)
            redisTemplate.opsForValue().set(key, currentNumber, Duration.ofSeconds(10))
        } finally {
            if (lockAcquired) {
                lock.unlock()
            }
        }
    }

    private fun formatWaitingNumber(productId: Int, waitingNumber: Int): String {
        return "${productId}-${waitingNumber.toString().padStart(6, '0')}"
    }

    companion object {
        private const val WAITING_NUMBER_KEY_PREFIX = "waiting:number:"
    }
}