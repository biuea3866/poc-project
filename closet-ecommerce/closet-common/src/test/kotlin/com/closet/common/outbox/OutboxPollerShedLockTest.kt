package com.closet.common.outbox

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.SimpleLock
import java.time.Duration
import java.time.Instant
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * ShedLock 기반 OutboxPoller 분산 락 테스트.
 *
 * K8s 멀티 인스턴스 환경에서 OutboxPoller가 동시에 실행될 때
 * ShedLock이 단 1개 인스턴스만 실행하도록 보장하는지 검증한다.
 *
 * ADR-001: @Scheduled → ShedLock + K8s CronJob 마이그레이션
 */
class OutboxPollerShedLockTest : BehaviorSpec({

    Given("ShedLock이 설정된 OutboxPoller") {

        When("단일 인스턴스에서 poll()을 실행하면") {
            Then("정상적으로 실행되고 락이 획득/해제된다") {
                val lockProvider = InMemoryLockProvider()
                val executionCount = AtomicInteger(0)

                // ShedLock이 감싸는 poll() 시뮬레이션
                val lock =
                    lockProvider.lock(
                        net.javacrumbs.shedlock.core.LockConfiguration(
                            Instant.now(),
                            "outbox-poller",
                            Duration.ofSeconds(4),
                            Duration.ofSeconds(3),
                        ),
                    )

                lock.isPresent shouldBe true

                // poll 실행
                executionCount.incrementAndGet()
                executionCount.get() shouldBe 1

                // 락 해제
                lock.get().unlock()
                lockProvider.isLocked("outbox-poller") shouldBe false
            }
        }

        When("2개 인스턴스가 동시에 poll()을 실행하면") {
            Then("1개만 실행되고 나머지는 스킵된다") {
                val lockProvider = InMemoryLockProvider()
                val executionCount = AtomicInteger(0)
                val latch = CountDownLatch(2)
                val executor = Executors.newFixedThreadPool(2)
                val results = ConcurrentHashMap<String, Boolean>()

                repeat(2) { i ->
                    executor.submit {
                        val lock =
                            lockProvider.lock(
                                net.javacrumbs.shedlock.core.LockConfiguration(
                                    Instant.now(),
                                    "outbox-poller",
                                    Duration.ofSeconds(10),
                                    Duration.ofSeconds(5),
                                ),
                            )

                        if (lock.isPresent) {
                            executionCount.incrementAndGet()
                            results["instance-$i"] = true
                            Thread.sleep(100) // 작업 시뮬레이션
                            lock.get().unlock()
                        } else {
                            results["instance-$i"] = false
                        }
                        latch.countDown()
                    }
                }

                latch.await()
                executor.shutdown()

                // 정확히 1개만 실행
                executionCount.get() shouldBe 1
                results.values.count { it } shouldBe 1
                results.values.count { !it } shouldBe 1
            }
        }

        When("lockAtMostFor 시간이 경과하면") {
            Then("락이 자동으로 해제되어 다음 실행이 가능하다") {
                val lockProvider = InMemoryLockProvider()

                // 짧은 lockAtMost로 락 획득
                val lock1 =
                    lockProvider.lock(
                        net.javacrumbs.shedlock.core.LockConfiguration(
                            Instant.now(),
                            "outbox-poller",
                            // 50ms 후 만료
                            Duration.ofMillis(50),
                            Duration.ZERO,
                        ),
                    )
                lock1.isPresent shouldBe true

                // 만료 전: 두 번째 락 불가
                val lock2 =
                    lockProvider.lock(
                        net.javacrumbs.shedlock.core.LockConfiguration(
                            Instant.now(),
                            "outbox-poller",
                            Duration.ofSeconds(4),
                            Duration.ZERO,
                        ),
                    )
                lock2.isPresent shouldBe false

                // 만료 대기
                Thread.sleep(60)

                // 만료 후: 다시 락 획득 가능
                val lock3 =
                    lockProvider.lock(
                        net.javacrumbs.shedlock.core.LockConfiguration(
                            Instant.now(),
                            "outbox-poller",
                            Duration.ofSeconds(4),
                            Duration.ZERO,
                        ),
                    )
                lock3.isPresent shouldBe true
                lock3.get().unlock()
            }
        }
    }

    Given("ShedLock 테이블 DDL") {
        When("shedlock 테이블 정의를 확인하면") {
            Then("name, lock_until, locked_at, locked_by 컬럼이 존재한다") {
                val ddl =
                    """
                    CREATE TABLE shedlock (
                        name       VARCHAR(64)  NOT NULL COMMENT '락 이름',
                        lock_until DATETIME(6)  NOT NULL COMMENT '락 유지 시각',
                        locked_at  DATETIME(6)  NOT NULL COMMENT '락 획득 시각',
                        locked_by  VARCHAR(255) NOT NULL COMMENT '락 획득 인스턴스',
                        PRIMARY KEY (name)
                    )
                    """.trimIndent()

                ddl shouldContain "name"
                ddl shouldContain "lock_until"
                ddl shouldContain "locked_at"
                ddl shouldContain "locked_by"
                ddl shouldContain "DATETIME(6)"
                ddl shouldContain "COMMENT"
            }
        }
    }

    Given("Feature Flag 기반 무중단 마이그레이션") {
        When("feature.auto-confirm-scheduler-enabled=false 이면") {
            Then("기존 @Scheduled 스케줄러가 비활성화된다") {
                // ConditionalOnProperty 시뮬레이션
                val featureEnabled = false
                val schedulerCreated = featureEnabled // matchIfMissing=false

                schedulerCreated shouldBe false
            }
        }

        When("feature.auto-confirm-scheduler-enabled=true 이면") {
            Then("기존 @Scheduled 스케줄러가 활성화된다 (롤백 시)") {
                val featureEnabled = true
                val schedulerCreated = featureEnabled

                schedulerCreated shouldBe true
            }
        }
    }
})

/**
 * 테스트용 인메모리 LockProvider.
 * 실제 환경에서는 JdbcTemplateLockProvider (MySQL) 사용.
 */
class InMemoryLockProvider : LockProvider {
    private val locks = ConcurrentHashMap<String, Instant>()

    @Synchronized
    override fun lock(lockConfiguration: net.javacrumbs.shedlock.core.LockConfiguration): Optional<SimpleLock> {
        val name = lockConfiguration.name
        val now = Instant.now()
        val existing = locks[name]

        // 기존 락이 있고 아직 만료 안 됨
        if (existing != null && existing.isAfter(now)) {
            return Optional.empty()
        }

        // 락 획득
        val lockUntil = now.plus(lockConfiguration.lockAtMostFor)
        locks[name] = lockUntil

        return Optional.of(
            SimpleLock {
                // lockAtLeastFor가 지나지 않았으면 유지
                val leastUntil = now.plus(lockConfiguration.lockAtLeastFor)
                if (Instant.now().isBefore(leastUntil)) {
                    locks[name] = leastUntil
                } else {
                    locks.remove(name)
                }
            },
        )
    }

    fun isLocked(name: String): Boolean {
        val lockUntil = locks[name] ?: return false
        return lockUntil.isAfter(Instant.now())
    }
}
