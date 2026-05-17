package com.hrplatform.core.domain

import jakarta.persistence.Transient
import java.time.ZonedDateTime

abstract class AggregateRoot(
    id: Long?,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
) : BaseEntity(id, createdAt, updatedAt) {

    @Transient
    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    @Synchronized
    protected fun addDomainEvent(event: DomainEvent) {
        _domainEvents.add(event)
    }

    @Synchronized
    fun pullDomainEvents(): List<DomainEvent> {
        val events = _domainEvents.toList()
        _domainEvents.clear()
        return events
    }
}
