package com.hrplatform.core.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.hrplatform.core.util.ZonedDateTimes
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class DomainEventEnvelopeTest : BehaviorSpec({

    val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    given("DomainEvent 구현체를 DomainEventEnvelope.from()으로 변환할 때") {
        val eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val occurredAt = ZonedDateTime.of(2026, 5, 17, 10, 0, 0, 123456000, ZoneOffset.UTC)

        val event = TestFullDomainEvent(
            eventId = eventId,
            occurredAt = occurredAt,
            aggregateId = 42L,
            companyId = 100L,
            actorEmploymentId = 7L,
        )

        val envelope = DomainEventEnvelope.from(event)

        `when`("eventId를 확인하면") {
            then("UUID 문자열로 변환된다") {
                envelope.eventId shouldBe eventId.toString()
            }
        }

        `when`("eventType을 확인하면") {
            then("이벤트 타입 문자열과 일치한다") {
                envelope.eventType shouldBe "EmployeeHired"
            }
        }

        `when`("eventVersion을 확인하면") {
            then("버전 1과 일치한다") {
                envelope.eventVersion shouldBe 1
            }
        }

        `when`("occurredAt을 확인하면") {
            then("ISO-8601 마이크로초 UTC 문자열로 변환된다") {
                envelope.occurredAt shouldBe ZonedDateTimes.toIso8601(occurredAt)
            }
        }

        `when`("aggregateType을 확인하면") {
            then("Employment 문자열과 일치한다") {
                envelope.aggregateType shouldBe "Employment"
            }
        }

        `when`("aggregateId를 확인하면") {
            then("42L과 일치한다") {
                envelope.aggregateId shouldBe 42L
            }
        }

        `when`("companyId를 확인하면") {
            then("100L과 일치한다") {
                envelope.companyId shouldBe 100L
            }
        }

        `when`("actorEmploymentId를 확인하면") {
            then("7L과 일치한다") {
                envelope.actorEmploymentId shouldBe 7L
            }
        }

        `when`("action을 확인하면") {
            then("action.type이 HIRE와 일치한다") {
                envelope.action.type shouldBe "HIRE"
            }
            then("action.details에 department가 포함된다") {
                envelope.action.details["department"] shouldBe "Engineering"
            }
        }

        `when`("state를 확인하면") {
            then("state.status가 ACTIVE와 일치한다") {
                envelope.state.status shouldBe "ACTIVE"
            }
            then("state.snapshot에 employmentId가 포함된다") {
                envelope.state.snapshot["employmentId"] shouldBe 42L
            }
        }
    }

    given("actorEmploymentId가 null인 시스템 발행 이벤트를") {
        val event = TestFullDomainEvent(
            eventId = UUID.randomUUID(),
            occurredAt = ZonedDateTimes.nowUtc(),
            aggregateId = 1L,
            companyId = 10L,
            actorEmploymentId = null,
        )
        val envelope = DomainEventEnvelope.from(event)

        `when`("DomainEventEnvelope.from()으로 변환하면") {
            then("actorEmploymentId가 null이다") {
                envelope.actorEmploymentId shouldBe null
            }
        }
    }

    given("DomainEventEnvelope을 Jackson으로 직렬화할 때") {
        val event = TestFullDomainEvent(
            eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            occurredAt = ZonedDateTimes.nowUtc(),
            aggregateId = 42L,
            companyId = 100L,
            actorEmploymentId = 7L,
        )
        val envelope = DomainEventEnvelope.from(event)
        val json = objectMapper.writeValueAsString(envelope)
        val tree = objectMapper.readTree(json)

        `when`("JSON 필수 필드를 확인하면") {
            then("eventId 필드가 존재한다") {
                tree.has("eventId") shouldBe true
            }
            then("eventType 필드가 존재한다") {
                tree.has("eventType") shouldBe true
            }
            then("action.type 필드가 존재한다") {
                tree.get("action").has("type") shouldBe true
            }
            then("state.status 필드가 존재한다") {
                tree.get("state").has("status") shouldBe true
            }
            then("state.snapshot 필드가 존재한다") {
                tree.get("state").has("snapshot") shouldBe true
            }
        }
    }
})

private data class TestFullDomainEvent(
    override val eventId: UUID,
    override val occurredAt: ZonedDateTime,
    override val aggregateId: Long,
    override val companyId: Long,
    override val actorEmploymentId: Long?,
    override val eventType: String = "EmployeeHired",
    override val eventVersion: Int = 1,
    override val aggregateType: String = "Employment",
    override val action: DomainEventAction = TestAction(),
    override val state: DomainEventState = TestState(aggregateId),
) : DomainEvent

private data class TestAction(
    override val type: String = "HIRE",
    override val details: Map<String, Any?> = mapOf("department" to "Engineering"),
) : DomainEventAction

private data class TestState(
    val employmentId: Long,
    override val status: String = "ACTIVE",
    override val snapshot: Map<String, Any?> = mapOf("employmentId" to employmentId),
) : DomainEventState
