package com.biuea.delivery.infrastructure.persistence

import com.biuea.delivery.domain.order.AcceptOutcome
import com.biuea.delivery.domain.order.DeliveryOrder
import com.biuea.delivery.domain.order.DeliveryOrderNotAssignableException
import com.biuea.delivery.domain.order.DeliveryOrderRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 배차 한 건의 트랜잭션 경계.
 *
 * 전략(비관적·낙관적·분산락)이 공유하는 "조회 → 엔티티에 배차 위임 → 저장" 을 한 트랜잭션으로 묶는다.
 * 경계를 전략 안이 아니라 전략 아래에 둔 이유:
 * - 낙관적 락의 버전 충돌은 커밋 시점에 터진다. 재시도 루프가 트랜잭션 안에 있으면 이미 롤백 표시된
 *   트랜잭션에서 재시도하게 되어 절대 성공하지 못한다.
 * - 분산락은 락을 잡은 뒤 최대한 짧은 트랜잭션만 열어야 커넥션 점유가 줄어든다.
 * self-invocation 은 프록시를 타지 않으므로 전략과 별도 빈으로 분리했다.
 */
@Component
class DeliveryOrderAssignmentTransaction(
    private val deliveryOrderRepository: DeliveryOrderRepository,
) {
    /** 비관적 락 경로: 행을 잠근 뒤 상태를 확인하고 배차한다. */
    @Transactional
    fun assignWithRowLock(orderId: Long, riderId: Long): AcceptOutcome =
        assign(deliveryOrderRepository.findForUpdateBy(orderId), orderId, riderId)

    /** 잠그지 않고 읽는 경로: 충돌 판정은 커밋 시점의 version 비교에 맡긴다. */
    @Transactional
    fun assignWithVersionCheck(orderId: Long, riderId: Long): AcceptOutcome =
        assign(deliveryOrderRepository.findBy(orderId), orderId, riderId)

    private fun assign(order: DeliveryOrder?, orderId: Long, riderId: Long): AcceptOutcome {
        val targetOrder = order ?: return AcceptOutcome.OrderNotFound(orderId)
        return try {
            targetOrder.assignTo(riderId)
            deliveryOrderRepository.save(targetOrder)
            AcceptOutcome.Accepted(orderId, riderId, retryCount = 0)
        } catch (exception: DeliveryOrderNotAssignableException) {
            AcceptOutcome.fromRejection(orderId, exception)
        }
    }
}
