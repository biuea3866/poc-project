package com.hrplatform.core.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

class AggregateRootTest : BehaviorSpec({

    given("AggregateRoot 구현체에 DomainEvent가 적재될 때") {
        val aggregate = TestAggregate(id = 1L)

        `when`("addDomainEvent을 2번 호출하면") {
            val event1 = TestDomainEvent("EVENT_1")
            val event2 = TestDomainEvent("EVENT_2")
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
        aggregate.addTestEvent(TestDomainEvent("EVENT_ONCE"))

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

private class TestAggregate(id: Long) : AggregateRoot(
    id = id,
    createdAt = ZonedDateTime.now(),
    updatedAt = ZonedDateTime.now(),
) {
    fun addTestEvent(event: DomainEvent) {
        addDomainEvent(event)
    }
}

private data class TestDomainEvent(
    override val eventType: String,
    override val occurredAt: ZonedDateTime = ZonedDateTime.now(),
) : DomainEvent
