package com.closet.common.kafka

interface DomainEventPublisher {
    fun publish(topic: String, event: DomainEvent)
}
