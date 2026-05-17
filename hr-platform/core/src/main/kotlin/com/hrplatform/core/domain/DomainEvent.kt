package com.hrplatform.core.domain

import java.time.ZonedDateTime

interface DomainEvent {
    val occurredAt: ZonedDateTime
    val eventType: String
}
