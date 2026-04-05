package com.closet.shipping.consumer

import com.closet.common.event.ClosetTopics
import com.closet.common.idempotency.IdempotencyChecker
import com.closet.shipping.application.PrepareShipmentRequest
import com.closet.shipping.application.ShippingService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.order 토픽 Consumer.
 *
 * 주문 생성(OrderCreated) 이벤트를 수신하여 배송 준비 정보를 사전 저장한다.
 * eventType="OrderCreated"만 처리하고, 나머지는 무시한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.shipping-kafka-enabled"], havingValue = "true", matchIfMissing = true)
class OrderCreatedConsumer(
    private val shippingService: ShippingService,
    private val idempotencyChecker: IdempotencyChecker,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val CONSUMER_GROUP = "shipping-service"
    }

    data class OrderEventEnvelope(
        val eventType: String,
        val orderId: Long? = null,
        val memberId: Long? = null,
        val sellerId: Long? = null,
        val receiverName: String? = null,
        val receiverPhone: String? = null,
        val zipCode: String? = null,
        val address: String? = null,
        val detailAddress: String? = null,
    )

    @KafkaListener(topics = [ClosetTopics.ORDER], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val envelope = try {
            objectMapper.readValue(record.value(), OrderEventEnvelope::class.java)
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.ORDER} 메시지 파싱 실패: ${record.value()}" }
            return
        }

        if (envelope.eventType != "OrderCreated") {
            logger.debug { "처리하지 않는 eventType 무시: ${envelope.eventType}" }
            return
        }

        val orderId = envelope.orderId ?: return
        val eventId = "shipping-order-created-$orderId"
        logger.info { "OrderCreated 수신: orderId=$orderId" }

        idempotencyChecker.process(eventId, ClosetTopics.ORDER, CONSUMER_GROUP) {
            shippingService.prepareShipment(
                PrepareShipmentRequest(
                    orderId = orderId,
                    sellerId = envelope.sellerId ?: 0L,
                    memberId = envelope.memberId ?: 0L,
                    receiverName = envelope.receiverName ?: "",
                    receiverPhone = envelope.receiverPhone ?: "",
                    zipCode = envelope.zipCode ?: "",
                    address = envelope.address ?: "",
                    detailAddress = envelope.detailAddress ?: "",
                )
            )
        }
    }
}
