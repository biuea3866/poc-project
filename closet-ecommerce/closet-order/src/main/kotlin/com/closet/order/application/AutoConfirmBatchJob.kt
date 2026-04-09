package com.closet.order.application

import com.closet.order.domain.order.OrderStatus
import com.closet.order.domain.order.OrderStatusHistory
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.OrderStatusHistoryRepository
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

/**
 * 자동 구매확정 배치 (CP-16, PD-16, PD-17).
 *
 * - 매일 00:00, 12:00 실행
 * - delivered_at + 168시간 경과 + 반품/교환 미진행 건 자동 CONFIRMED
 * - OrderConfirmedEvent Outbox 발행
 *
 * PD-16: 168시간(시각 기준). 반품/교환이 구매확정보다 우선.
 * PD-17: 매일 00:00 + 12:00 (2회) 배치 실행.
 */
@Component
@ConditionalOnProperty(name = ["feature.auto-confirm-batch-enabled"], havingValue = "true", matchIfMissing = false)
class AutoConfirmBatchJob(
    private val orderRepository: OrderRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    companion object {
        private const val CONFIRM_HOURS = 168L // 7일
    }

    /**
     * 매일 00:00, 12:00 실행.
     */
    @Scheduled(cron = "0 0 0,12 * * *")
    @Transactional
    fun execute() {
        val cutoff = ZonedDateTime.now().minusHours(CONFIRM_HOURS)
        logger.info { "자동 구매확정 배치 시작: cutoff=$cutoff" }

        val candidates = orderRepository.findAutoConfirmCandidates(cutoff)

        var confirmedCount = 0
        var skippedCount = 0

        for (order in candidates) {
            try {
                // 반품/교환 진행 중 건 배제 (PD-16)
                if (order.status == OrderStatus.RETURN_REQUESTED || order.status == OrderStatus.EXCHANGE_REQUESTED) {
                    skippedCount++
                    continue
                }

                if (!order.status.canTransitionTo(OrderStatus.CONFIRMED)) {
                    skippedCount++
                    continue
                }

                val previousStatus = order.status
                order.confirm()

                orderStatusHistoryRepository.save(
                    OrderStatusHistory.create(
                        orderId = order.id,
                        fromStatus = previousStatus,
                        toStatus = OrderStatus.CONFIRMED,
                        changedBy = "auto-confirm-batch",
                    ),
                )

                // OrderConfirmedEvent 발행
                eventPublisher.publishEvent(
                    com.closet.order.domain.event.OrderConfirmedEvent(
                        orderId = order.id,
                        memberId = order.memberId,
                    ),
                )

                confirmedCount++
                logger.info { "자동 구매확정: orderId=${order.id}" }
            } catch (e: Exception) {
                logger.error(e) { "자동 구매확정 실패: orderId=${order.id}" }
            }
        }

        logger.info { "자동 구매확정 배치 완료: 대상=${candidates.size}, 확정=$confirmedCount, 스킵=$skippedCount" }
    }
}
