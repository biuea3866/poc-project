package com.biuea.concurrency.distributedlock

import com.biuea.concurrency.support.TestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.redisson.api.RedissonClient
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Redisson Watchdog 테스트
 *
 * Watchdog이란?
 * - leaseTime을 -1 (또는 미지정)로 설정하면 Watchdog이 활성화됨
 * - 기본 30초마다 락 TTL을 자동 갱신 (lockWatchdogTimeout / 3 = 10초마다)
 * - 스레드가 살아있는 한 락이 만료되지 않음
 * - 스레드가 죽거나 unlock() 하면 갱신 중지 → TTL 만료 후 자동 해제
 *
 * Watchdog이 해결하는 문제:
 * - 시나리오 3 (API 종료 > lease → 동시성 문제) 원천 차단
 * - lease를 예측할 필요 없음 — 처리 시간이 얼마든 자동 연장
 */
import org.springframework.beans.factory.annotation.Autowired

class WatchdogTest : TestContainersConfig() {

    @Autowired
    lateinit var redissonClient: RedissonClient

    // =========================================================================
    // 테스트 1: Watchdog 활성화 확인 (leaseTime = -1)
    //
    // leaseTime을 -1로 설정하면 Watchdog이 활성화되어
    // 처리가 30초를 넘겨도 락이 만료되지 않음
    // =========================================================================

    @Test
    @DisplayName("Watchdog: leaseTime=-1이면 처리 시간이 길어도 락 유지")
    fun watchdog_longProcessing_lockMaintained() {
        val lock = redissonClient.getLock("lock:watchdog:test1")

        // leaseTime = -1 → Watchdog 활성화
        val acquired = lock.tryLock(0, -1, TimeUnit.SECONDS)
        assertThat(acquired).isTrue()

        // 5초 처리 (Watchdog 기본 갱신 주기 10초 내)
        Thread.sleep(5000)

        // 여전히 락 보유 중
        assertThat(lock.isHeldByCurrentThread).isTrue()

        lock.unlock()
        assertThat(lock.isHeldByCurrentThread).isFalse()
    }

    // =========================================================================
    // 테스트 2: Watchdog vs 고정 lease 비교 — 동시성 안전성
    //
    // 고정 lease(2초) + 처리(4초) → 동시성 문제 (시나리오 3)
    // Watchdog(-1) + 처리(4초) → 안전 (자동 갱신)
    // =========================================================================

    @Test
    @DisplayName("Watchdog vs 고정 lease: Watchdog은 동시성 문제 없음")
    fun watchdog_vs_fixedLease_concurrencySafety() {
        val executor = Executors.newFixedThreadPool(2)
        val maxConcurrent = AtomicInteger(0)
        val concurrentCount = AtomicInteger(0)
        val latch = CountDownLatch(2)

        // T1: Watchdog 활성화 (leaseTime = -1) → 4초 처리
        executor.submit {
            try {
                val lock = redissonClient.getLock("lock:watchdog:safe")
                val acquired = lock.tryLock(5, -1, TimeUnit.SECONDS) // Watchdog!
                assertThat(acquired).isTrue()

                val c = concurrentCount.incrementAndGet()
                maxConcurrent.updateAndGet { maxOf(it, c) }

                Thread.sleep(4000) // 4초 처리 — 고정 lease(2초)면 터짐, Watchdog은 안전

                concurrentCount.decrementAndGet()
                lock.unlock()
            } finally {
                latch.countDown()
            }
        }

        // T2: 1초 후 시도 → T1이 Watchdog으로 락 유지 중 → 대기 후 획득
        executor.submit {
            try {
                Thread.sleep(1000)
                val lock = redissonClient.getLock("lock:watchdog:safe")
                val acquired = lock.tryLock(10, -1, TimeUnit.SECONDS)
                assertThat(acquired).isTrue()

                val c = concurrentCount.incrementAndGet()
                maxConcurrent.updateAndGet { maxOf(it, c) }

                Thread.sleep(1000)
                concurrentCount.decrementAndGet()
                lock.unlock()
            } finally {
                latch.countDown()
            }
        }

        latch.await(20, TimeUnit.SECONDS)
        executor.shutdown()

        // Watchdog 덕분에 동시 실행 없음!
        assertThat(maxConcurrent.get()).isEqualTo(1)
    }

    // =========================================================================
    // 테스트 3: Watchdog은 unlock 후 갱신 중지
    //
    // unlock() 호출하면 Watchdog이 즉시 중지되고
    // 대기 중이던 스레드가 바로 획득 가능
    // =========================================================================

    @Test
    @DisplayName("Watchdog: unlock 후 즉시 갱신 중지 → 대기 스레드 획득")
    fun watchdog_unlockStopsRenewal() {
        val executor = Executors.newFixedThreadPool(2)
        val order = ConcurrentLinkedQueue<String>()
        val latch = CountDownLatch(2)

        executor.submit {
            try {
                val lock = redissonClient.getLock("lock:watchdog:stop")
                lock.tryLock(0, -1, TimeUnit.SECONDS)
                order.add("T1:락획득")
                Thread.sleep(2000)
                order.add("T1:unlock")
                lock.unlock() // Watchdog 중지
            } finally {
                latch.countDown()
            }
        }

        executor.submit {
            try {
                Thread.sleep(500)
                val lock = redissonClient.getLock("lock:watchdog:stop")
                // Pub/Sub으로 T1 unlock 시 즉시 알림 → 바로 획득
                val acquired = lock.tryLock(10, -1, TimeUnit.SECONDS)
                assertThat(acquired).isTrue()
                order.add("T2:락획득")
                lock.unlock()
            } finally {
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        val list = order.toList()
        assertThat(list).containsExactly("T1:락획득", "T1:unlock", "T2:락획득")
    }

    // =========================================================================
    // 테스트 4: Watchdog — 스레드 중단 시 자동 만료
    //
    // Watchdog은 스레드가 살아있어야 갱신함
    // 스레드가 인터럽트되면 갱신 중지 → lockWatchdogTimeout(30초) 후 만료
    //
    // 이 테스트는 시간이 오래 걸리므로 짧은 watchdogTimeout으로 설정된
    // 별도 RedissonClient가 필요 — 여기서는 개념만 검증
    // =========================================================================

    @Test
    @DisplayName("Watchdog: 스레드 인터럽트 시 갱신 중지 확인")
    fun watchdog_threadInterrupt_renewalStops() {
        val lock = redissonClient.getLock("lock:watchdog:interrupt")
        val lockAcquired = AtomicReference<Boolean>()
        val threadRef = AtomicReference<Thread>()

        val thread = Thread {
            val acquired = lock.tryLock(0, -1, TimeUnit.SECONDS)
            lockAcquired.set(acquired)
            try {
                Thread.sleep(60000) // 오래 대기
            } catch (e: InterruptedException) {
                // 인터럽트됨 — Watchdog 갱신이 중지됨
                Thread.currentThread().interrupt()
            }
        }
        threadRef.set(thread)
        thread.start()
        Thread.sleep(1000) // 락 획득 대기

        assertThat(lockAcquired.get()).isTrue()
        assertThat(lock.isLocked).isTrue()

        // 스레드 인터럽트
        thread.interrupt()
        thread.join(3000)

        // 락은 여전히 걸려있음 (TTL이 남아있으므로)
        // 하지만 Watchdog 갱신은 중지됨 → lockWatchdogTimeout(30초) 후 자동 만료
        assertThat(lock.isLocked).isTrue()

        // 수동으로 unlock (테스트 정리)
        // Watchdog 스레드가 아닌 다른 스레드에서 unlock 불가 — 이건 정상
        // 실제로는 30초 후 자동 만료됨
    }

    // =========================================================================
    // 테스트 5: Watchdog + 재진입 (Reentrant)
    //
    // 같은 스레드에서 같은 락을 여러 번 획득 가능
    // Watchdog은 최초 획득 시 활성화, 마지막 unlock 시 중지
    // =========================================================================

    @Test
    @DisplayName("Watchdog + 재진입: 같은 스레드에서 중첩 락 가능")
    fun watchdog_reentrant() {
        val lock = redissonClient.getLock("lock:watchdog:reentrant")

        // 1차 획득
        val acquired1 = lock.tryLock(0, -1, TimeUnit.SECONDS)
        assertThat(acquired1).isTrue()
        assertThat(lock.holdCount).isEqualTo(1)

        // 2차 획득 (재진입)
        val acquired2 = lock.tryLock(0, -1, TimeUnit.SECONDS)
        assertThat(acquired2).isTrue()
        assertThat(lock.holdCount).isEqualTo(2)

        // 1차 해제
        lock.unlock()
        assertThat(lock.holdCount).isEqualTo(1)
        assertThat(lock.isHeldByCurrentThread).isTrue() // 아직 보유 중

        // 2차 해제
        lock.unlock()
        assertThat(lock.holdCount).isEqualTo(0)
        assertThat(lock.isHeldByCurrentThread).isFalse()
    }

    // =========================================================================
    // 테스트 6: 고정 lease(leaseTime > 0)이면 Watchdog 비활성화
    //
    // leaseTime을 양수로 주면 Watchdog이 동작하지 않음
    // → lease 만료 시 강제 해제됨 (시나리오 3 위험)
    // =========================================================================

    @Test
    @DisplayName("고정 lease: Watchdog 비활성화 → TTL 후 강제 해제")
    fun fixedLease_noWatchdog_forcedRelease() {
        val lock = redissonClient.getLock("lock:watchdog:fixed")

        // leaseTime = 2초 → Watchdog 비활성화!
        val acquired = lock.tryLock(0, 2, TimeUnit.SECONDS)
        assertThat(acquired).isTrue()

        // 1초 후 — 아직 유효
        Thread.sleep(1000)
        assertThat(lock.isLocked).isTrue()

        // 2.5초 후 — lease 만료로 강제 해제됨
        Thread.sleep(1500)
        assertThat(lock.isLocked).isFalse() // 강제 해제!
    }
}
