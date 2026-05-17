package com.hrplatform.employee.application.employee

import com.hrplatform.core.exception.ForbiddenException
import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.employment.EmploymentRole
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.EmploymentType
import com.hrplatform.employee.domain.history.EmploymentHistory
import com.hrplatform.employee.domain.history.EmploymentHistoryDomainService
import com.hrplatform.employee.domain.history.EmploymentHistoryEventType
import com.hrplatform.employee.domain.query.EmployeeQueryDomainService
import com.hrplatform.employee.domain.query.SearchCriteria
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class SearchAndGetEmployeeUseCaseTest : BehaviorSpec({

    val employeeQueryDomainService = mockk<EmployeeQueryDomainService>()
    val employmentDomainService = mockk<EmploymentDomainService>()
    val employmentHistoryDomainService = mockk<EmploymentHistoryDomainService>()

    val searchUseCase = SearchEmployeesUseCase(employeeQueryDomainService, employmentDomainService)
    val getEmployeeUseCase = GetEmployeeUseCase(employeeQueryDomainService, employmentDomainService)
    val getHistoryUseCase = GetEmploymentHistoryUseCase(
        employmentHistoryDomainService,
        employeeQueryDomainService,
        employmentDomainService,
    )

    val now = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC)

    given("SearchEmployeesUseCase.execute") {
        `when`("TEAM_LEAD viewer가 검색을 요청하면") {
            val command = SearchEmployeesCommand(
                viewerEmploymentId = 1L,
                companyId = 1L,
                keyword = null,
                departmentId = null,
                pageable = Pageable.ofSize(10),
            )

            val viewerEmployment = mockk<Employment> {
                every { id } returns 1L
                every { role } returns EmploymentRole.TEAM_LEAD
                every { status } returns EmploymentStatus.ACTIVE
            }
            every { employmentDomainService.getById(1L) } returns viewerEmployment

            val mockEmployee = mockk<Employment> {
                every { id } returns 2L
                every { personId } returns 20L
                every { companyId } returns 1L
                every { employeeNumber } returns "EMP-002"
                every { employmentType } returns EmploymentType.REGULAR
                every { status } returns EmploymentStatus.ACTIVE
                every { departmentId } returns 5L
            }
            val mockPage = PageImpl(listOf(mockEmployee))
            every { employeeQueryDomainService.search(viewerEmployment, any<SearchCriteria>(), any()) } returns mockPage

            then("EmployeeQueryDomainService.search를 정확히 1회 호출하고 결과 페이지를 반환한다") {
                val result = searchUseCase.execute(command)

                verify(exactly = 1) { employeeQueryDomainService.search(viewerEmployment, any(), any()) }
                result.content.size shouldBe 1
                result.content[0].employmentId shouldBe 2L
            }
        }
    }

    given("GetEmployeeUseCase.execute") {
        `when`("접근 가능한 직원 상세 조회를 요청하면") {
            val command = GetEmployeeCommand(
                viewerEmploymentId = 1L,
                targetEmploymentId = 2L,
            )

            val viewerEmployment = mockk<Employment> {
                every { id } returns 1L
                every { role } returns EmploymentRole.HR_MANAGER
                every { status } returns EmploymentStatus.ACTIVE
            }
            every { employmentDomainService.getById(1L) } returns viewerEmployment

            val targetEmployment = mockk<Employment> {
                every { id } returns 2L
                every { personId } returns 20L
                every { companyId } returns 1L
                every { employeeNumber } returns "EMP-002"
                every { employmentType } returns EmploymentType.REGULAR
                every { status } returns EmploymentStatus.ACTIVE
                every { startDate } returns LocalDate.of(2025, 3, 1)
                every { departmentId } returns 5L
                every { positionId } returns null
                every { baseSalary } returns null
            }
            every { employeeQueryDomainService.getEmployee(viewerEmployment, 2L) } returns targetEmployment

            then("EmployeeQueryDomainService.getEmployee를 정확히 1회 호출하고 결과를 반환한다") {
                val result = getEmployeeUseCase.execute(command)

                verify(exactly = 1) { employeeQueryDomainService.getEmployee(viewerEmployment, 2L) }
                result.employmentId shouldBe 2L
            }
        }

        `when`("TEAM_LEAD viewer가 다른 팀 직원 조회 시") {
            val command = GetEmployeeCommand(
                viewerEmploymentId = 1L,
                targetEmploymentId = 99L,
            )

            val viewerEmployment = mockk<Employment> {
                every { id } returns 1L
                every { role } returns EmploymentRole.TEAM_LEAD
                every { status } returns EmploymentStatus.ACTIVE
            }
            every { employmentDomainService.getById(1L) } returns viewerEmployment
            every {
                employeeQueryDomainService.getEmployee(viewerEmployment, 99L)
            } throws ForbiddenException("ACCESS_DENIED", "해당 직원 정보에 접근할 수 없습니다")

            then("ForbiddenException이 전파된다") {
                shouldThrow<ForbiddenException> { getEmployeeUseCase.execute(command) }
            }
        }
    }

    given("GetEmploymentHistoryUseCase.execute") {
        `when`("접근 가능한 직원의 발령 이력 조회를 요청하면") {
            val command = GetEmploymentHistoryCommand(
                viewerEmploymentId = 1L,
                employmentId = 2L,
            )

            val viewerEmployment = mockk<Employment> {
                every { id } returns 1L
            }
            every { employmentDomainService.getById(1L) } returns viewerEmployment

            val targetEmployment = mockk<Employment> {
                every { id } returns 2L
            }
            every { employeeQueryDomainService.getEmployee(viewerEmployment, 2L) } returns targetEmployment

            val mockHistory = mockk<EmploymentHistory> {
                every { id } returns 10L
                every { employmentId } returns 2L
                every { eventType } returns EmploymentHistoryEventType.HIRE
                every { effectiveDate } returns LocalDate.of(2025, 1, 1)
                every { note } returns null
                every { cancelledAt } returns null
            }
            every { employmentHistoryDomainService.findByEmployment(2L) } returns listOf(mockHistory)

            then("이력 목록을 반환한다") {
                val result = getHistoryUseCase.execute(command)

                verify(exactly = 1) { employmentHistoryDomainService.findByEmployment(2L) }
                result.size shouldBe 1
                result[0].historyId shouldBe 10L
            }
        }
    }
})
