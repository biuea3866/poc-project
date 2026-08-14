package com.biuea.delivery.infrastructure.lock

import java.time.Duration

/**
 * 락 획득 실패 시의 대기 정책.
 *
 * 즉시 실패(failFast)와 짧은 대기 후 재시도(waitUpTo)를 같은 타입으로 표현한다.
 * 즉시 실패는 대기 시간 0인 특수 케이스일 뿐이라 호출부에 분기를 만들지 않는다.
 */
class LockWaitPolicy private constructor(
    private val waitTimeout: Duration,
    private val retryInterval: Duration,
) {
    /** 대기 마감을 넘겼는지 판단한다. 즉시 실패 정책은 첫 시도 직후 곧바로 마감이다. */
    fun isExpired(waitStartedAtNanos: Long): Boolean =
        System.nanoTime() - waitStartedAtNanos >= waitTimeout.toNanos()

    fun sleepBeforeRetry() {
        if (!retryInterval.isZero) {
            Thread.sleep(retryInterval.toMillis())
        }
    }

    companion object {
        fun failFast(): LockWaitPolicy = LockWaitPolicy(Duration.ZERO, Duration.ZERO)

        fun waitUpTo(waitTimeout: Duration, retryInterval: Duration): LockWaitPolicy =
            LockWaitPolicy(waitTimeout, retryInterval)
    }
}
