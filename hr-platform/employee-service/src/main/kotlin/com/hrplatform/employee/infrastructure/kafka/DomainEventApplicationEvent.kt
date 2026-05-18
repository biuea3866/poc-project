package com.hrplatform.employee.infrastructure.kafka

import com.hrplatform.core.domain.DomainEvent
import org.springframework.context.ApplicationEvent

/**
 * DomainEvent를 Spring ApplicationEvent로 감싸는 래퍼.
 * SpringApplicationEventDomainEventPublisher가 발행하고
 * KafkaDomainEventPublisher가 @TransactionalEventListener(AFTER_COMMIT)로 수신한다.
 */
class DomainEventApplicationEvent(
    val domainEvent: DomainEvent,
) : ApplicationEvent(domainEvent)
