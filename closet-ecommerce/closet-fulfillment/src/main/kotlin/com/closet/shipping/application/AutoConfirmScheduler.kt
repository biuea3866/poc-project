package com.closet.shipping.application

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 자동 구매확정 스케줄러 (US-503).
 *
 * 매일 00:00 실행.
 * 배송 완료(DELIVERED) 후 7일(168시간) 경과한 건을 자동 구매확정 처리한다.
 * 반품/교환 진행 중인 건은 제외한다.
 * DeliveryConfirmed Kafka 이벤트를 ClosetTopics.SHIPPING 토픽으로 발행한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.auto-confirm-scheduler-enabled"], havingValue = "true", matchIfMissing = false)
class AutoConfirmScheduler(
    private val shippingService: ShippingService,
    private val returnService: ReturnService,
    private val exchangeService: ExchangeService,
) {
    @Scheduled(cron = "0 0 0 * * *") // 매일 00:00
    fun execute() {
        logger.info { "자동 구매확정 스케줄러 시작" }
        try {
            shippingService.autoConfirmOrders(returnService, exchangeService)
            logger.info { "자동 구매확정 스케줄러 완료" }
        } catch (e: Exception) {
            logger.error(e) { "자동 구매확정 스케줄러 실패" }
        }
    }
}
