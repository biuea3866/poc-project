package com.biuea.delivery.infrastructure.persistence

import com.biuea.delivery.domain.order.AcceptOutcome
import com.biuea.delivery.domain.order.DeliveryAcceptStrategy
import org.springframework.context.annotation.Primary
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.stereotype.Component

/**
 * 비관적 락 전략 — SELECT ... FOR UPDATE 로 주문 행을 잠그고 배차한다.
 *
 * 승자 판정을 DB 에 위임하므로 가장 단순하고 확실하다. 대신 대기하는 라이더 전원이
 * "커넥션을 붙잡은 채" 줄을 선다 — 커넥션 풀이 곧 동시 처리 한계가 된다. 그래서 기본 전략(@Primary)으로 두되
 * 경합이 심한 구간에서는 아래 두 전략과 수치를 비교해 선택한다.
 */
@Component
@Primary
class PessimisticLockAcceptStrategy(
    private val deliveryOrderAssignmentTransaction: DeliveryOrderAssignmentTransaction,
) : DeliveryAcceptStrategy {

    override fun accept(orderId: Long, riderId: Long): AcceptOutcome =
        try {
            deliveryOrderAssignmentTransaction.assignWithRowLock(orderId, riderId)
        } catch (exception: PessimisticLockingFailureException) {
            // innodb_lock_wait_timeout 초과. 대기 시간을 다 쓰고 실패하므로 커넥션도 그만큼 붙잡힌다.
            AcceptOutcome.LockAcquisitionFailed(orderId)
        }
}
