package com.hrplatform.auth.presentation.consumer

import com.hrplatform.auth.application.auth.UserAccountSyncUseCase
import com.hrplatform.core.domain.DomainEventEnvelope
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class EmployeeEventWorkerTest : BehaviorSpec({

    fun buildEnvelope(eventType: String, aggregateId: Long = 1L, eventId: String = UUID.randomUUID().toString()): DomainEventEnvelope =
        DomainEventEnvelope(
            eventId = eventId,
            eventType = eventType,
            eventVersion = 1,
            occurredAt = ZonedDateTime.now(ZoneOffset.UTC).toString(),
            aggregateType = "Employment",
            aggregateId = aggregateId,
            companyId = 1L,
            actorEmploymentId = null,
            action = DomainEventEnvelope.ActionEnvelope(type = eventType, details = mapOf("email" to "user@example.com")),
            state = DomainEventEnvelope.StateEnvelope(status = "ACTIVE", snapshot = emptyMap()),
        )

    fun buildWorker(userAccountSyncUseCase: UserAccountSyncUseCase, isNewEvent: Boolean = true): EmployeeEventWorker {
        val redisTemplate = mockk<StringRedisTemplate>()
        val valueOps = mockk<ValueOperations<String, String>>()
        every { redisTemplate.opsForValue() } returns valueOps
        every { valueOps.setIfAbsent(any(), any(), any()) } returns isNewEvent
        return EmployeeEventWorker(userAccountSyncUseCase, redisTemplate)
    }

    given("EmployeeHired 이벤트 수신") {
        then("consume 호출 시 userAccountSyncUseCase.syncHired가 호출된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = buildWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeeHired"))

            verify { userAccountSyncUseCase.syncHired(any()) }
        }
    }

    given("EmployeeResigned 이벤트 수신") {
        then("consume 호출 시 userAccountSyncUseCase.syncResigned가 호출된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = buildWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeeResigned"))

            verify { userAccountSyncUseCase.syncResigned(any()) }
        }
    }

    given("EmployeeSuspended 이벤트 수신") {
        then("consume 호출 시 userAccountSyncUseCase.syncSuspended가 호출된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = buildWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeeSuspended"))

            verify { userAccountSyncUseCase.syncSuspended(any()) }
        }
    }

    given("EmployeeResumed 이벤트 수신") {
        then("consume 호출 시 userAccountSyncUseCase.syncResumed가 호출된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = buildWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeeResumed"))

            verify { userAccountSyncUseCase.syncResumed(any()) }
        }
    }

    given("미지원 이벤트 타입 수신") {
        then("consume 호출 시 어떤 useCase 메서드도 호출되지 않는다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = buildWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeePromoted"))

            verify(exactly = 0) { userAccountSyncUseCase.syncHired(any()) }
            verify(exactly = 0) { userAccountSyncUseCase.syncResigned(any()) }
            verify(exactly = 0) { userAccountSyncUseCase.syncSuspended(any()) }
            verify(exactly = 0) { userAccountSyncUseCase.syncResumed(any()) }
        }
    }

    given("EmployeeTransferred 이벤트 수신") {
        then("consume 호출 시 userAccountSyncUseCase.syncTransferred가 호출된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = buildWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeeTransferred"))

            verify { userAccountSyncUseCase.syncTransferred(any()) }
        }
    }

    given("중복 이벤트 수신 (멱등성)") {
        then("같은 eventId의 이벤트를 두 번 수신하면 두 번째는 무시된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = buildWorker(userAccountSyncUseCase, isNewEvent = false)

            worker.consume(buildEnvelope("EmployeeHired", eventId = "duplicate-event-id"))

            verify(exactly = 0) { userAccountSyncUseCase.syncHired(any()) }
        }
    }
})
