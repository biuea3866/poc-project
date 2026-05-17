package com.hrplatform.kafka.publisher

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.event.DomainEventPublisher

/**
 * 테스트 더블 — DomainEventPublisher의 in-memory 구현체.
 *
 * 도메인 서비스 단위 테스트에서 발행 이벤트를 캡처해 검증하는 용도. 운영 코드에서 주입하면 안 됩니다.
 */
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
