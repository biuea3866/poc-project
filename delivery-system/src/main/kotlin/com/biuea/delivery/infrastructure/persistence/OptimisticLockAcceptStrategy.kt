package com.biuea.delivery.infrastructure.persistence

import com.biuea.delivery.domain.order.AcceptOutcome
import com.biuea.delivery.domain.order.DeliveryAcceptStrategy
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

/**
 * 낙관적 락 전략 — 잠그지 않고 읽은 뒤, 커밋 시점의 version 비교로 충돌을 판정한다.
 *
 * 충돌한 라이더는 재조회 후 다시 시도한다. 경합이 없으면 가장 빠르지만,
 * 100명이 동시에 누르는 순간에는 "먼저 읽은 만큼 전부 충돌" 이라 재시도가 폭증한다.
 * 그 폭증을 눈으로 보려고 총 재시도 횟수를 카운터로 노출한다.
 */
@Component
class OptimisticLockAcceptStrategy(
    private val deliveryOrderAssignmentTransaction: DeliveryOrderAssignmentTransaction,
) : DeliveryAcceptStrategy {

    private val retryCounter = AtomicLong()

    /** 관측용 — 버전 충돌로 재시도가 발생한 총 횟수. */
    val totalRetryCount: Long get() = retryCounter.get()

    fun resetTotalRetryCount() = retryCounter.set(0)

    override fun accept(orderId: Long, riderId: Long): AcceptOutcome {
        var attemptCount = 0
        while (attemptCount < MAX_ATTEMPT_COUNT) {
            attemptCount++
            try {
                return deliveryOrderAssignmentTransaction.assignWithVersionCheck(orderId, riderId)
                    .withRetryCount(attemptCount - 1)
            } catch (exception: OptimisticLockingFailureException) {
                // 백오프를 두지 않는다. 대기를 넣으면 재시도 횟수는 줄지만 수락 지연이 늘어난다 —
                // 여기서는 "경합 시 재시도 비용" 을 있는 그대로 측정하는 쪽을 택했다.
                retryCounter.incrementAndGet()
            }
        }
        return AcceptOutcome.RetryExhausted(orderId, attemptCount)
    }

    companion object {
        const val MAX_ATTEMPT_COUNT = 3
    }
}
