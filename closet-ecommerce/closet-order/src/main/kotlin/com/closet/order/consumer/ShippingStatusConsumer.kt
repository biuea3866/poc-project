package com.closet.order.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.order.domain.order.OrderStatus
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.OrderStatusHistoryRepository
import com.closet.order.domain.order.OrderStatusHistory
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Component
class ShippingStatusConsumer(
    private val orderRepository: OrderRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val idempotencyChecker: IdempotencyChecker,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "shipping.status.changed"
        private const val CONSUMER_GROUP = "order-service"
    }

    data class ShippingStatusChangedPayload(
        val eventId: String,
        val orderId: Long,
        val shippingStatus: String,
    )

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    @Transactional
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ShippingStatusChangedPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "shipping.status.changed 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "shipping.status.changed 수신: orderId=${payload.orderId}, shippingStatus=${payload.shippingStatus}" }

        idempotencyChecker.process(payload.eventId, TOPIC, CONSUMER_GROUP) {
            val order = orderRepository.findByIdAndDeletedAtIsNull(payload.orderId)
            if (order == null) {
                logger.warn { "주문을 찾을 수 없습니다. orderId=${payload.orderId}" }
                return@process
            }

            if (order.status.isTerminal()) {
                logger.info { "터미널 상태 주문은 무시합니다. orderId=${payload.orderId}, status=${order.status}" }
                return@process
            }

            val targetStatus = mapShippingStatusToOrderStatus(payload.shippingStatus)
            if (targetStatus == null) {
                logger.warn { "알 수 없는 배송 상태: ${payload.shippingStatus}" }
                return@process
            }

            if (!order.status.canTransitionTo(targetStatus)) {
                logger.warn { "잘못된 상태 전이 시도: ${order.status} -> $targetStatus, orderId=${payload.orderId}" }
                return@process
            }

            val previousStatus = order.status
            when (targetStatus) {
                OrderStatus.PREPARING -> order.prepare()
                OrderStatus.SHIPPED -> order.ship()
                OrderStatus.DELIVERED -> order.deliver()
                else -> {
                    logger.warn { "처리할 수 없는 대상 상태: $targetStatus" }
                    return@process
                }
            }

            orderStatusHistoryRepository.save(
                OrderStatusHistory.create(
                    orderId = order.id,
                    fromStatus = previousStatus,
                    toStatus = order.status,
                    changedBy = "shipping-service",
                )
            )

            logger.info { "주문 상태 동기화 완료: orderId=${payload.orderId}, ${previousStatus} -> ${order.status}" }
        }
    }

    private fun mapShippingStatusToOrderStatus(shippingStatus: String): OrderStatus? {
        return when (shippingStatus) {
            "READY" -> OrderStatus.PREPARING
            "IN_TRANSIT" -> OrderStatus.SHIPPED
            "DELIVERED" -> OrderStatus.DELIVERED
            else -> null
        }
    }
}
