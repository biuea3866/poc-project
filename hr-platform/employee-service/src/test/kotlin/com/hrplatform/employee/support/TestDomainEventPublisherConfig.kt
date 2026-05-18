package com.hrplatform.employee.support

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.event.DomainEventPublisher
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestDomainEventPublisherConfig {

    @Bean
    @Primary
    fun domainEventPublisher(): DomainEventPublisher = NoOpDomainEventPublisher()
}

internal class NoOpDomainEventPublisher : DomainEventPublisher {
    override fun publish(event: DomainEvent) = Unit
    override fun publishAll(events: List<DomainEvent>) = Unit
}
