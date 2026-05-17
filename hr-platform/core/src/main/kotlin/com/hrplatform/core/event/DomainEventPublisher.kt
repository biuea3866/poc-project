package com.hrplatform.core.event

import com.hrplatform.core.domain.DomainEvent

/**
 * 도메인 이벤트 발행 추상화.
 *
 * `domain` layer가 의존하는 interface로, ADR-001 §3 의존 방향에 따라 `core/event/`에 정의합니다.
 * Kafka 구현체(`KafkaDomainEventPublisher`)는 `common-kafka`에서 별도 작성하고, 도메인 서비스는
 * 이 interface에만 의존해 Kafka 모듈을 import 하지 않도록 합니다.
 */
interface DomainEventPublisher {
    fun publish(event: DomainEvent)

    fun publishAll(events: List<DomainEvent>)
}
