package com.hrplatform.kafka.publisher

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventAction
import com.hrplatform.core.domain.DomainEventState
import com.hrplatform.core.util.ZonedDateTimes
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime
import java.util.UUID

class InMemoryDomainEventPublisherTest : BehaviorSpec({

    given("InMemoryDomainEventPublisher") {
        `when`("publishAll로 3건을 발행하면") {
            val publisher = InMemoryDomainEventPublisher()
            val events = listOf(
                TestEvent(eventType = "E1"),
                TestEvent(eventType = "E2"),
                TestEvent(eventType = "E3"),
            )
            publisher.publishAll(events)

            then("published 리스트가 3건을 입력 순서대로 보존한다") {
                publisher.published shouldHaveSize 3
                publisher.published[0].eventType shouldBe "E1"
                publisher.published[1].eventType shouldBe "E2"
                publisher.published[2].eventType shouldBe "E3"
            }
        }

        `when`("publish를 1건 호출하면") {
            val publisher = InMemoryDomainEventPublisher()
            publisher.publish(TestEvent(eventType = "SINGLE"))

            then("published 리스트에 1건이 존재한다") {
                publisher.published shouldHaveSize 1
                publisher.published[0].eventType shouldBe "SINGLE"
            }
        }

        `when`("clear() 호출 후") {
            val publisher = InMemoryDomainEventPublisher()
            publisher.publish(TestEvent(eventType = "BEFORE_CLEAR"))
            publisher.clear()

            then("published 리스트가 비어있다") {
                publisher.published.shouldBeEmpty()
            }
        }
    }
})

private data class TestEvent(
    override val eventType: String,
    override val eventId: UUID = UUID.randomUUID(),
    override val eventVersion: Int = 1,
    override val occurredAt: ZonedDateTime = ZonedDateTimes.nowUtc(),
    override val aggregateType: String = "TestAggregate",
    override val aggregateId: Long = 0L,
    override val companyId: Long = 0L,
    override val actorEmploymentId: Long? = null,
    override val action: DomainEventAction = TestEventAction(),
    override val state: DomainEventState = TestEventState(),
) : DomainEvent

private data class TestEventAction(
    override val type: String = "TEST_ACTION",
    override val details: Map<String, Any?> = emptyMap(),
) : DomainEventAction

private data class TestEventState(
    override val status: String = "ACTIVE",
    override val snapshot: Map<String, Any?> = emptyMap(),
) : DomainEventState
