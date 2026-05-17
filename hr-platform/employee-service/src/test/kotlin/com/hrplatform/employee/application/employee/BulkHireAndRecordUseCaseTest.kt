package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.EmploymentType
import com.hrplatform.employee.domain.employment.RecordableEventType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BulkHireAndRecordUseCaseTest : BehaviorSpec({

    val employmentDomainService = mockk<EmploymentDomainService>()
    val bulkHireUseCase = BulkHireUseCase(employmentDomainService)
    val bulkRecordUseCase = BulkRecordEmploymentEventsUseCase(employmentDomainService)

    val now = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC)

    given("BulkHireUseCase.execute") {
        val hireCommands = (1..3).map { index ->
            HireEmployeeCommand(
                personalEmail = "emp$index@example.com",
                name = "직원$index",
                birthDate = LocalDate.of(1998, 1, 1),
                nationality = null,
                gender = null,
                companyId = 1L,
                employeeNumber = "EMP-00$index",
                employmentType = EmploymentType.REGULAR,
                startDate = LocalDate.of(2026, 1, 10),
                country = "KR",
                currency = "KRW",
                timezone = "Asia/Seoul",
                departmentId = null,
                managerEmploymentId = null,
                actorEmploymentId = 100L,
            )
        }
        val bulkCommand = BulkHireCommand(commands = hireCommands)

        `when`("3명의 입사 일괄 처리를 요청하면") {
            val mockEmployment = mockk<Employment> {
                every { id } returns 1L
                every { status } returns EmploymentStatus.ACTIVE
            }
            every { employmentDomainService.hire(any(), now) } returns mockEmployment

            then("DomainService.hire를 3회 호출하고 successCount=3을 반환한다") {
                val result = bulkHireUseCase.execute(bulkCommand, now)

                verify(exactly = 3) { employmentDomainService.hire(any(), now) }
                result.successCount shouldBe 3
                result.failureCount shouldBe 0
                result.failures.isEmpty() shouldBe true
            }
        }

        `when`("DomainService가 예외를 던지면 트랜잭션이 롤백된다") {
            every { employmentDomainService.hire(any(), now) } throws RuntimeException("유효성 실패")

            then("예외가 전파되어 롤백을 유발한다") {
                shouldThrow<RuntimeException> { bulkHireUseCase.execute(bulkCommand, now) }
            }
        }
    }

    given("BulkRecordEmploymentEventsUseCase.execute") {
        val recordCommands = (1..2).map { index ->
            RecordEmploymentEventCommand(
                employmentId = index.toLong(),
                eventType = RecordableEventType.DEPT_CHANGE,
                newDepartmentId = 10L,
                effectiveDate = LocalDate.of(2026, 1, 10),
                actorEmploymentId = 100L,
            )
        }
        val bulkCommand = BulkRecordEmploymentEventsCommand(commands = recordCommands)

        `when`("2건의 발령 일괄 처리를 요청하면") {
            val mockEmployment = mockk<Employment> {
                every { id } returns 1L
                every { status } returns EmploymentStatus.ACTIVE
                every { departmentId } returns 10L
                every { positionId } returns null
                every { baseSalary } returns null
            }
            every { employmentDomainService.recordEvent(any(), now) } returns mockEmployment

            then("DomainService.recordEvent를 2회 호출하고 successCount=2를 반환한다") {
                val result = bulkRecordUseCase.execute(bulkCommand, now)

                verify(exactly = 2) { employmentDomainService.recordEvent(any(), now) }
                result.successCount shouldBe 2
                result.failureCount shouldBe 0
            }
        }
    }
})
