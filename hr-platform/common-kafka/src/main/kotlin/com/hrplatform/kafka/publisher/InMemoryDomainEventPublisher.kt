package com.hrplatform.kafka.publisher

import com.hrplatform.core.domain.DomainEvent

class InMemoryDomainEventPublisher : DomainEventPublisher {

    private val _published: MutableList<DomainEvent> = mutableListOf()

    val published: List<DomainEvent>
        get() = _published.toList()

    override fun publish(event: DomainEvent) {
        _published.add(event)
    }

    override fun publishAll(events: List<DomainEvent>) {
        _published.addAll(events)
    }

    fun clear() {
        _published.clear()
    }
}
