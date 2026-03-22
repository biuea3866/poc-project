package com.closet.order.infrastructure.scheduler

import com.closet.order.application.OrderSagaOrchestrator
import com.closet.order.domain.order.OrderStatus
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.SagaExecutionRepository
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * 재고 예약 만료 스케줄러.
 * 15분 이내에 결제가 완료되지 않은 주문을 자동 취소하고 재고를 해제한다.
 */
@Component
class ReservationTimeoutScheduler(
    private val orderRepository: OrderRepository,
    private val sagaRepository: SagaExecutionRepository,
    private val sagaOrchestrator: OrderSagaOrchestrator,
) {

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    fun cancelExpiredReservations() {
        val now = LocalDateTime.now()
        val expiredOrders = orderRepository.findExpiredReservations(now)

        if (expiredOrders.isEmpty()) return

        logger.info { "만료된 예약 주문 ${expiredOrders.size}건 처리 시작" }

        expiredOrders.forEach { order ->
            try {
                val saga = sagaRepository.findByOrderId(order.id)
                if (saga != null) {
                    sagaOrchestrator.handlePaymentFailed(
                        sagaId = saga.sagaId,
                        reason = "Reservation timeout (15 minutes)",
                    )
                    logger.info { "예약 만료 처리 완료: orderId=${order.id}, sagaId=${saga.sagaId}" }
                } else {
                    logger.warn { "Saga를 찾을 수 없어 만료 처리 생략: orderId=${order.id}" }
                }
            } catch (e: Exception) {
                logger.error(e) { "예약 만료 처리 실패: orderId=${order.id}" }
            }
        }
    }
}
