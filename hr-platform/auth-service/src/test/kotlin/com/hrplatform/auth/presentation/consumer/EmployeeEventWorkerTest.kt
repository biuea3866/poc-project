package com.hrplatform.auth.presentation.consumer

import com.hrplatform.auth.application.auth.UserAccountSyncUseCase
import com.hrplatform.core.domain.DomainEventEnvelope
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class EmployeeEventWorkerTest : BehaviorSpec({

    fun buildEnvelope(eventType: String, aggregateId: Long = 1L): DomainEventEnvelope =
        DomainEventEnvelope(
            eventId = UUID.randomUUID().toString(),
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

    given("EmployeeHired 이벤트 수신") {
        then("consume 호출 시 userAccountSyncUseCase.syncHired가 호출된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = EmployeeEventWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeeHired"))

            verify { userAccountSyncUseCase.syncHired(any()) }
        }
    }

    given("EmployeeResigned 이벤트 수신") {
        then("consume 호출 시 userAccountSyncUseCase.syncResigned가 호출된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = EmployeeEventWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeeResigned"))

            verify { userAccountSyncUseCase.syncResigned(any()) }
        }
    }

    given("EmployeeSuspended 이벤트 수신") {
        then("consume 호출 시 userAccountSyncUseCase.syncSuspended가 호출된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = EmployeeEventWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeeSuspended"))

            verify { userAccountSyncUseCase.syncSuspended(any()) }
        }
    }

    given("EmployeeResumed 이벤트 수신") {
        then("consume 호출 시 userAccountSyncUseCase.syncResumed가 호출된다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = EmployeeEventWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeeResumed"))

            verify { userAccountSyncUseCase.syncResumed(any()) }
        }
    }

    given("미지원 이벤트 타입 수신") {
        then("consume 호출 시 어떤 useCase 메서드도 호출되지 않는다") {
            val userAccountSyncUseCase = mockk<UserAccountSyncUseCase>(relaxed = true)
            val worker = EmployeeEventWorker(userAccountSyncUseCase)

            worker.consume(buildEnvelope("EmployeePromoted"))

            verify(exactly = 0) { userAccountSyncUseCase.syncHired(any()) }
            verify(exactly = 0) { userAccountSyncUseCase.syncResigned(any()) }
            verify(exactly = 0) { userAccountSyncUseCase.syncSuspended(any()) }
            verify(exactly = 0) { userAccountSyncUseCase.syncResumed(any()) }
        }
    }
})
