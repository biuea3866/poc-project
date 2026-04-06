package com.closet.common.lock

import com.closet.common.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RLock
import org.redisson.api.RedissonClient

class DistributedLockAspectTest : BehaviorSpec({

    val redissonClient = mockk<RedissonClient>()
    val aspect = DistributedLockAspect(redissonClient)

    Given("분산 락 획득 성공 시") {
        val lock = mockk<RLock>(relaxed = true)
        every { redissonClient.getLock(any()) } returns lock
        every { lock.tryLock(any(), any(), any()) } returns true
        every { lock.isHeldByCurrentThread } returns true

        val joinPoint = mockk<ProceedingJoinPoint>()
        val signature = mockk<MethodSignature>()

        every { joinPoint.signature } returns signature
        every { signature.method } returns DistributedLockAspectTest::class.java.getMethod("toString")
        every { joinPoint.args } returns arrayOf()
        every { joinPoint.proceed() } returns "result"

        val annotation =
            DistributedLock(
                key = "'test:lock:key'",
                waitTime = 5L,
                leaseTime = 3L,
                maxRetries = 3,
            )

        When("around 실행") {
            // 어노테이션의 리터럴 SpEL 키를 사용
            val result = aspect.around(joinPoint, annotation)

            Then("비즈니스 로직이 실행되고 결과를 반환한다") {
                result shouldBe "result"
            }

            Then("락이 해제된다") {
                verify { lock.unlock() }
            }
        }
    }

    Given("분산 락 획득 실패 시 (모든 재시도 실패)") {
        val lock = mockk<RLock>(relaxed = true)
        every { redissonClient.getLock(any()) } returns lock
        every { lock.tryLock(any(), any(), any()) } returns false

        val joinPoint = mockk<ProceedingJoinPoint>()
        val signature = mockk<MethodSignature>()

        every { joinPoint.signature } returns signature
        every { signature.method } returns DistributedLockAspectTest::class.java.getMethod("toString")
        every { joinPoint.args } returns arrayOf()

        val annotation =
            DistributedLock(
                key = "'test:lock:fail'",
                waitTime = 1L,
                leaseTime = 1L,
                maxRetries = 2,
            )

        When("around 실행") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    aspect.around(joinPoint, annotation)
                }
            }

            Then("proceed가 호출되지 않는다") {
                verify(exactly = 0) { joinPoint.proceed() }
            }
        }
    }

    Given("비즈니스 로직 실행 중 예외 발생 시") {
        val lock = mockk<RLock>(relaxed = true)
        every { redissonClient.getLock(any()) } returns lock
        every { lock.tryLock(any(), any(), any()) } returns true
        every { lock.isHeldByCurrentThread } returns true

        val joinPoint = mockk<ProceedingJoinPoint>()
        val signature = mockk<MethodSignature>()

        every { joinPoint.signature } returns signature
        every { signature.method } returns DistributedLockAspectTest::class.java.getMethod("toString")
        every { joinPoint.args } returns arrayOf()
        every { joinPoint.proceed() } throws RuntimeException("비즈니스 로직 오류")

        val annotation =
            DistributedLock(
                key = "'test:lock:error'",
                waitTime = 5L,
                leaseTime = 3L,
                maxRetries = 1,
            )

        When("around 실행") {
            Then("예외가 전파되고 락은 해제된다") {
                shouldThrow<RuntimeException> {
                    aspect.around(joinPoint, annotation)
                }
                verify { lock.unlock() }
            }
        }
    }
})
