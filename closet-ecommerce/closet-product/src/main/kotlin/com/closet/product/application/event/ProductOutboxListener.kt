package com.closet.product.application.event

import com.closet.common.event.ClosetTopics
import com.closet.common.outbox.OutboxEventPublisher
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val logger = KotlinLogging.logger {}

/**
 * Product 도메인 이벤트를 수신하여 outbox_event 테이블에 INSERT한다.
 *
 * @TransactionalEventListener(phase = BEFORE_COMMIT) 사용:
 * - 비즈니스 트랜잭션 커밋 직전에 outbox INSERT가 실행됨
 * - OutboxEventPublisher는 MANDATORY propagation이므로 기존 트랜잭션에 참여
 * - 비즈니스 로직 실패 시 outbox INSERT도 함께 롤백
 */
@Component
class ProductOutboxListener(
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val AGGREGATE_TYPE = "Product"
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleProductCreated(event: ProductCreatedEvent) {
        logger.info { "ProductCreatedEvent 수신: productId=${event.productId}" }

        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.productId.toString(),
            eventType = "ProductCreated",
            topic = ClosetTopics.PRODUCT,
            partitionKey = event.productId.toString(),
            payload = objectMapper.writeValueAsString(event),
        )

        logger.debug { "Outbox INSERT 완료: topic=${ClosetTopics.PRODUCT}, productId=${event.productId}" }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleProductUpdated(event: ProductUpdatedEvent) {
        logger.info { "ProductUpdatedEvent 수신: productId=${event.productId}" }

        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.productId.toString(),
            eventType = "ProductUpdated",
            topic = ClosetTopics.PRODUCT,
            partitionKey = event.productId.toString(),
            payload = objectMapper.writeValueAsString(event),
        )

        logger.debug { "Outbox INSERT 완료: topic=${ClosetTopics.PRODUCT}, productId=${event.productId}" }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleProductDeleted(event: ProductDeletedEvent) {
        logger.info { "ProductDeletedEvent 수신: productId=${event.productId}" }

        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.productId.toString(),
            eventType = "ProductDeleted",
            topic = ClosetTopics.PRODUCT,
            partitionKey = event.productId.toString(),
            payload = objectMapper.writeValueAsString(event),
        )

        logger.debug { "Outbox INSERT 완료: topic=${ClosetTopics.PRODUCT}, productId=${event.productId}" }
    }
}
