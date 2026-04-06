package com.closet.payment.consumer

import com.closet.common.event.ClosetTopics
import com.closet.common.idempotency.IdempotencyChecker
import com.closet.payment.application.PaymentService
import com.closet.payment.application.RefundPaymentRequest
import com.closet.payment.consumer.event.ShippingEvent
import com.closet.payment.domain.PaymentRepository
import mu.KotlinLogging
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
) {
    companion object {
        private const val CONSUMER_GROUP = "payment-service"
    }

    @KafkaListener(topics = [ClosetTopics.SHIPPING], groupId = CONSUMER_GROUP)
    fun handle(event: ShippingEvent) {
        logger.info { "${ClosetTopics.SHIPPING} 수신: eventType=${event.eventType}, orderId=${event.orderId}" }

        when (event.eventType) {
            "ReturnApproved" -> {
                val returnApproved = event.toReturnApprovedEvent()
                val eventId = "payment-return-approved-${returnApproved.returnRequestId}"

                idempotencyChecker.process(eventId, ClosetTopics.SHIPPING, CONSUMER_GROUP) {
                    val payment =
                        paymentRepository.findByOrderId(returnApproved.orderId)
                            .orElse(null)
                    if (payment == null) {
                        logger.warn { "결제 정보를 찾을 수 없습니다: orderId=${returnApproved.orderId}" }
                        return@process
                    }

                    paymentService.refund(
                        payment.id,
                        RefundPaymentRequest(
                            amount = returnApproved.refundAmount,
                            reason = "반품 승인 환불 (returnRequestId=${returnApproved.returnRequestId})",
                        ),
                    )
                }
            }
            else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
        }
    }
}
