package com.closet.common.kafka.idempotency

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class IdempotencyServiceTest : BehaviorSpec({

    Given("이벤트가 아직 처리되지 않았을 때") {
        val eventId = "event-001"
        val consumerGroup = "order-service"

        Then("isProcessed는 false를 반환한다") {
            val repository = mockk<ProcessedEventRepository>()
            val service = IdempotencyService(repository)
            every { repository.existsByEventIdAndConsumerGroup(eventId, consumerGroup) } returns false

            val result = service.isProcessed(eventId, consumerGroup)

            result shouldBe false
        }

        Then("markProcessed를 호출하면 ProcessedEvent가 저장된다") {
            val repository = mockk<ProcessedEventRepository>()
            val service = IdempotencyService(repository)
            every { repository.existsByEventIdAndConsumerGroup(eventId, consumerGroup) } returns false
            val eventSlot = slot<ProcessedEvent>()
            every { repository.save(capture(eventSlot)) } answers { eventSlot.captured }

            service.markProcessed(eventId, consumerGroup)

            verify(exactly = 1) { repository.save(any()) }
            eventSlot.captured.eventId shouldBe eventId
            eventSlot.captured.consumerGroup shouldBe consumerGroup
        }
    }

    Given("이벤트가 이미 처리되었을 때") {
        val eventId = "event-002"
        val consumerGroup = "order-service"

        Then("isProcessed는 true를 반환한다") {
            val repository = mockk<ProcessedEventRepository>()
            val service = IdempotencyService(repository)
            every { repository.existsByEventIdAndConsumerGroup(eventId, consumerGroup) } returns true

            val result = service.isProcessed(eventId, consumerGroup)

            result shouldBe true
        }

        Then("markProcessed를 호출해도 중복 저장하지 않는다") {
            val repository = mockk<ProcessedEventRepository>()
            val service = IdempotencyService(repository)
            every { repository.existsByEventIdAndConsumerGroup(eventId, consumerGroup) } returns true

            service.markProcessed(eventId, consumerGroup)

            verify(exactly = 0) { repository.save(any<ProcessedEvent>()) }
        }
    }
})
