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
import io.kotest.matchers.string.shouldNotBeEmpty
import java.time.LocalDate
import java.time.ZonedDateTime

class EmploymentEventsTest : BehaviorSpec({

    val now: ZonedDateTime = ZonedDateTime.now()
    val employmentId = 1L
    val companyId = 10L

    given("EmployeeHiredEvent 생성 시") {
        val event = EmployeeHiredEvent(
            employmentId = employmentId,
            companyId = companyId,
            personId = 100L,
            employeeNumber = "EMP-001",
            occurredAt = now,
        )

        then("occurredAt이 설정된다") {
            event.occurredAt shouldBe now
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
        then("employmentId가 일치한다") {
            event.employmentId shouldBe employmentId
        }
        then("companyId가 일치한다") {
            event.companyId shouldBe companyId
        }
    }

    given("EmployeeSuspendedEvent 생성 시") {
        val event = EmployeeSuspendedEvent(
            employmentId = employmentId,
            companyId = companyId,
            reason = "건강 문제",
            until = LocalDate.of(2026, 8, 31),
            occurredAt = now,
        )

        then("reason이 설정된다") {
            event.reason shouldBe "건강 문제"
        }
        then("until이 설정된다") {
            event.until shouldBe LocalDate.of(2026, 8, 31)
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }

    given("EmployeeResumedEvent 생성 시") {
        val event = EmployeeResumedEvent(
            employmentId = employmentId,
            companyId = companyId,
            occurredAt = now,
        )

        then("occurredAt이 설정된다") {
            event.occurredAt shouldBe now
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }

    given("EmployeeResignedEvent 생성 시") {
        val event = EmployeeResignedEvent(
            employmentId = employmentId,
            companyId = companyId,
            reason = "개인 사유",
            occurredAt = now,
        )

        then("reason이 설정된다") {
            event.reason shouldBe "개인 사유"
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }

    given("EmployeeTransferredEvent 생성 시") {
        val event = EmployeeTransferredEvent(
            employmentId = employmentId,
            companyId = companyId,
            previousDepartmentId = 5L,
            newDepartmentId = 10L,
            occurredAt = now,
        )

        then("previousDepartmentId가 설정된다") {
            event.previousDepartmentId shouldBe 5L
        }
        then("newDepartmentId가 설정된다") {
            event.newDepartmentId shouldBe 10L
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }

    given("EmployeePromotedEvent 생성 시") {
        val event = EmployeePromotedEvent(
            employmentId = employmentId,
            companyId = companyId,
            previousPositionId = 3L,
            newPositionId = 4L,
            occurredAt = now,
        )

        then("previousPositionId가 설정된다") {
            event.previousPositionId shouldBe 3L
        }
        then("newPositionId가 설정된다") {
            event.newPositionId shouldBe 4L
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }

    given("EmployeeSalaryChangedEvent 생성 시") {
        val event = EmployeeSalaryChangedEvent(
            employmentId = employmentId,
            companyId = companyId,
            previousBaseSalary = 3_000_000L,
            newBaseSalary = 3_500_000L,
            currency = "KRW",
            occurredAt = now,
        )

        then("previousBaseSalary가 설정된다") {
            event.previousBaseSalary shouldBe 3_000_000L
        }
        then("newBaseSalary가 설정된다") {
            event.newBaseSalary shouldBe 3_500_000L
        }
        then("currency가 설정된다") {
            event.currency shouldBe "KRW"
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }

    given("보상 이벤트 — EmployeeTransferredCancelledEvent 생성 시") {
        val event = EmployeeTransferredCancelledEvent(
            employmentId = employmentId,
            companyId = companyId,
            cancelledDepartmentId = 10L,
            restoredDepartmentId = 5L,
            occurredAt = now,
        )

        then("cancelledDepartmentId가 설정된다") {
            event.cancelledDepartmentId shouldBe 10L
        }
        then("restoredDepartmentId가 설정된다") {
            event.restoredDepartmentId shouldBe 5L
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }

    given("보상 이벤트 — EmployeePromotedCancelledEvent 생성 시") {
        val event = EmployeePromotedCancelledEvent(
            employmentId = employmentId,
            companyId = companyId,
            cancelledPositionId = 4L,
            restoredPositionId = 3L,
            occurredAt = now,
        )

        then("cancelledPositionId가 설정된다") {
            event.cancelledPositionId shouldBe 4L
        }
        then("restoredPositionId가 설정된다") {
            event.restoredPositionId shouldBe 3L
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }

    given("보상 이벤트 — EmployeeSalaryChangedCancelledEvent 생성 시") {
        val event = EmployeeSalaryChangedCancelledEvent(
            employmentId = employmentId,
            companyId = companyId,
            cancelledBaseSalary = 3_500_000L,
            restoredBaseSalary = 3_000_000L,
            currency = "KRW",
            occurredAt = now,
        )

        then("cancelledBaseSalary가 설정된다") {
            event.cancelledBaseSalary shouldBe 3_500_000L
        }
        then("restoredBaseSalary가 설정된다") {
            event.restoredBaseSalary shouldBe 3_000_000L
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }

    given("보상 이벤트 — EmployeeSuspendedCancelledEvent 생성 시") {
        val event = EmployeeSuspendedCancelledEvent(
            employmentId = employmentId,
            companyId = companyId,
            occurredAt = now,
        )

        then("occurredAt이 설정된다") {
            event.occurredAt shouldBe now
        }
        then("eventType이 비어 있지 않다") {
            event.eventType.shouldNotBeEmpty()
        }
    }
})
