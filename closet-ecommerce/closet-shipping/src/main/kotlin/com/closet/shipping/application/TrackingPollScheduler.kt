package com.closet.shipping.application

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 배송 추적 폴링 스케줄러.
 *
 * 30분 간격으로 READY/IN_TRANSIT 상태 배송 건을 폴링하여 택배사 API로 상태를 갱신한다.
 * 상태 변경 감지 시 shipping.status.changed Kafka 이벤트 발행 + Redis 캐시 갱신.
 */
@Component
@ConditionalOnProperty(name = ["feature.shipping-tracking-poll-enabled"], havingValue = "true", matchIfMissing = false)
class TrackingPollScheduler(
    private val shippingService: ShippingService,
) {

    @Scheduled(fixedDelay = 1800000) // 30분
    fun pollTrackingStatus() {
        logger.info { "배송 추적 폴링 시작" }
        try {
            shippingService.pollTrackingStatus()
            logger.info { "배송 추적 폴링 완료" }
        } catch (e: Exception) {
            logger.error(e) { "배송 추적 폴링 실패" }
        }
    }
}
