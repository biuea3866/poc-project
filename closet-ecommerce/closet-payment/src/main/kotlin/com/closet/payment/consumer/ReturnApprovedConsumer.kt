package com.closet.payment.consumer

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
 * return.approved 이벤트 수신.
 * 반품 승인 시 부분 환불을 처리한다 (PD-12, PD-17).
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
        private const val TOPIC = "return.approved"
        private const val CONSUMER_GROUP = "payment-service"
    }

    data class ReturnApprovedPayload(
        val orderId: Long,
        val returnRequestId: Long,
        val refundAmount: Long,
        val shippingFee: Long,
    )

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ReturnApprovedPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "return.approved 메시지 파싱 실패: ${record.value()}" }
            return
        }

        val eventId = "payment-return-approved-${payload.returnRequestId}"
        logger.info { "return.approved 수신: orderId=${payload.orderId}, refundAmount=${payload.refundAmount}" }

        idempotencyChecker.process(eventId, TOPIC, CONSUMER_GROUP) {
            val payment = paymentRepository.findByOrderId(payload.orderId)
                .orElse(null)
            if (payment == null) {
                logger.warn { "결제 정보를 찾을 수 없습니다: orderId=${payload.orderId}" }
                return@process
            }

            paymentService.refund(
                payment.id,
                RefundPaymentRequest(
                    amount = payload.refundAmount,
                    reason = "반품 승인 환불 (returnRequestId=${payload.returnRequestId})"
                )
            )
        }
    }
}
