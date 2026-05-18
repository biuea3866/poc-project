package com.hrplatform.employee.application.department

import com.hrplatform.employee.domain.department.CircularDepartmentException
import com.hrplatform.employee.domain.department.Department
import com.hrplatform.employee.domain.department.DepartmentDomainService
import com.hrplatform.employee.domain.department.IneligibleHeadException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class DepartmentUseCaseTest : BehaviorSpec({

    val departmentDomainService = mockk<DepartmentDomainService>()
    val createUseCase = CreateDepartmentUseCase(departmentDomainService)
    val moveUseCase = MoveDepartmentUseCase(departmentDomainService)
    val assignHeadUseCase = AssignDepartmentHeadUseCase(departmentDomainService)

    given("CreateDepartmentUseCase.execute") {
        val command = CreateDepartmentUseCaseCommand(
            companyId = 1L,
            name = "개발팀",
            code = "DEV",
            parentId = null,
            orderNo = 1,
            effectiveFrom = LocalDate.of(2026, 1, 1),
            actorEmploymentId = 100L,
        )

        `when`("정상적인 CreateDepartmentUseCaseCommand가 주어지면") {
            val mockDepartment = mockk<Department> {
                every { id } returns 1L
                every { companyId } returns 1L
                every { name } returns "개발팀"
                every { code } returns "DEV"
                every { parentId } returns null
                every { path } returns "/1/"
                every { headEmploymentId } returns null
                every { effectiveFrom } returns LocalDate.of(2026, 1, 1)
            }
            every { departmentDomainService.create(any()) } returns mockDepartment

            then("DomainService.create를 정확히 1회 호출하고 결과를 반환한다") {
                val result = createUseCase.execute(command)

                verify(exactly = 1) { departmentDomainService.create(any()) }
                result.departmentId shouldBe 1L
                result.name shouldBe "개발팀"
            }
        }
    }

    given("MoveDepartmentUseCase.execute") {
        `when`("정상적인 MoveDepartmentCommand가 주어지면") {
            val command = MoveDepartmentCommand(
                departmentId = 5L,
                newParentId = 2L,
                actorEmploymentId = 100L,
            )

            val mockDepartment = mockk<Department> {
                every { id } returns 5L
                every { companyId } returns 1L
                every { name } returns "A팀"
                every { code } returns "TEAM-A"
                every { parentId } returns 2L
                every { path } returns "/1/2/5/"
                every { headEmploymentId } returns null
                every { effectiveFrom } returns LocalDate.of(2026, 1, 1)
            }
            every { departmentDomainService.moveTo(5L, 2L, 100L, any()) } returns mockDepartment

            then("DomainService.moveTo를 정확히 1회 호출하고 결과를 반환한다") {
                val result = moveUseCase.execute(command)

                verify(exactly = 1) { departmentDomainService.moveTo(5L, 2L, 100L, any()) }
                result.departmentId shouldBe 5L
                result.parentId shouldBe 2L
            }
        }

        `when`("자기 자신을 부모로 지정하면") {
            val command = MoveDepartmentCommand(
                departmentId = 5L,
                newParentId = 5L,
                actorEmploymentId = 100L,
            )
            every {
                departmentDomainService.moveTo(5L, 5L, 100L, any())
            } throws CircularDepartmentException()

            then("CircularDepartmentException이 전파된다") {
                shouldThrow<CircularDepartmentException> { moveUseCase.execute(command) }
            }
        }
    }

    given("AssignDepartmentHeadUseCase.execute") {
        `when`("정상적인 AssignDepartmentHeadCommand가 주어지면") {
            val command = AssignDepartmentHeadCommand(
                departmentId = 1L,
                employmentId = 2L,
                actorEmploymentId = 100L,
            )

            val mockDepartment = mockk<Department> {
                every { id } returns 1L
                every { companyId } returns 1L
                every { name } returns "개발팀"
                every { code } returns "DEV"
                every { parentId } returns null
                every { path } returns "/1/"
                every { headEmploymentId } returns 2L
                every { effectiveFrom } returns LocalDate.of(2026, 1, 1)
            }
            every { departmentDomainService.assignHead(1L, 2L, 100L, any()) } returns mockDepartment

            then("DomainService.assignHead를 정확히 1회 호출하고 headEmploymentId가 반영된 결과를 반환한다") {
                val result = assignHeadUseCase.execute(command)

                verify(exactly = 1) { departmentDomainService.assignHead(1L, 2L, 100L, any()) }
                result.headEmploymentId shouldBe 2L
            }
        }

        `when`("ON_LEAVE Employment를 부서장으로 지정하면") {
            val command = AssignDepartmentHeadCommand(
                departmentId = 1L,
                employmentId = 99L,
                actorEmploymentId = 100L,
            )
            every {
                departmentDomainService.assignHead(1L, 99L, 100L, any())
            } throws IneligibleHeadException()

            then("IneligibleHeadException이 전파된다") {
                shouldThrow<IneligibleHeadException> { assignHeadUseCase.execute(command) }
            }
        }
    }
})
