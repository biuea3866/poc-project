package com.biuea.delivery.infrastructure.persistence

import com.zaxxer.hikari.metrics.IMetricsTracker
import com.zaxxer.hikari.metrics.MetricsTrackerFactory
import com.zaxxer.hikari.metrics.PoolStats
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 커넥션 풀 계측기. "비관적 락은 DB 커넥션을 얼마나 오래 붙잡는가"를 숫자로 만들기 위해 존재한다.
 *
 * - 획득 대기(acquire): getConnection() 호출부터 풀이 커넥션을 내줄 때까지. 풀이 마르면 여기서 폭증한다.
 * - 점유(hold): 커넥션을 빌린 순간부터 반납(close)까지. 트랜잭션 안에서 락을 기다리면 그만큼 길어진다.
 *
 * HikariCP 의 MetricsTrackerFactory 훅을 쓰면 프로덕션 코드를 건드리지 않고 실측할 수 있다.
 */
class ConnectionHoldTimeRecorder : MetricsTrackerFactory {
    private val acquireWaitNanos = ConcurrentLinkedQueue<Long>()
    private val holdMillis = ConcurrentLinkedQueue<Long>()

    override fun create(poolName: String, poolStats: PoolStats): IMetricsTracker =
        object : IMetricsTracker {
            override fun recordConnectionAcquiredNanos(elapsedAcquiredNanos: Long) {
                acquireWaitNanos.add(elapsedAcquiredNanos)
            }

            override fun recordConnectionUsageMillis(elapsedBorrowedMillis: Long) {
                holdMillis.add(elapsedBorrowedMillis)
            }
        }

    fun reset() {
        acquireWaitNanos.clear()
        holdMillis.clear()
    }

    fun acquireWaitMillis(): List<Double> = acquireWaitNanos.map { it / NANOS_PER_MILLI }

    fun holdMillis(): List<Double> = holdMillis.map { it.toDouble() }

    companion object {
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
