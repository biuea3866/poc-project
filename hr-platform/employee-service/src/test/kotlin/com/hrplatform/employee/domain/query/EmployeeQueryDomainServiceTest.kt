package com.hrplatform.employee.domain.query

import com.hrplatform.core.exception.ForbiddenException
import com.hrplatform.employee.domain.department.DepartmentRepository
import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentNotFoundException
import com.hrplatform.employee.domain.employment.EmploymentRepository
import com.hrplatform.employee.domain.employment.EmploymentRole
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.EmploymentType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EmployeeQueryDomainServiceTest : BehaviorSpec({

    val employmentRepository = mockk<EmploymentRepository>()
    val departmentRepository = mockk<DepartmentRepository>()
    val employeeQueryDomainService = EmployeeQueryDomainService(
        employmentRepository = employmentRepository,
        departmentRepository = departmentRepository,
    )

    val now = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC)

    given("search") {
        `when`("viewer가 ACTIVE가 아니면") {
            val inactiveViewer = buildEmployment(id = 1L, status = EmploymentStatus.ON_LEAVE, departmentId = null)

            then("ForbiddenException이 발생한다") {
                shouldThrow<ForbiddenException> {
                    employeeQueryDomainService.search(
                        viewer = inactiveViewer,
                        criteria = SearchCriteria(companyId = 1L, keyword = null, departmentId = null),
                        page = PageRequest.of(0, 20),
                    )
                }
            }
        }

        `when`("viewer가 HR_MANAGER이면") {
            val hrManagerViewer = buildEmployment(
                id = 2L,
                status = EmploymentStatus.ACTIVE,
                role = EmploymentRole.HR_MANAGER,
                departmentId = 10L,
            )
            val pageResult = PageImpl(listOf(hrManagerViewer))
            every {
                employmentRepository.findByCompanyIdWithPage(1L, null, null, PageRequest.of(0, 20))
            } returns pageResult

            then("companyId 전체 범위로 조회한다") {
                val result = employeeQueryDomainService.search(
                    viewer = hrManagerViewer,
                    criteria = SearchCriteria(companyId = 1L, keyword = null, departmentId = null),
                    page = PageRequest.of(0, 20),
                )
                result.totalElements shouldBe 1
            }
        }

        `when`("viewer가 TEAM_LEAD이면") {
            val teamLeadViewer = buildEmployment(
                id = 3L,
                status = EmploymentStatus.ACTIVE,
                role = EmploymentRole.TEAM_LEAD,
                departmentId = 20L,
            )
            val department = mockk<com.hrplatform.employee.domain.department.Department> {
                every { path } returns "/1/20/"
            }
            every { departmentRepository.findById(20L) } returns department

            val subordinateEmployments = listOf(teamLeadViewer)
            val pageResult = PageImpl(subordinateEmployments)
            every {
                employmentRepository.findByCompanyIdWithPage(1L, "/1/20/", null, PageRequest.of(0, 20))
            } returns pageResult

            then("viewer 부서 path 하위 직원만 조회한다") {
                val result = employeeQueryDomainService.search(
                    viewer = teamLeadViewer,
                    criteria = SearchCriteria(companyId = 1L, keyword = null, departmentId = null),
                    page = PageRequest.of(0, 20),
                )
                result.totalElements shouldBe 1
            }
        }

        `when`("viewer가 EMPLOYEE이면") {
            val employeeViewer = buildEmployment(
                id = 4L,
                status = EmploymentStatus.ACTIVE,
                role = EmploymentRole.EMPLOYEE,
                departmentId = 30L,
            )
            val pageResult = PageImpl(listOf(employeeViewer))
            every {
                employmentRepository.findByCompanyIdWithPage(1L, null, 4L, PageRequest.of(0, 20))
            } returns pageResult

            then("자기 자신만 조회한다") {
                val result = employeeQueryDomainService.search(
                    viewer = employeeViewer,
                    criteria = SearchCriteria(companyId = 1L, keyword = null, departmentId = null),
                    page = PageRequest.of(0, 20),
                )
                result.totalElements shouldBe 1
            }
        }
    }

    given("getEmployee") {
        `when`("존재하지 않는 employmentId를 조회하면") {
            every { employmentRepository.findById(999L) } returns null

            then("EmploymentNotFoundException이 발생한다") {
                shouldThrow<EmploymentNotFoundException> {
                    employeeQueryDomainService.getEmployee(
                        viewer = buildEmployment(id = 1L, status = EmploymentStatus.ACTIVE, departmentId = null),
                        employmentId = 999L,
                    )
                }
            }
        }

        `when`("다른 회사의 Employment를 조회하면") {
            val viewer = buildEmployment(id = 1L, status = EmploymentStatus.ACTIVE, companyId = 1L, departmentId = null)
            val target = buildEmployment(id = 2L, status = EmploymentStatus.ACTIVE, companyId = 2L, departmentId = null)
            every { employmentRepository.findById(2L) } returns target

            then("ForbiddenException이 발생한다") {
                shouldThrow<ForbiddenException> {
                    employeeQueryDomainService.getEmployee(
                        viewer = viewer,
                        employmentId = 2L,
                    )
                }
            }
        }

        `when`("같은 회사의 Employment를 조회하면") {
            val viewer = buildEmployment(id = 1L, status = EmploymentStatus.ACTIVE, companyId = 1L, departmentId = null)
            val target = buildEmployment(id = 2L, status = EmploymentStatus.ACTIVE, companyId = 1L, departmentId = null)
            every { employmentRepository.findById(2L) } returns target

            then("Employment를 반환한다") {
                val result = employeeQueryDomainService.getEmployee(
                    viewer = viewer,
                    employmentId = 2L,
                )
                result.id shouldBe 2L
            }
        }
    }
})

private fun buildEmployment(
    id: Long,
    status: EmploymentStatus,
    role: EmploymentRole = EmploymentRole.EMPLOYEE,
    companyId: Long = 1L,
    departmentId: Long?,
): Employment {
    val employment = Employment(
        personId = 10L,
        companyId = companyId,
        employeeNumber = "EMP-00$id",
        employmentType = EmploymentType.REGULAR,
        status = status,
        startDate = LocalDate.of(2025, 1, 1),
        country = "KR",
        currency = "KRW",
        timezone = "Asia/Seoul",
        departmentId = departmentId,
        role = role,
    )
    val idField = employment.javaClass.superclass.superclass.getDeclaredField("id")
    idField.isAccessible = true
    idField.set(employment, id)
    return employment
}
