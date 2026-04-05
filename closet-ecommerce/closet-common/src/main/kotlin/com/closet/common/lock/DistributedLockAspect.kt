package com.closet.common.lock

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * @DistributedLock 어노테이션을 처리하는 AOP Aspect.
 *
 * Redisson 분산 락을 이용하여 동시성을 제어한다.
 * SpEL 표현식으로 메서드 파라미터 기반의 동적 키를 생성한다.
 */
@Aspect
@Component
class DistributedLockAspect(
    private val redissonClient: RedissonClient,
) {

    private val parser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(distributedLock)")
    fun around(joinPoint: ProceedingJoinPoint, distributedLock: DistributedLock): Any? {
        val lockKey = resolveKey(joinPoint, distributedLock.key)
        val lock = redissonClient.getLock(lockKey)

        var lastException: Exception? = null

        for (attempt in 1..distributedLock.maxRetries) {
            val acquired = try {
                lock.tryLock(distributedLock.waitTime, distributedLock.leaseTime, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "분산 락 획득 중 인터럽트가 발생했습니다. key=$lockKey"
                )
            }

            if (acquired) {
                return try {
                    joinPoint.proceed()
                } finally {
                    if (lock.isHeldByCurrentThread) {
                        lock.unlock()
                        logger.debug { "분산 락 해제: key=$lockKey" }
                    }
                }
            }

            logger.warn { "분산 락 획득 실패 (시도 $attempt/${distributedLock.maxRetries}): key=$lockKey" }
            lastException = BusinessException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "분산 락 획득에 실패했습니다. key=$lockKey"
            )
        }

        throw lastException ?: BusinessException(
            ErrorCode.INTERNAL_SERVER_ERROR,
            "분산 락 획득에 실패했습니다. 잠시 후 다시 시도해주세요. key=$lockKey"
        )
    }

    /**
     * SpEL 표현식을 파싱하여 실제 락 키를 생성한다.
     */
    private fun resolveKey(joinPoint: ProceedingJoinPoint, keyExpression: String): String {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val parameterNames = nameDiscoverer.getParameterNames(method) ?: return keyExpression
        val args = joinPoint.args

        val context = StandardEvaluationContext()
        for (i in parameterNames.indices) {
            context.setVariable(parameterNames[i], args[i])
        }

        return try {
            parser.parseExpression(keyExpression).getValue(context, String::class.java) ?: keyExpression
        } catch (e: Exception) {
            logger.warn { "SpEL 키 파싱 실패, 원본 키 사용: keyExpression=$keyExpression, error=${e.message}" }
            keyExpression
        }
    }
}
