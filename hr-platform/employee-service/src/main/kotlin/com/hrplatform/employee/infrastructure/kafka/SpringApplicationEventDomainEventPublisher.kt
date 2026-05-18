package com.hrplatform.employee.infrastructure.kafka

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.event.DomainEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * DomainEventPublisher 구현체.
 * 도메인 이벤트를 Spring ApplicationEvent로 변환해 발행한다.
 * KafkaDomainEventPublisher가 @TransactionalEventListener(AFTER_COMMIT)로 수신해 Kafka로 전송한다.
 * 트랜잭션 커밋 후에만 Kafka 전송이 이루어지므로 DB 롤백 시 이벤트 누출이 없다.
 */
@Component
class SpringApplicationEventDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(DomainEventApplicationEvent(event))
    }

    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
