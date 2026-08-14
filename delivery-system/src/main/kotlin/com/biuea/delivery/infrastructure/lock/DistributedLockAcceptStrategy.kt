package com.biuea.delivery.infrastructure.lock

import com.biuea.delivery.domain.order.AcceptOutcome
import com.biuea.delivery.domain.order.DeliveryAcceptStrategy
import com.biuea.delivery.infrastructure.persistence.DeliveryOrderAssignmentTransaction
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 분산락 전략 — Redis 로 주문별 임계 구역을 만들고, 그 안에서 짧은 트랜잭션으로만 배차한다.
 *
 * DB 락과 달리 대기하는 라이더가 커넥션을 붙잡지 않는다. 대기는 Redis 폴링으로 하고
 * 커넥션은 실제 배차하는 순간에만 쓴다 — 앱 서버가 여러 대여도 커넥션 풀을 지킬 수 있다.
 */
@Component
class DistributedLockAcceptStrategy(
    private val redisDistributedLock: RedisDistributedLock,
    private val deliveryOrderAssignmentTransaction: DeliveryOrderAssignmentTransaction,
) : DeliveryAcceptStrategy {

    override fun accept(orderId: Long, riderId: Long): AcceptOutcome {
        val lockName = lockNameOf(orderId)
        val token = redisDistributedLock.tryAcquire(lockName, LEASE_TTL, WAIT_POLICY)
            ?: return AcceptOutcome.LockAcquisitionFailed(orderId)
        return try {
            deliveryOrderAssignmentTransaction.assignWithVersionCheck(orderId, riderId)
        } catch (exception: OptimisticLockingFailureException) {
            // TTL 이 임계 구역보다 먼저 만료되면 두 노드가 같은 락을 잡을 수 있다.
            // 그때 중복 배차를 막는 최후 방어선이 DB 의 version 이다 — 분산락만 믿지 않는다.
            AcceptOutcome.LockAcquisitionFailed(orderId)
        } finally {
            redisDistributedLock.release(lockName, token)
        }
    }

    private fun lockNameOf(orderId: Long): String = "$LOCK_NAME_PREFIX$orderId"

    companion object {
        private const val LOCK_NAME_PREFIX = "lock:order:"

        /** 임계 구역(조회 + 배차)은 수 ms 다. TTL 은 장애로 해제가 누락됐을 때의 자동 회수 시간이다. */
        private val LEASE_TTL: Duration = Duration.ofSeconds(3)

        /** 수락 버튼을 누른 라이더를 즉시 탈락시키지 않고, 앞사람 처리가 끝날 때까지 짧게 기다린다. */
        private val WAIT_POLICY: LockWaitPolicy =
            LockWaitPolicy.waitUpTo(Duration.ofSeconds(5), Duration.ofMillis(20))
    }
}
