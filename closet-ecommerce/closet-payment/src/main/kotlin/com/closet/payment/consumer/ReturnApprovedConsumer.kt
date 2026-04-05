package com.closet.payment.consumer

import com.closet.common.event.ClosetTopics
import com.closet.common.idempotency.IdempotencyChecker
import com.closet.payment.application.PaymentService
import com.closet.payment.application.RefundPaymentRequest
import com.closet.payment.domain.PaymentRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.shipping 토픽 Consumer.
 *
 * 반품 승인(ReturnApproved) 이벤트를 수신하여 부분 환불을 처리한다 (PD-12, PD-17).
 * eventType="ReturnApproved"만 처리하고, 나머지는 무시한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.payment-kafka-enabled"], havingValue = "true", matchIfMissing = true)
class ReturnApprovedConsumer(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository,
    private val idempotencyChecker: IdempotencyChecker,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val CONSUMER_GROUP = "payment-service"
    }

    data class ShippingEventEnvelope(
        val eventType: String,
        val orderId: Long? = null,
        val returnRequestId: Long? = null,
        val refundAmount: Long? = null,
        val shippingFee: Long? = null,
    )

    @KafkaListener(topics = [ClosetTopics.SHIPPING], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val envelope = try {
            objectMapper.readValue(record.value(), ShippingEventEnvelope::class.java)
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.SHIPPING} 메시지 파싱 실패: ${record.value()}" }
            return
        }

        if (envelope.eventType != "ReturnApproved") {
            logger.debug { "처리하지 않는 eventType 무시: ${envelope.eventType}" }
            return
        }

        val orderId = envelope.orderId ?: return
        val returnRequestId = envelope.returnRequestId ?: return
        val refundAmount = envelope.refundAmount ?: return

        val eventId = "payment-return-approved-$returnRequestId"
        logger.info { "ReturnApproved 수신: orderId=$orderId, refundAmount=$refundAmount" }

        idempotencyChecker.process(eventId, ClosetTopics.SHIPPING, CONSUMER_GROUP) {
            val payment = paymentRepository.findByOrderId(orderId)
                .orElse(null)
            if (payment == null) {
                logger.warn { "결제 정보를 찾을 수 없습니다: orderId=$orderId" }
                return@process
            }

            paymentService.refund(
                payment.id,
                RefundPaymentRequest(
                    amount = refundAmount,
                    reason = "반품 승인 환불 (returnRequestId=$returnRequestId)"
                )
            )
        }
    }
}
