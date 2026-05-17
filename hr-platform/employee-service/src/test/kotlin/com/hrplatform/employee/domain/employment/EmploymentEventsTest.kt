package com.hrplatform.employee.domain.employment

import com.hrplatform.employee.domain.employment.event.EmployeeHiredEvent
import com.hrplatform.employee.domain.employment.event.EmployeePromotedCancelledEvent
import com.hrplatform.employee.domain.employment.event.EmployeePromotedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeResignedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeResumedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeSalaryChangedCancelledEvent
import com.hrplatform.employee.domain.employment.event.EmployeeSalaryChangedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeSuspendedCancelledEvent
import com.hrplatform.employee.domain.employment.event.EmployeeSuspendedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeTransferredCancelledEvent
import com.hrplatform.employee.domain.employment.event.EmployeeTransferredEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 13종 DomainEvent 페이로드 — KF-02 JSON Schema 정합 검증
 *
 * 모든 이벤트가 DomainEvent interface를 완전 구현하는지,
 * action.type / state.status / state.snapshot 필드가 Schema와 일치하는지 확인한다.
 */
class EmploymentEventsTest : BehaviorSpec({

    val now: ZonedDateTime = ZonedDateTime.parse("2026-01-15T09:00:00+09:00")
    val actorId: Long = 100L

    fun buildPreHiredEmployment(): Employment = Employment(
        personId = 1L,
        companyId = 10L,
        employeeNumber = "EMP-001",
        employmentType = EmploymentType.REGULAR,
        status = EmploymentStatus.PRE_HIRED,
        startDate = LocalDate.of(2026, 1, 15),
        country = "KR",
        currency = "KRW",
        timezone = "Asia/Seoul",
        departmentId = 5L,
        managerEmploymentId = 50L,
    )

    given("EmployeeHiredEvent") {
        val employment = buildPreHiredEmployment()
        employment.activate(now, actorId)
        val event = employment.pullDomainEvents()[0] as EmployeeHiredEvent
        then("eventType = EmployeeHired") { event.eventType shouldBe "EmployeeHired" }
        then("aggregateType = Employment") { event.aggregateType shouldBe "Employment" }
        then("action.type = HIRE") { event.action.type shouldBe "HIRE" }
        then("action.details.personId 포함") { event.action.details["personId"] shouldBe 1L }
        then("action.details.startDate 포함") { event.action.details["startDate"] shouldBe "2026-01-15" }
        then("action.details.employmentType 포함") { event.action.details["employmentType"] shouldBe "REGULAR" }
        then("state.status = ACTIVE") { event.state.status shouldBe "ACTIVE" }
        then("state.snapshot.departmentId 포함") { event.state.snapshot["departmentId"] shouldBe 5L }
        then("state.snapshot.country 포함") { event.state.snapshot["country"] shouldBe "KR" }
        then("state.snapshot.currency 포함") { event.state.snapshot["currency"] shouldBe "KRW" }
        then("state.snapshot.timezone 포함") { event.state.snapshot["timezone"] shouldBe "Asia/Seoul" }
        then("eventId UUID not null") { event.eventId shouldNotBe null }
        then("eventVersion = 1") { event.eventVersion shouldBe 1 }
        then("companyId = 10") { event.companyId shouldBe 10L }
        then("actorEmploymentId = 100") { event.actorEmploymentId shouldBe actorId }
    }

    given("EmployeeResignedEvent") {
        val employment = buildPreHiredEmployment()
        employment.activate(now, actorId)
        employment.pullDomainEvents()
        employment.resign(now, "개인 사유", actorId)
        val event = employment.pullDomainEvents()[0] as EmployeeResignedEvent
        then("eventType = EmployeeResigned") { event.eventType shouldBe "EmployeeResigned" }
        then("action.type = RESIGN") { event.action.type shouldBe "RESIGN" }
        then("action.details.reason 포함") { event.action.details["reason"] shouldBe "개인 사유" }
        then("action.details.effectiveDate 포함") { event.action.details["effectiveDate"] shouldNotBe null }
        then("state.status = RESIGNED") { event.state.status shouldBe "RESIGNED" }
    }

    given("EmployeeSuspendedEvent") {
        val employment = buildPreHiredEmployment()
        employment.activate(now, actorId)
        employment.pullDomainEvents()
        employment.suspend("병가", LocalDate.of(2026, 2, 28), now, actorId)
        val event = employment.pullDomainEvents()[0] as EmployeeSuspendedEvent
        then("eventType = EmployeeSuspended") { event.eventType shouldBe "EmployeeSuspended" }
        then("action.type = SUSPEND") { event.action.type shouldBe "SUSPEND" }
        then("action.details.reason 포함") { event.action.details["reason"] shouldBe "병가" }
        then("action.details.until 포함") { event.action.details["until"] shouldBe "2026-02-28" }
        then("state.status = ON_LEAVE") { event.state.status shouldBe "ON_LEAVE" }
    }

    given("EmployeeResumedEvent") {
        val employment = buildPreHiredEmployment()
        employment.activate(now, actorId)
        employment.suspend("병가", null, now, actorId)
        employment.pullDomainEvents()
        employment.resume(now, actorId)
        val event = employment.pullDomainEvents()[0] as EmployeeResumedEvent
        then("eventType = EmployeeResumed") { event.eventType shouldBe "EmployeeResumed" }
        then("action.type = RESUME") { event.action.type shouldBe "RESUME" }
        then("action.details.resumedAt 포함") { event.action.details["resumedAt"] shouldNotBe null }
        then("state.status = ACTIVE") { event.state.status shouldBe "ACTIVE" }
    }

    given("EmployeeTransferredEvent") {
        val employment = buildPreHiredEmployment()
        employment.activate(now, actorId)
        employment.pullDomainEvents()
        employment.transferTo(newDepartmentId = 20L, now = now, actorEmploymentId = actorId)
        val event = employment.pullDomainEvents()[0] as EmployeeTransferredEvent
        then("eventType = EmployeeTransferred") { event.eventType shouldBe "EmployeeTransferred" }
        then("action.type = TRANSFER") { event.action.type shouldBe "TRANSFER" }
        then("action.details.oldDepartmentId 포함") { event.action.details["oldDepartmentId"] shouldBe 5L }
        then("action.details.newDepartmentId 포함") { event.action.details["newDepartmentId"] shouldBe 20L }
        then("state.status = ACTIVE") { event.state.status shouldBe "ACTIVE" }
        then("state.snapshot.departmentId = 20") { event.state.snapshot["departmentId"] shouldBe 20L }
    }

    given("EmployeePromotedEvent") {
        val employment = buildPreHiredEmployment()
        employment.positionId = 3L
        employment.activate(now, actorId)
        employment.pullDomainEvents()
        employment.promote(newPositionId = 7L, now = now, actorEmploymentId = actorId)
        val event = employment.pullDomainEvents()[0] as EmployeePromotedEvent
        then("eventType = EmployeePromoted") { event.eventType shouldBe "EmployeePromoted" }
        then("action.type = PROMOTE") { event.action.type shouldBe "PROMOTE" }
        then("action.details.oldPositionId 포함") { event.action.details["oldPositionId"] shouldBe 3L }
        then("action.details.newPositionId 포함") { event.action.details["newPositionId"] shouldBe 7L }
        then("state.status = ACTIVE") { event.state.status shouldBe "ACTIVE" }
    }

    given("EmployeeSalaryChangedEvent") {
        val employment = buildPreHiredEmployment()
        employment.baseSalary = 3_000_000L
        employment.compensationCurrency = "KRW"
        employment.activate(now, actorId)
        employment.pullDomainEvents()
        employment.changeCompensation(newBaseSalary = 4_000_000L, newCurrency = "KRW", now = now, actorEmploymentId = actorId)
        val event = employment.pullDomainEvents()[0] as EmployeeSalaryChangedEvent
        then("eventType = EmployeeSalaryChanged") { event.eventType shouldBe "EmployeeSalaryChanged" }
        then("action.type = SALARY_CHANGE") { event.action.type shouldBe "SALARY_CHANGE" }
        then("action.details.oldBaseSalary 포함") { event.action.details["oldBaseSalary"] shouldBe 3_000_000L }
        then("action.details.newBaseSalary 포함") { event.action.details["newBaseSalary"] shouldBe 4_000_000L }
        then("action.details.currency 포함") { event.action.details["currency"] shouldBe "KRW" }
        then("state.status = ACTIVE") { event.state.status shouldBe "ACTIVE" }
    }

    given("EmployeeTransferredCancelledEvent") {
        val employment = buildPreHiredEmployment()
        employment.activate(now, actorId)
        employment.pullDomainEvents()
        employment.cancelLastTransfer(
            cancelledHistoryId = 99L,
            cancellationReason = "오기재",
            previousDepartmentId = 3L,
            now = now,
            actorEmploymentId = actorId,
        )
        val event = employment.pullDomainEvents()[0] as EmployeeTransferredCancelledEvent
        then("eventType = EmployeeTransferredCancelled") { event.eventType shouldBe "EmployeeTransferredCancelled" }
        then("action.type = TRANSFER_CANCELLED") { event.action.type shouldBe "TRANSFER_CANCELLED" }
        then("action.details.cancelledHistoryId 포함") { event.action.details["cancelledHistoryId"] shouldBe 99L }
        then("action.details.cancellationReason 포함") { event.action.details["cancellationReason"] shouldBe "오기재" }
        then("state.status = ACTIVE") { event.state.status shouldBe "ACTIVE" }
    }

    given("EmployeePromotedCancelledEvent") {
        val employment = buildPreHiredEmployment()
        employment.activate(now, actorId)
        employment.pullDomainEvents()
        employment.cancelLastPromotion(
            cancelledHistoryId = 99L,
            cancellationReason = "오기재",
            previousPositionId = 3L,
            now = now,
            actorEmploymentId = actorId,
        )
        val event = employment.pullDomainEvents()[0] as EmployeePromotedCancelledEvent
        then("eventType = EmployeePromotedCancelled") { event.eventType shouldBe "EmployeePromotedCancelled" }
        then("action.type = PROMOTE_CANCELLED") { event.action.type shouldBe "PROMOTE_CANCELLED" }
        then("action.details.cancelledHistoryId 포함") { event.action.details["cancelledHistoryId"] shouldBe 99L }
        then("state.status = ACTIVE") { event.state.status shouldBe "ACTIVE" }
    }

    given("EmployeeSalaryChangedCancelledEvent") {
        val employment = buildPreHiredEmployment()
        employment.baseSalary = 4_000_000L
        employment.compensationCurrency = "KRW"
        employment.activate(now, actorId)
        employment.pullDomainEvents()
        employment.cancelLastSalaryChange(
            cancelledHistoryId = 99L,
            cancellationReason = "오기재",
            previousBaseSalary = 3_000_000L,
            previousCurrency = "KRW",
            now = now,
            actorEmploymentId = actorId,
        )
        val event = employment.pullDomainEvents()[0] as EmployeeSalaryChangedCancelledEvent
        then("eventType = EmployeeSalaryChangedCancelled") { event.eventType shouldBe "EmployeeSalaryChangedCancelled" }
        then("action.type = SALARY_CHANGE_CANCELLED") { event.action.type shouldBe "SALARY_CHANGE_CANCELLED" }
        then("action.details.cancelledHistoryId 포함") { event.action.details["cancelledHistoryId"] shouldBe 99L }
        then("state.status = ACTIVE") { event.state.status shouldBe "ACTIVE" }
    }

    given("EmployeeSuspendedCancelledEvent") {
        val employment = buildPreHiredEmployment()
        employment.activate(now, actorId)
        employment.suspend("병가", LocalDate.of(2026, 2, 28), now, actorId)
        employment.pullDomainEvents()
        employment.cancelLastSuspend(
            cancelledHistoryId = 99L,
            cancellationReason = "오기재",
            previousSuspendReason = "병가",
            previousSuspendUntil = LocalDate.of(2026, 2, 28),
            now = now,
            actorEmploymentId = actorId,
        )
        val event = employment.pullDomainEvents()[0] as EmployeeSuspendedCancelledEvent
        then("eventType = EmployeeSuspendedCancelled") { event.eventType shouldBe "EmployeeSuspendedCancelled" }
        then("action.type = SUSPEND_CANCELLED") { event.action.type shouldBe "SUSPEND_CANCELLED" }
        then("action.details.cancelledHistoryId 포함") { event.action.details["cancelledHistoryId"] shouldBe 99L }
        then("action.details.cancellationReason 포함") { event.action.details["cancellationReason"] shouldBe "오기재" }
        then("state.status = ACTIVE — 휴직 취소 후 복귀") { event.state.status shouldBe "ACTIVE" }
    }
})
