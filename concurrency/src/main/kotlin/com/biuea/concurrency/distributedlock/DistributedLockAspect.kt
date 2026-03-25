package com.biuea.concurrency.distributedlock

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 분산 락 AOP
 *
 * try { joinPoint.proceed() } finally { lock.release() }
 * → 어노테이션이 지정된 함수가 종료되면 락이 해제됨
 *
 * wait: 락을 획득하려는 클라이언트 대기 시간, 초과 시 락 획득 실패 예외
 * lease: 락 획득 후 최대 점유 시간, 초과 시 강제 해제 (동시성 문제 가능)
 *
 * 핵심 규칙:
 * - lease > API max latency (반드시) — 위반 시 동시성 문제 발생
 * - wait 설정은 비즈니스 요구에 따라:
 *   - 따닥 방지: wait = 0 (즉시 튕김)
 *   - 순차 처리: wait > API max latency
 *   - 적절한 대기: wait = API p95 latency
 */
@Aspect
@Component
class DistributedLockAspect(
    private val redissonClient: RedissonClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(distributedLock)")
    fun around(joinPoint: ProceedingJoinPoint, distributedLock: DistributedLock): Any? {
        val key = resolveKey(joinPoint, distributedLock.key)
        val lock = redissonClient.getLock("lock:$key")

        val acquired = lock.tryLock(
            distributedLock.waitTime,
            distributedLock.leaseTime,
            TimeUnit.SECONDS,
        )

        if (!acquired) {
            log.warn("[DistributedLock] 락 획득 실패 — key={}, wait={}s", key, distributedLock.waitTime)
            throw DistributedLockAcquireFailedException(key)
        }

        log.info("[DistributedLock] 락 획득 — key={}, lease={}s", key, distributedLock.leaseTime)

        try {
            return joinPoint.proceed()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                log.info("[DistributedLock] 락 해제 — key={}", key)
            } else {
                log.warn("[DistributedLock] 락이 이미 만료됨 (lease 초과) — key={}", key)
            }
        }
    }

    /**
     * SpEL 키 해석 (단순 구현 — 파라미터 이름 기반)
     * 예: key = "order:{orderId}" → "order:123"
     */
    private fun resolveKey(joinPoint: ProceedingJoinPoint, keyExpression: String): String {
        val signature = joinPoint.signature as MethodSignature
        val paramNames = signature.parameterNames
        val paramValues = joinPoint.args

        var resolved = keyExpression
        paramNames.forEachIndexed { index, name ->
            resolved = resolved.replace("{$name}", paramValues[index]?.toString() ?: "null")
        }
        return resolved
    }
}

class DistributedLockAcquireFailedException(key: String) :
    RuntimeException("분산 락 획득에 실패했습니다. key=$key (이미 처리 중인 요청입니다)")
