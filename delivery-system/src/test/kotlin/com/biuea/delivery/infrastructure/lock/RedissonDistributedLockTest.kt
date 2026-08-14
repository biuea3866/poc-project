package com.biuea.delivery.infrastructure.lock

import com.biuea.delivery.infrastructure.persistence.AcceptStrategyTestContext
import com.biuea.delivery.support.RedisCommandCounter
import com.biuea.delivery.support.RedisContainer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.redisson.Redisson
import org.redisson.api.RLock
import org.redisson.config.Config
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Redisson `RLock` 이 직접 구현 스핀락과 다른 지점만 검증한다.
 *
 * 스핀락(RedisDistributedLock)은 UUID 토큰으로 소유자를 구분하고 20ms 간격 폴링으로 대기한다.
 * Redisson 은 소유자를 `{클라이언트ID}:{스레드ID}` 로 잡아 **재진입**을 지원하고, 대기는
 * `redisson_lock__channel:{락이름}` 구독 + 블로킹으로 처리하며, leaseTime 을 주지 않으면
 * **워치독**이 TTL 을 계속 갱신한다.
 *
 * 마지막 케이스(대기 구간 Redis 명령 수)가 이 PoC 의 핵심 질문 —
 * "스핀락의 낮은 처리량이 20ms 폴링 탓인가" — 에 대한 직접 근거다.
 */
class RedissonDistributedLockTest : BehaviorSpec({

    val redissonClient = AcceptStrategyTestContext.redissonClient
    val redisDistributedLock = AcceptStrategyTestContext.redisDistributedLock
    val redisCommandCounter = AcceptStrategyTestContext.redisCommandCounter

    // 기본 워치독은 30초다. 그대로 검증하면 테스트가 30초를 넘기므로 1초로 줄인 클라이언트를 따로 띄운다.
    // 워치독은 만료 시간의 1/3(약 333ms) 주기로 TTL 을 갱신한다.
    val shortWatchdogClient = Redisson.create(
        Config().apply {
            useSingleServer().setAddress("redis://${RedisContainer.host}:${RedisContainer.port}")
            setLockWatchdogTimeout(WATCHDOG_TIMEOUT.toMillis())
        },
    )
    afterSpec { shortWatchdogClient.shutdown() }

    Given("같은 스레드가 이미 잡고 있는 Redisson 락") {
        val lockName = "lock:redisson:test-reentrant"

        When("같은 스레드가 같은 락을 한 번 더 잡으면") {
            val observation = onSingleThread {
                val lock = redissonClient.getLock(lockName)
                val firstAcquired = lock.tryLock(1, TimeUnit.SECONDS)
                val secondAcquired = lock.tryLock(1, TimeUnit.SECONDS)
                val holdCountAfterSecondAcquire = lock.holdCount
                lock.unlock()
                val lockedAfterFirstUnlock = lock.isLocked
                lock.unlock()
                ReentrantLockObservation(
                    firstAcquired = firstAcquired,
                    secondAcquired = secondAcquired,
                    holdCountAfterSecondAcquire = holdCountAfterSecondAcquire,
                    lockedAfterFirstUnlock = lockedAfterFirstUnlock,
                    lockedAfterSecondUnlock = lock.isLocked,
                )
            }

            Then("두 번째 획득도 성공하고 보유 횟수가 2가 된다") {
                observation.firstAcquired shouldBe true
                observation.secondAcquired shouldBe true
                observation.holdCountAfterSecondAcquire shouldBe 2
            }

            Then("획득한 횟수만큼 해제해야 락이 풀린다") {
                observation.lockedAfterFirstUnlock shouldBe true
                observation.lockedAfterSecondUnlock shouldBe false
            }
        }
    }

    Given("다른 스레드가 잡고 있는 Redisson 락") {
        val lockName = "lock:redisson:test-not-owner-unlock"
        val ownerLock = redissonClient.getLock(lockName)
        val lockHolder = LockHolderThread(ownerLock)
        lockHolder.acquireAndHold()

        When("소유자가 아닌 스레드가 해제를 시도하면") {
            val thrownFailure = onSingleThread {
                runCatching { redissonClient.getLock(lockName).unlock() }.exceptionOrNull()
            }

            Then("해제에 실패하고 예외를 받는다") {
                thrownFailure.shouldBeInstanceOf<IllegalMonitorStateException>()
            }

            Then("락은 원래 소유자에게 그대로 남아 있다") {
                ownerLock.isLocked shouldBe true
                lockHolder.releaseAndJoin() shouldBe true
                ownerLock.isLocked shouldBe false
            }
        }
    }

    Given("워치독 만료 시간을 ${WATCHDOG_TIMEOUT.toMillis()}ms 로 줄인 Redisson 클라이언트") {
        val criticalSectionMillis = WATCHDOG_TIMEOUT.toMillis() * CRITICAL_SECTION_TIMES_WATCHDOG

        When("leaseTime 을 주지 않고 락을 잡아 워치독 만료 시간보다 오래 점유하면") {
            val watchdogLock = shortWatchdogClient.getLock("lock:redisson:test-watchdog-renewed")
            val observation = onSingleThread {
                watchdogLock.tryLock(1, TimeUnit.SECONDS)
                Thread.sleep(criticalSectionMillis)
                LeaseObservation(watchdogLock.isLocked, watchdogLock.remainTimeToLive())
                    // 관측 직후 해제해 갱신 태스크를 멈춘다. 살려두면 뒤의 명령 수 측정에 갱신 명령이 섞인다.
                    .also { watchdogLock.unlock() }
            }

            Then("워치독이 TTL 을 갱신해 락이 만료되지 않는다") {
                observation.locked shouldBe true
                (observation.remainTimeToLiveMillis > 0) shouldBe true
            }

            Then("남은 TTL 이 워치독 만료 시간 수준으로 되살아나 있다") {
                // ${criticalSectionMillis}ms 를 점유했는데 남은 TTL 이 만료 시간에 가깝다 = 갱신이 실제로 돌았다.
                (observation.remainTimeToLiveMillis > WATCHDOG_TIMEOUT.toMillis() / 2) shouldBe true
            }
        }

        When("같은 시간을 leaseTime 을 명시해 잡고 점유하면") {
            // 대조군. 고정 TTL 로 잡으면 임계 구역이 TTL 보다 길어질 때 락이 먼저 풀린다.
            val fixedLeaseLock = shortWatchdogClient.getLock("lock:redisson:test-fixed-lease-expires")
            val observation = onSingleThread {
                fixedLeaseLock.tryLock(1, WATCHDOG_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                Thread.sleep(criticalSectionMillis)
                LeaseObservation(fixedLeaseLock.isLocked, fixedLeaseLock.remainTimeToLive())
            }

            Then("고정 TTL 은 갱신되지 않아 임계 구역 도중에 락이 풀린다") {
                observation.locked shouldBe false
            }
        }
    }

    Given("선점자가 ${LOCK_HOLD.toMillis()}ms 동안 락을 잡고 대기자 ${WAITER_COUNT}명이 동시에 붙는 상황") {
        val spinLockWaitPhase = redisCommandCounter.countCommandsWhileWaiting {
            val lockName = "lock:redisson:test-command-count-spin"
            redisDistributedLock.tryAcquire(lockName, LEASE_TTL, LockWaitPolicy.waitUpTo(WAIT_TIMEOUT, POLLING_INTERVAL))
                ?.let { token -> LockRelease { redisDistributedLock.release(lockName, token) } }
        }
        val redissonWaitPhase = redisCommandCounter.countCommandsWhileWaiting {
            val lock = redissonClient.getLock("lock:redisson:test-command-count-pubsub")
            if (lock.tryLock(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) LockRelease { lock.unlock() } else null
        }

        Then("두 방식 모두 대기자 전원이 락을 통과한다") {
            // 명령 수가 적은 이유가 "아무도 락을 못 잡아서" 가 아님을 먼저 못 박는다.
            spinLockWaitPhase.passedWaiterCount shouldBe WAITER_COUNT
            redissonWaitPhase.passedWaiterCount shouldBe WAITER_COUNT
        }

        Then("pub/sub 대기가 ${POLLING_INTERVAL.toMillis()}ms 폴링보다 적은 Redis 명령으로 같은 대기를 처리한다") {
            println("## 대기 구간 Redis 명령 수 (대기자 ${WAITER_COUNT}명, 선점 ${LOCK_HOLD.toMillis()}ms)")
            println("- 스핀락(${POLLING_INTERVAL.toMillis()}ms 폴링) ${spinLockWaitPhase.commandCount}건 :: ${spinLockWaitPhase.breakdown}")
            println("- Redisson(pub/sub)  ${redissonWaitPhase.commandCount}건 :: ${redissonWaitPhase.breakdown}")
            (redissonWaitPhase.commandCount < spinLockWaitPhase.commandCount) shouldBe true
        }

        Then("폴링 비용은 대기자가 반복해 던지는 SET 이 지배한다") {
            // 대기자 ${WAITER_COUNT}명 × (${LOCK_HOLD.toMillis()}ms / ${POLLING_INTERVAL.toMillis()}ms) 만큼의
            // SET NX 가 그대로 서버 부하다 — 스핀락 명령의 과반이 "아직 안 풀렸나" 를 되묻는 데 쓰인다.
            (spinLockWaitPhase.callCountOf("set") * 2 > spinLockWaitPhase.commandCount) shouldBe true
            spinLockWaitPhase.callCountOf("publish") shouldBe 0L
        }

        Then("pub/sub 대기는 폴링 SET 없이 해제 알림으로만 재시도한다") {
            // Redisson 의 SET 은 폴링이 아니다 — 해제 스크립트가 멱등 처리용 latch 를 한 번 쓰는 것뿐이라
            // 해제 횟수(대기자 + 선점자)를 넘지 않는다. 대기 중에는 SET 을 던지지 않는다.
            (redissonWaitPhase.callCountOf("set") <= WAITER_COUNT + 1) shouldBe true
            // 대기자가 깨어난 경로가 PUBLISH 였다는 직접 증거.
            // 이 카운터는 스핀락이 쓰는 Redis(StringRedisTemplate 경유)를 읽는다 — PUBLISH 가 잡힌다는 건
            // Redisson 이 스핀락과 같은 Redis 를 본다는 뜻이기도 하다. 접속 좌표 설정이 틀어지면 여기서 걸린다.
            (redissonWaitPhase.callCountOf("publish") > 0) shouldBe true
        }
    }
})

/** 락 해제 동작. 획득 실패는 null 로 표현한다. */
private fun interface LockRelease {
    fun release()
}

private data class ReentrantLockObservation(
    val firstAcquired: Boolean,
    val secondAcquired: Boolean,
    val holdCountAfterSecondAcquire: Int,
    val lockedAfterFirstUnlock: Boolean,
    val lockedAfterSecondUnlock: Boolean,
)

private data class LeaseObservation(val locked: Boolean, val remainTimeToLiveMillis: Long)

private data class WaitPhaseCommandCount(
    val callCountsByCommand: Map<String, Long>,
    val passedWaiterCount: Int,
    val breakdown: String,
) {
    val commandCount: Long get() = callCountsByCommand.values.sum()

    fun callCountOf(command: String): Long = callCountsByCommand.getOrDefault(command, 0L)
}

private val WATCHDOG_TIMEOUT: Duration = Duration.ofSeconds(1)
private const val CRITICAL_SECTION_TIMES_WATCHDOG = 3L
private val LEASE_TTL: Duration = Duration.ofSeconds(30)
private val WAIT_TIMEOUT: Duration = Duration.ofSeconds(30)
private val POLLING_INTERVAL: Duration = Duration.ofMillis(20)
private val LOCK_HOLD: Duration = Duration.ofMillis(500)
private const val WAITER_COUNT = 20
private const val TASK_TIMEOUT_SECONDS = 60L

/**
 * Redisson 락의 소유자는 `{클라이언트ID}:{스레드ID}` 다. 재진입·소유권 검증은 스레드가 고정돼야 의미가 있는데,
 * Kotest 는 테스트 블록을 코루틴으로 실행해 스레드 고정을 보장하지 않는다. 전용 스레드에서 돌려 고정한다.
 */
private fun <T> onSingleThread(block: () -> T): T {
    val executor = Executors.newSingleThreadExecutor()
    return try {
        executor.submit(block).get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } finally {
        executor.shutdownNow()
    }
}

/** 락을 잡은 스레드를 살려둔 채로 다른 스레드의 해제 시도를 관찰하기 위한 홀더. */
private class LockHolderThread(private val lock: RLock) {
    private val acquiredGate = CountDownLatch(1)
    private val releaseGate = CountDownLatch(1)

    @Volatile
    private var released = false

    private val holderThread = Thread {
        lock.tryLock(1, TimeUnit.SECONDS)
        acquiredGate.countDown()
        releaseGate.await()
        lock.unlock()
        released = true
    }

    fun acquireAndHold() {
        holderThread.start()
        acquiredGate.await(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    fun releaseAndJoin(): Boolean {
        releaseGate.countDown()
        holderThread.join(TASK_TIMEOUT_SECONDS * MILLIS_PER_SECOND)
        return released
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1_000L
    }
}

/**
 * 선점자가 락을 [LOCK_HOLD] 동안 잡고, 그 사이 대기자 [WAITER_COUNT] 명이 동시에 붙는다.
 * 선점 획득은 측정 구간 밖에 두고, 대기자가 붙어 락을 인계받는 구간의 명령 수만 센다.
 */
private fun RedisCommandCounter.countCommandsWhileWaiting(acquire: () -> LockRelease?): WaitPhaseCommandCount {
    val ownerRelease = requireNotNull(acquire()) { "선점자가 락을 잡지 못했다 — 측정 전제가 깨졌다" }
    var passedWaiterCount = 0
    val counted = countCallsByCommandDuring {
        val executor = Executors.newFixedThreadPool(WAITER_COUNT)
        val startGate = CountDownLatch(1)
        try {
            val waiters = (1..WAITER_COUNT).map {
                executor.submit<Boolean> {
                    startGate.await()
                    val release = acquire() ?: return@submit false
                    release.release()
                    true
                }
            }
            startGate.countDown()
            Thread.sleep(LOCK_HOLD.toMillis())
            ownerRelease.release()
            passedWaiterCount = waiters.count { it.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }
    return WaitPhaseCommandCount(counted.callCountsByCommand, passedWaiterCount, counted.formatted())
}
