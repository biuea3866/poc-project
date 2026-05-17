package com.hrplatform.kafka.publisher

import com.hrplatform.core.domain.DomainEvent

interface DomainEventPublisher {
    fun publish(event: DomainEvent)
    fun publishAll(events: List<DomainEvent>)
}
