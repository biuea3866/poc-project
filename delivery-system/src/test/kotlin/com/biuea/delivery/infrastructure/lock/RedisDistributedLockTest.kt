package com.biuea.delivery.infrastructure.lock

import com.biuea.delivery.infrastructure.persistence.AcceptStrategyTestContext
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Duration
import kotlin.concurrent.thread

/**
 * 분산락의 안전성은 "해제"에 달려 있다. GET 후 DEL 하는 2단계 해제는 그 사이 TTL 만료가 끼면
 * 남의 락을 지운다. 토큰 일치 확인과 삭제가 원자적으로 묶이는지 검증한다.
 */
class RedisDistributedLockTest : BehaviorSpec({

    val redisDistributedLock = AcceptStrategyTestContext.redisDistributedLock
    val leaseTtl = Duration.ofSeconds(5)

    Given("이미 선점된 락") {
        val lockName = "lock:order:test-already-held"
        val ownerToken = redisDistributedLock.tryAcquire(lockName, leaseTtl, LockWaitPolicy.failFast())
            .shouldNotBeNull()

        When("다른 클라이언트가 즉시 실패 옵션으로 획득을 시도하면") {
            val otherToken = redisDistributedLock.tryAcquire(lockName, leaseTtl, LockWaitPolicy.failFast())

            Then("대기하지 않고 획득에 실패한다") {
                otherToken shouldBe null
            }
        }

        When("다른 클라이언트가 자기 토큰으로 해제를 시도하면") {
            val released = redisDistributedLock.release(lockName, token = "someone-elses-token")

            Then("락은 풀리지 않고 원래 주인이 유지된다") {
                released shouldBe false
                redisDistributedLock.tryAcquire(lockName, leaseTtl, LockWaitPolicy.failFast()) shouldBe null
            }
        }

        When("원래 주인이 자기 토큰으로 해제하면") {
            val released = redisDistributedLock.release(lockName, ownerToken)

            Then("락이 풀리고 다음 클라이언트가 획득한다") {
                released shouldBe true
                redisDistributedLock.tryAcquire(lockName, leaseTtl, LockWaitPolicy.failFast()).shouldNotBeNull()
            }
        }
    }

    Given("선점자가 곧 해제할 락") {
        val lockName = "lock:order:test-wait-and-acquire"
        val ownerToken = redisDistributedLock.tryAcquire(lockName, leaseTtl, LockWaitPolicy.failFast())
            .shouldNotBeNull()
        val holdMillis = 300L

        When("대기 옵션으로 획득을 시도하면") {
            thread {
                Thread.sleep(holdMillis)
                redisDistributedLock.release(lockName, ownerToken)
            }
            val waitStartedAt = System.nanoTime()
            val acquiredToken = redisDistributedLock.tryAcquire(
                lockName,
                leaseTtl,
                LockWaitPolicy.waitUpTo(Duration.ofSeconds(3), Duration.ofMillis(20)),
            )
            val waitedMillis = (System.nanoTime() - waitStartedAt) / 1_000_000

            Then("선점자가 해제할 때까지 기다렸다가 획득한다") {
                acquiredToken.shouldNotBeNull()
                (waitedMillis >= holdMillis) shouldBe true
            }

            Then("대기 시간이 지나도 못 잡으면 null 을 돌려준다") {
                redisDistributedLock.tryAcquire(
                    lockName,
                    leaseTtl,
                    LockWaitPolicy.waitUpTo(Duration.ofMillis(200), Duration.ofMillis(20)),
                ) shouldBe null
            }
        }
    }

    Given("TTL 이 짧은 락") {
        val lockName = "lock:order:test-ttl-expiry"
        redisDistributedLock.tryAcquire(lockName, Duration.ofMillis(300), LockWaitPolicy.failFast())
            .shouldNotBeNull()

        When("소유자가 해제하지 않은 채 TTL 이 지나면") {
            Thread.sleep(500)

            Then("락이 자동으로 풀려 다음 클라이언트가 획득한다") {
                redisDistributedLock.tryAcquire(lockName, leaseTtl, LockWaitPolicy.failFast()).shouldNotBeNull()
            }
        }
    }
})
