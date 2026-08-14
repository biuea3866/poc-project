package com.biuea.delivery.application

import com.biuea.delivery.domain.order.AcceptOutcome
import com.biuea.delivery.domain.order.DeliveryAcceptStrategy
import org.springframework.stereotype.Service

/**
 * 라이더의 배달 요청 수락.
 *
 * 트랜잭션 경계를 여기에 두지 않는다. 낙관적 락은 커밋 시점에 충돌이 드러나므로
 * 재시도가 트랜잭션 "바깥" 이어야 하고, 분산락은 락을 잡은 뒤 짧은 트랜잭션만 열어야 한다.
 * 그래서 경계는 전략 아래(DeliveryOrderAssignmentTransaction)에 있다.
 */
@Service
class AcceptDeliveryUseCase(
    private val deliveryAcceptStrategy: DeliveryAcceptStrategy,
) {
    fun execute(command: AcceptDeliveryCommand): AcceptOutcome =
        deliveryAcceptStrategy.accept(command.orderId, command.riderId)
}
