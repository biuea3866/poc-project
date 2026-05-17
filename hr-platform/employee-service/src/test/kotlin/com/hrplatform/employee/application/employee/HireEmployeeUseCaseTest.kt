package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.EmploymentType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HireEmployeeUseCaseTest : BehaviorSpec({

    val employmentDomainService = mockk<EmploymentDomainService>()
    val useCase = HireEmployeeUseCase(employmentDomainService)

    val now = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC)

    val command = HireEmployeeCommand(
        personalEmail = "new@example.com",
        name = "신입사원",
        birthDate = LocalDate.of(1998, 5, 10),
        nationality = null,
        gender = null,
        companyId = 1L,
        employeeNumber = "EMP-001",
        employmentType = EmploymentType.REGULAR,
        startDate = LocalDate.of(2026, 1, 10),
        country = "KR",
        currency = "KRW",
        timezone = "Asia/Seoul",
        departmentId = null,
        managerEmploymentId = null,
        actorEmploymentId = 100L,
    )

    given("HireEmployeeUseCase.execute") {
        `when`("정상적인 HireEmployeeCommand가 주어지면") {
            val mockEmployment = mockk<Employment> {
                every { id } returns 1L
                every { personId } returns 10L
                every { companyId } returns 1L
                every { employeeNumber } returns "EMP-001"
                every { employmentType } returns EmploymentType.REGULAR
                every { status } returns EmploymentStatus.ACTIVE
                every { startDate } returns LocalDate.of(2026, 1, 10)
            }
            every { employmentDomainService.hire(any(), now) } returns mockEmployment

            then("DomainService.hire를 정확히 1회 호출하고 결과를 반환한다") {
                val result = useCase.execute(command, now)

                verify(exactly = 1) { employmentDomainService.hire(any(), now) }
                result.employmentId shouldBe 1L
                result.status shouldBe EmploymentStatus.ACTIVE
            }
        }

        `when`("DomainService가 예외를 던지면") {
            every { employmentDomainService.hire(any(), now) } throws RuntimeException("도메인 예외")

            then("예외가 그대로 전파된다") {
                shouldThrow<RuntimeException> {
                    useCase.execute(command, now)
                }
            }
        }
    }
})
