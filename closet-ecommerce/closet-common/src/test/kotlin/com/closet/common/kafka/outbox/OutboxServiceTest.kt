package com.closet.common.kafka.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class OutboxServiceTest : BehaviorSpec({

    val outboxRepository = mockk<OutboxRepository>()
    val objectMapper = ObjectMapper()
    val outboxService = OutboxService(outboxRepository, objectMapper)

    Given("아웃박스 이벤트를 저장할 때") {
        val aggregateType = "Order"
        val aggregateId = "order-123"
        val eventType = "OrderCreated"
        val payload = mapOf("orderId" to "order-123", "amount" to 50000)

        val eventSlot = slot<OutboxEvent>()
        every { outboxRepository.save(capture(eventSlot)) } answers { eventSlot.captured }

        When("save를 호출하면") {
            outboxService.save(aggregateType, aggregateId, eventType, payload)

            Then("OutboxEvent가 PENDING 상태로 저장된다") {
                val saved = eventSlot.captured
                saved.aggregateType shouldBe "Order"
                saved.aggregateId shouldBe "order-123"
                saved.eventType shouldBe "OrderCreated"
                saved.status shouldBe OutboxStatus.PENDING
                saved.retryCount shouldBe 0
                saved.publishedAt shouldBe null
                saved.payload shouldNotBe null
            }

            Then("payload가 JSON으로 직렬화된다") {
                val saved = eventSlot.captured
                val deserialized = objectMapper.readValue(saved.payload, Map::class.java)
                deserialized["orderId"] shouldBe "order-123"
                deserialized["amount"] shouldBe 50000
            }

            Then("repository.save가 호출된다") {
                verify(exactly = 1) { outboxRepository.save(any()) }
            }
        }
    }
})
