package com.biuea.concurrency.distributedlock

import com.biuea.concurrency.support.TestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 분산 락 테스트 — wait, lease, API 종료 시간 시나리오 기반
 *
 * 참고: "분산락 wait, lease 시간 설정" 문서
 *
 * 핵심 관계:
 * - wait: 락 대기 시간 (초과 시 락 획득 실패)
 * - lease: 락 최대 점유 시간 (초과 시 강제 해제)
 * - API 종료: 실제 함수 실행 시간
 *
 * 핵심 규칙: lease > API 종료 시간 (반드시!)
 */
class DistributedLockTest : TestContainersConfig() {

    @Autowired
    lateinit var service: DistributedLockExampleService

    @Autowired
    lateinit var redissonClient: RedissonClient

    @BeforeEach
    fun setUp() {
        service.reset()
    }

    // =========================================================================
    // 시나리오 1: lease(10) > API 종료(3) > wait(1) → 락 획득 실패
    //
    // T1이 락 획득 후 3초 동안 처리
    // T2가 1초 대기 → 1초 후 T1이 아직 처리 중 → 락 획득 실패
    // =========================================================================

    @Test
    @DisplayName("시나리오1: wait < API 종료 → 두 번째 요청이 락 획득 실패 (따닥 방지)")
    fun scenario1_waitShorterThanApiDuration_lockAcquireFail() {
        val executor = Executors.newFixedThreadPool(2)
        val t1Result = AtomicReference<String>()
        val t2Exception = AtomicReference<Throwable>()
        val latch = CountDownLatch(2)

        // T1: 락 획득 → 3초 처리 (wait=1, lease=10이지만 직접 Redisson으로 테스트)
        executor.submit {
            try {
                val lock = redissonClient.getLock("lock:scenario1")
                val acquired = lock.tryLock(1, 10, TimeUnit.SECONDS)
                assertThat(acquired).isTrue()
                Thread.sleep(3000) // API 3초 소요
                t1Result.set("T1 완료")
                lock.unlock()
            } catch (e: Exception) {
                t1Result.set("T1 실패: ${e.message}")
            } finally {
                latch.countDown()
            }
        }

        // T2: 500ms 후 시작 → wait 1초 대기 → T1이 아직 처리 중 → 실패
        executor.submit {
            try {
                Thread.sleep(500) // T1이 먼저 락 획득하도록 대기
                val lock = redissonClient.getLock("lock:scenario1")
                val acquired = lock.tryLock(1, 10, TimeUnit.SECONDS) // wait=1초
                if (!acquired) {
                    t2Exception.set(DistributedLockAcquireFailedException("scenario1"))
                } else {
                    t2Exception.set(null)
                    lock.unlock()
                }
            } catch (e: Exception) {
                t2Exception.set(e)
            } finally {
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(t1Result.get()).isEqualTo("T1 완료")
        assertThat(t2Exception.get()).isNotNull // T2는 락 획득 실패
        assertThat(t2Exception.get()).isInstanceOf(DistributedLockAcquireFailedException::class.java)
    }

    // =========================================================================
    // 시나리오 2: lease(10) > wait(4) > API 종료(3) → 정상 순차 처리
    //
    // T1이 락 획득 후 3초 처리 → 해제
    // T2가 4초 대기 → T1 종료(3초) 후 락 획득 → 정상 처리
    // =========================================================================

    @Test
    @DisplayName("시나리오2: wait > API 종료 → 두 번째 요청도 정상 처리 (순차 처리)")
    fun scenario2_waitLongerThanApiDuration_bothSucceed() {
        val executor = Executors.newFixedThreadPool(2)
        val results = ConcurrentLinkedQueue<String>()
        val latch = CountDownLatch(2)

        // T1: 락 획득 → 1초 처리
        executor.submit {
            try {
                val lock = redissonClient.getLock("lock:scenario2")
                val acquired = lock.tryLock(4, 10, TimeUnit.SECONDS) // wait=4
                assertThat(acquired).isTrue()
                Thread.sleep(1000) // API 1초 소요
                results.add("T1 완료")
                lock.unlock()
            } finally {
                latch.countDown()
            }
        }

        // T2: 200ms 후 시작 → wait 4초 대기 → T1 종료(1초) 후 획득
        executor.submit {
            try {
                Thread.sleep(200)
                val lock = redissonClient.getLock("lock:scenario2")
                val acquired = lock.tryLock(4, 10, TimeUnit.SECONDS) // wait=4 > API 종료 1
                assertThat(acquired).isTrue() // T1이 1초 후 해제하므로 획득 성공
                Thread.sleep(1000)
                results.add("T2 완료")
                lock.unlock()
            } finally {
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(results).hasSize(2)
        assertThat(results).containsExactly("T1 완료", "T2 완료")
    }

    // =========================================================================
    // 시나리오 3: API 종료(6) > lease(5) > wait(4) → 동시성 문제 발생!
    //
    // T1이 락 획득 → 6초 처리 중
    // lease 5초 만료 → 락 강제 해제
    // T2가 wait 4초 이내에 대기 중 → lease 해제 시점에 락 획득
    // T1과 T2가 동시에 임계 영역 실행 → 동시성 문제!
    // =========================================================================

    @Test
    @DisplayName("시나리오3: API 종료 > lease → 락 강제 해제로 동시성 문제 발생 (위험!)")
    fun scenario3_apiDurationExceedsLease_concurrencyIssue() {
        val executor = Executors.newFixedThreadPool(2)
        val concurrentExecution = AtomicInteger(0) // 동시 실행 감지
        val maxConcurrent = AtomicInteger(0)
        val latch = CountDownLatch(2)

        // T1: 락 획득 → 4초 처리 (lease=2이므로 중간에 만료됨)
        executor.submit {
            try {
                val lock = redissonClient.getLock("lock:scenario3")
                val acquired = lock.tryLock(3, 2, TimeUnit.SECONDS) // lease=2초!
                assertThat(acquired).isTrue()

                val current = concurrentExecution.incrementAndGet()
                maxConcurrent.updateAndGet { max -> maxOf(max, current) }

                Thread.sleep(4000) // API 4초 (lease 2초보다 김!)

                concurrentExecution.decrementAndGet()

                // lease 만료로 이미 다른 스레드가 소유 → unlock 시 예외 가능
                if (lock.isHeldByCurrentThread) {
                    lock.unlock()
                }
            } catch (e: Exception) {
                // IllegalMonitorStateException 등 발생 가능
            } finally {
                latch.countDown()
            }
        }

        // T2: 1초 후 시작 → 대기 → lease 만료(2초) 시점에 락 획득
        executor.submit {
            try {
                Thread.sleep(1000)
                val lock = redissonClient.getLock("lock:scenario3")
                val acquired = lock.tryLock(3, 2, TimeUnit.SECONDS) // wait=3, lease=2

                if (acquired) {
                    val current = concurrentExecution.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }

                    Thread.sleep(1000)
                    concurrentExecution.decrementAndGet()

                    if (lock.isHeldByCurrentThread) {
                        lock.unlock()
                    }
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // 동시에 2개가 실행된 순간이 있었음 → 동시성 문제!
        assertThat(maxConcurrent.get()).isGreaterThanOrEqualTo(2)
    }

    // =========================================================================
    // 시나리오 4: wait=0 → 즉시 튕김 (따닥 방지 최적)
    //
    // 결제, AI 서류평가 등 절대 중복 실행하면 안 되는 경우
    // 두 번째 요청은 대기 없이 즉시 실패
    // =========================================================================

    @Test
    @DisplayName("시나리오4: wait=0 → 즉시 락 획득 실패 (따닥 방지)")
    fun scenario4_waitZero_immediateReject() {
        val executor = Executors.newFixedThreadPool(2)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val latch = CountDownLatch(2)

        // 두 스레드가 거의 동시에 시도
        repeat(2) {
            executor.submit {
                try {
                    val lock = redissonClient.getLock("lock:payment:order-123")
                    val acquired = lock.tryLock(0, 10, TimeUnit.SECONDS) // wait=0!
                    if (acquired) {
                        Thread.sleep(2000) // 결제 처리 2초
                        successCount.incrementAndGet()
                        lock.unlock()
                    } else {
                        failCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(successCount.get()).isEqualTo(1) // 1개만 성공
        assertThat(failCount.get()).isEqualTo(1) // 1개는 즉시 실패
    }

    // =========================================================================
    // 시나리오 5: 트랜잭션 커밋 vs 락 해제 타이밍
    //
    // @Transactional이 Service에 있고 @DistributedLock이 Controller에 있을 때:
    // 1. 락 획득
    // 2. Controller → Service (트랜잭션 시작)
    // 3. Service 비즈니스 로직 실행
    // 4. Service 종료 → 트랜잭션 커밋
    // 5. Controller 종료 → 락 해제
    //
    // 트랜잭션 커밋(4)과 락 해제(5) 사이에 갭이 있음!
    // 이 갭에 다른 스레드가 락을 획득하면 커밋되지 않은 데이터를 볼 수 있음
    // =========================================================================

    @Test
    @DisplayName("시나리오5: 트랜잭션 커밋 후 락 해제까지의 갭 — 순차 접근 검증")
    fun scenario5_transactionCommitBeforeLockRelease() {
        val executor = Executors.newFixedThreadPool(2)
        val processingOrder = ConcurrentLinkedQueue<String>()
        val latch = CountDownLatch(2)

        // 트랜잭션 커밋 → 락 해제 시뮬레이션
        // 실제로는 AOP가 Controller 레벨에서 동작하므로
        // Service의 @Transactional이 먼저 커밋되고, 그 다음 finally에서 락 해제

        executor.submit {
            try {
                val lock = redissonClient.getLock("lock:tx-test")
                val acquired = lock.tryLock(5, 10, TimeUnit.SECONDS)
                assertThat(acquired).isTrue()

                // Service 로직 (트랜잭션 내)
                processingOrder.add("T1:비즈니스로직시작")
                Thread.sleep(1000)
                processingOrder.add("T1:트랜잭션커밋") // 여기서 DB 커밋

                // Controller 후처리 (트랜잭션 밖)
                Thread.sleep(100) // 커밋 → 락 해제 사이 갭
                processingOrder.add("T1:락해제")
                lock.unlock()
            } finally {
                latch.countDown()
            }
        }

        executor.submit {
            try {
                Thread.sleep(200) // T1이 먼저 시작
                val lock = redissonClient.getLock("lock:tx-test")
                val acquired = lock.tryLock(5, 10, TimeUnit.SECONDS) // T1 해제 대기
                assertThat(acquired).isTrue()

                processingOrder.add("T2:락획득")
                processingOrder.add("T2:비즈니스로직시작")
                Thread.sleep(500)
                processingOrder.add("T2:트랜잭션커밋")
                lock.unlock()
                processingOrder.add("T2:락해제")
            } finally {
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // T1 완전 종료 후 T2 시작 검증
        val orderList = processingOrder.toList()
        val t1LockRelease = orderList.indexOf("T1:락해제")
        val t2LockAcquire = orderList.indexOf("T2:락획득")
        assertThat(t1LockRelease).isLessThan(t2LockAcquire)
    }

    // =========================================================================
    // 시나리오 6: 동시 10개 요청 — 재고 차감 동시성 테스트
    //
    // 재고 100개, 10개 스레드가 동시에 10개씩 차감
    // 분산 락으로 순차 처리 → 최종 재고 0
    // =========================================================================

    @Test
    @DisplayName("시나리오6: 동시 10개 재고 차감 → 분산 락으로 정합성 보장")
    fun scenario6_concurrent10Threads_stockConsistency() {
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val stock = AtomicInteger(100)

        repeat(threadCount) {
            executor.submit {
                try {
                    val lock = redissonClient.getLock("lock:stock:product-1")
                    val acquired = lock.tryLock(10, 15, TimeUnit.SECONDS) // 충분한 wait
                    if (acquired) {
                        try {
                            // 락 내에서 재고 차감
                            val current = stock.get()
                            Thread.sleep(100) // DB 쿼리 시뮬레이션
                            stock.set(current - 10)
                            successCount.incrementAndGet()
                        } finally {
                            lock.unlock()
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(successCount.get()).isEqualTo(10) // 전부 성공
        assertThat(stock.get()).isEqualTo(0) // 정확히 0 (동시성 문제 없음)
    }

    // =========================================================================
    // 시나리오 7: 락 없이 동시 차감 → 동시성 문제 발생 (대조군)
    // =========================================================================

    @Test
    @DisplayName("시나리오7: 락 없이 동시 차감 → 동시성 문제 발생 (대조군)")
    fun scenario7_withoutLock_concurrencyIssue() {
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val stock = AtomicInteger(100)

        repeat(threadCount) {
            executor.submit {
                try {
                    // 락 없이 직접 차감 — race condition!
                    val current = stock.get()
                    Thread.sleep(100) // DB 쿼리 시뮬레이션 (이 사이에 다른 스레드가 읽음)
                    stock.set(current - 10)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // 락 없으면 race condition으로 0이 아닌 값이 나옴
        // (모든 스레드가 100을 읽고 90을 설정 → 최종 90)
        assertThat(stock.get()).isNotEqualTo(0) // 동시성 문제!
    }
}
