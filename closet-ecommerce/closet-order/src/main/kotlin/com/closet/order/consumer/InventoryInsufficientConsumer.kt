package com.closet.order.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.order.domain.order.OrderStatus
import com.closet.order.domain.order.OrderStatusHistory
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.OrderStatusHistoryRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Component
class InventoryInsufficientConsumer(
    private val orderRepository: OrderRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val idempotencyChecker: IdempotencyChecker,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "inventory.insufficient"
        private const val CONSUMER_GROUP = "order-service"
    }

    data class InventoryInsufficientPayload(
        val eventId: String,
        val orderId: Long,
        val reason: String = "재고 부족",
    )

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    @Transactional
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), InventoryInsufficientPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "inventory.insufficient 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "inventory.insufficient 수신: orderId=${payload.orderId}, reason=${payload.reason}" }

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

            if (order.status != OrderStatus.STOCK_RESERVED) {
                logger.warn { "STOCK_RESERVED 상태가 아닌 주문에 재고 부족 이벤트 수신: orderId=${payload.orderId}, status=${order.status}" }
                return@process
            }

            val previousStatus = order.status
            order.fail()

            orderStatusHistoryRepository.save(
                OrderStatusHistory.create(
                    orderId = order.id,
                    fromStatus = previousStatus,
                    toStatus = order.status,
                    reason = payload.reason,
                    changedBy = "inventory-service",
                )
            )

            logger.info { "주문 FAILED 처리 완료: orderId=${payload.orderId}, reason=${payload.reason}" }
        }
    }
}
