package com.hrplatform.core.domain

import com.hrplatform.core.util.ZonedDateTimes
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime
import java.util.UUID

class AggregateRootTest : BehaviorSpec({

    given("AggregateRoot 구현체에 DomainEvent가 적재될 때") {
        val aggregate = TestAggregate(id = 1L)

        `when`("addDomainEvent을 2번 호출하면") {
            val event1 = TestDomainEvent(eventType = "EVENT_1")
            val event2 = TestDomainEvent(eventType = "EVENT_2")
            aggregate.addTestEvent(event1)
            aggregate.addTestEvent(event2)

            then("pullDomainEvents는 2건을 순서대로 반환한다") {
                val events = aggregate.pullDomainEvents()
                events shouldHaveSize 2
                events[0] shouldBe event1
                events[1] shouldBe event2
            }
        }
    }

    given("pullDomainEvents 호출 이후") {
        val aggregate = TestAggregate(id = 2L)
        aggregate.addTestEvent(TestDomainEvent(eventType = "EVENT_ONCE"))

        `when`("pullDomainEvents를 첫 번째 호출하면") {
            val firstPull = aggregate.pullDomainEvents()

            then("1건을 반환한다") {
                firstPull shouldHaveSize 1
            }

            then("두 번째 pullDomainEvents 호출은 빈 리스트를 반환한다 (멱등성)") {
                val secondPull = aggregate.pullDomainEvents()
                secondPull.shouldBeEmpty()
            }
        }
    }

    given("DomainEvent를 한 건도 적재하지 않은 AggregateRoot에서") {
        val aggregate = TestAggregate(id = 3L)

        `when`("pullDomainEvents를 호출하면") {
            val events = aggregate.pullDomainEvents()

            then("빈 리스트를 반환한다") {
                events.shouldBeEmpty()
            }
        }
    }
})

<<<<<<< HEAD
private class TestAggregate(id: Long) : AggregateRoot(
    id = id,
    createdAt = ZonedDateTimes.nowUtc(),
    updatedAt = ZonedDateTimes.nowUtc(),
) {
=======
private class TestAggregate(id: Long) : AggregateRoot() {
    init {
        this.id = id
    }

>>>>>>> 9f35da15 (feat(core): BaseEntity에 createdBy/updatedBy + soft-delete 통합 (CM-03))
    fun addTestEvent(event: DomainEvent) {
        addDomainEvent(event)
    }
}

private data class TestDomainEvent(
    override val eventType: String,
    override val eventId: UUID = UUID.randomUUID(),
    override val eventVersion: Int = 1,
    override val occurredAt: ZonedDateTime = ZonedDateTimes.nowUtc(),
    override val aggregateType: String = "TestAggregate",
    override val aggregateId: Long = 0L,
    override val companyId: Long = 0L,
    override val actorEmploymentId: Long? = null,
    override val action: DomainEventAction = TestDomainEventAction(),
    override val state: DomainEventState = TestDomainEventState(),
) : DomainEvent

private data class TestDomainEventAction(
    override val type: String = "TEST_ACTION",
    override val details: Map<String, Any?> = emptyMap(),
) : DomainEventAction

private data class TestDomainEventState(
    override val status: String = "ACTIVE",
    override val snapshot: Map<String, Any?> = emptyMap(),
) : DomainEventState
