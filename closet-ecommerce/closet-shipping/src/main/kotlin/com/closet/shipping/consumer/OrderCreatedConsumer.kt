package com.closet.shipping.consumer

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
 * order.created 이벤트 수신.
 * 배송 준비 정보(수신인, 주소 등)를 사전 저장한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.shipping-kafka-enabled"], havingValue = "true", matchIfMissing = true)
class OrderCreatedConsumer(
    private val shippingService: ShippingService,
    private val idempotencyChecker: IdempotencyChecker,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "order.created"
        private const val CONSUMER_GROUP = "shipping-service"
    }

    data class OrderCreatedPayload(
        val orderId: Long,
        val memberId: Long,
        val sellerId: Long,
        val receiverName: String,
        val receiverPhone: String,
        val zipCode: String,
        val address: String,
        val detailAddress: String,
    )

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), OrderCreatedPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "order.created 메시지 파싱 실패: ${record.value()}" }
            return
        }

        val eventId = "shipping-order-created-${payload.orderId}"
        logger.info { "order.created 수신: orderId=${payload.orderId}" }

        idempotencyChecker.process(eventId, TOPIC, CONSUMER_GROUP) {
            shippingService.prepareShipment(
                PrepareShipmentRequest(
                    orderId = payload.orderId,
                    sellerId = payload.sellerId,
                    memberId = payload.memberId,
                    receiverName = payload.receiverName,
                    receiverPhone = payload.receiverPhone,
                    zipCode = payload.zipCode,
                    address = payload.address,
                    detailAddress = payload.detailAddress,
                )
            )
        }
    }
}
