package com.hrplatform.employee.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventAction
import com.hrplatform.core.domain.DomainEventState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.kafka.core.KafkaTemplate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

class KafkaDomainEventPublisherUnitTest : BehaviorSpec({

    val topic = "event.hr.employee.v1"
    val objectMapper = ObjectMapper().registerKotlinModule().also { mapper ->
        mapper.findAndRegisterModules()
    }

    given("단일 DomainEvent 발행") {
        val aggregateId = 42L
        val event = buildDomainEvent(aggregateId = aggregateId, eventType = "EmployeeHired")

        then("publish(event) 호출 시 kafkaTemplate.send가 정확히 1회 호출된다") {
            val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
            every { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) } returns
                CompletableFuture.completedFuture(mockk())
            val publisher = KafkaDomainEventPublisher(kafkaTemplate, objectMapper, topic)

            publisher.publish(event)

            verify(exactly = 1) { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) }
        }

        then("publish(event) 호출 시 토픽 이름이 event.hr.employee.v1이다") {
            val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
            every { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) } returns
                CompletableFuture.completedFuture(mockk())
            val publisher = KafkaDomainEventPublisher(kafkaTemplate, objectMapper, topic)
            val topicSlot = slot<String>()

            publisher.publish(event)

            verify { kafkaTemplate.send(capture(topicSlot), any<String>(), any<String>()) }
            topicSlot.captured shouldBe topic
        }

        then("publish(event) 호출 시 파티션 키가 aggregateId 문자열이다") {
            val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
            every { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) } returns
                CompletableFuture.completedFuture(mockk())
            val publisher = KafkaDomainEventPublisher(kafkaTemplate, objectMapper, topic)
            val keySlot = slot<String>()

            publisher.publish(event)

            verify { kafkaTemplate.send(any<String>(), capture(keySlot), any<String>()) }
            keySlot.captured shouldBe aggregateId.toString()
        }

        then("publish(event) 호출 시 페이로드가 DomainEventEnvelope JSON이다") {
            val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
            every { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) } returns
                CompletableFuture.completedFuture(mockk())
            val publisher = KafkaDomainEventPublisher(kafkaTemplate, objectMapper, topic)
            val valueSlot = slot<String>()

            publisher.publish(event)

            verify { kafkaTemplate.send(any<String>(), any<String>(), capture(valueSlot)) }
            val envelope = objectMapper.readValue(valueSlot.captured, Map::class.java)
            envelope["eventType"] shouldBe "EmployeeHired"
            envelope["aggregateId"] shouldBe aggregateId.toInt()
            (envelope.containsKey("action")) shouldBe true
            (envelope.containsKey("state")) shouldBe true
        }
    }

    given("13종 DomainEvent 일괄 발행 (publishAll)") {
        val events = EVENT_TYPES.mapIndexed { index, eventType ->
            buildDomainEvent(aggregateId = (index + 1).toLong(), eventType = eventType)
        }

        then("publishAll(13종) 호출 시 kafkaTemplate.send가 정확히 13회 호출된다") {
            val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
            every { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) } returns
                CompletableFuture.completedFuture(mockk())
            val publisher = KafkaDomainEventPublisher(kafkaTemplate, objectMapper, topic)

            publisher.publishAll(events)

            verify(exactly = 13) { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) }
        }
    }

    given("빈 리스트 publishAll") {
        then("publishAll(emptyList()) 호출 시 kafkaTemplate.send가 0회 호출된다") {
            val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
            every { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) } returns
                CompletableFuture.completedFuture(mockk())
            val publisher = KafkaDomainEventPublisher(kafkaTemplate, objectMapper, topic)

            publisher.publishAll(emptyList())

            verify(exactly = 0) { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) }
        }
    }
}) {
    companion object {
        val EVENT_TYPES: List<String> = listOf(
            "EmployeeHired",
            "EmployeeResigned",
            "EmployeeSuspended",
            "EmployeeResumed",
            "EmployeePromoted",
            "EmployeeTransferred",
            "EmployeeSalaryChanged",
            "DepartmentChanged",
            "DepartmentHeadChanged",
            "EmployeeTransferredCancelled",
            "EmployeePromotedCancelled",
            "EmployeeSalaryChangedCancelled",
            "EmployeeSuspendedCancelled",
        )

        fun buildDomainEvent(
            aggregateId: Long,
            eventType: String,
            companyId: Long = 1L,
        ): DomainEvent = object : DomainEvent {
            override val eventId: UUID = UUID.randomUUID()
            override val eventType: String = eventType
            override val eventVersion: Int = 1
            override val occurredAt: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)
            override val aggregateType: String = "Employment"
            override val aggregateId: Long = aggregateId
            override val companyId: Long = companyId
            override val actorEmploymentId: Long? = null
            override val action: DomainEventAction = object : DomainEventAction {
                override val type: String = "TEST_ACTION"
                override val details: Map<String, Any?> = mapOf("testKey" to "testValue")
            }
            override val state: DomainEventState = object : DomainEventState {
                override val status: String = "ACTIVE"
                override val snapshot: Map<String, Any?> = mapOf("field" to "value")
            }
        }
    }
}
