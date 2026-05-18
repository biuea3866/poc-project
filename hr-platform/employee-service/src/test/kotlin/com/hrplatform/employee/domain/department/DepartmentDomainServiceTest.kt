package com.hrplatform.employee.domain.department

import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentNotFoundException
import com.hrplatform.employee.domain.employment.EmploymentRepository
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.EmploymentType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class DepartmentDomainServiceTest : BehaviorSpec({

    fun makeDepartment(
        id: Long,
        parentId: Long? = null,
        path: String = "/$id/",
        headEmploymentId: Long? = null,
    ): Department {
        val dept = Department(
            companyId = 100L,
            name = "부서$id",
            code = "CODE$id",
            parentId = parentId,
            path = path,
            headEmploymentId = headEmploymentId,
            orderNo = 0,
            effectiveFrom = LocalDate.now(),
            effectiveTo = null,
        )
        val field = dept.javaClass.superclass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(dept, id)
        return dept
    }

    fun makeEmployment(id: Long, status: EmploymentStatus): Employment {
        val employment = Employment(
            personId = 1L,
            companyId = 100L,
            employeeNumber = "EMP$id",
            employmentType = EmploymentType.REGULAR,
            status = status,
            startDate = LocalDate.now(),
            country = "KR",
            currency = "KRW",
            timezone = "Asia/Seoul",
        )
        val field = employment.javaClass.superclass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(employment, id)
        return employment
    }

    fun mocks() = Triple(
        mockk<DepartmentRepository>(relaxed = true),
        mockk<EmploymentRepository>(relaxed = true),
        mockk<DomainEventPublisher>(relaxed = true),
    )

    given("DepartmentDomainService.create") {
        `when`("유효한 CreateDepartmentCommand로 호출하면 (부모 없음)") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val command = CreateDepartmentCommand(
                companyId = 100L,
                name = "개발팀",
                code = "DEV",
                parentId = null,
                orderNo = 1,
                effectiveFrom = LocalDate.now(),
                actorEmploymentId = null,
            )
            val savedDept = makeDepartment(id = 1L, path = "/1/")
            every { departmentRepository.save(any()) } returnsMany listOf(savedDept, savedDept)

            val result = service.create(command)

            then("저장된 Department가 반환된다") {
                result shouldBe savedDept
            }
        }

        `when`("부모 부서가 있는 경우 부모를 조회한다") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val parentDept = makeDepartment(id = 2L, path = "/2/")
            val command = CreateDepartmentCommand(
                companyId = 100L,
                name = "개발1팀",
                code = "DEV1",
                parentId = 2L,
                orderNo = 1,
                effectiveFrom = LocalDate.now(),
                actorEmploymentId = null,
            )
            val savedDept = makeDepartment(id = 10L, parentId = 2L, path = "/2/10/")
            every { departmentRepository.findById(2L) } returns parentDept
            every { departmentRepository.save(any()) } returnsMany listOf(savedDept, savedDept)

            val result = service.create(command)

            then("저장된 Department가 반환된다") {
                result shouldBe savedDept
            }
        }
    }

    given("DepartmentDomainService.moveTo") {
        `when`("부서를 새 부모로 이동하면 path가 재계산되고 자식 path가 일괄 갱신된다") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val targetDept = makeDepartment(id = 1L, path = "/1/")
            val newParentDept = makeDepartment(id = 5L, path = "/5/")
            val childDept = makeDepartment(id = 2L, parentId = 1L, path = "/1/2/")

            every { departmentRepository.findById(1L) } returns targetDept
            every { departmentRepository.findById(5L) } returns newParentDept
            every { departmentRepository.findByPathPrefix("/1/") } returns listOf(targetDept, childDept)
            every { departmentRepository.save(any()) } answers { firstArg() }

            val now = ZonedDateTime.now(ZoneOffset.UTC)
            service.moveTo(departmentId = 1L, newParentId = 5L, actorEmploymentId = 10L, now = now)

            then("대상 부서의 path가 갱신된다") {
                targetDept.path shouldBe "/5/1/"
            }
            then("자식 부서의 path가 일괄 갱신된다") {
                childDept.path shouldBe "/5/1/2/"
            }
            then("DepartmentChangedEvent가 발행된다") {
                verify(exactly = 1) { eventPublisher.publishAll(any()) }
            }
        }

        `when`("존재하지 않는 departmentId로 이동하면 DepartmentNotFoundException 발생") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            every { departmentRepository.findById(999L) } returns null
            val now = ZonedDateTime.now(ZoneOffset.UTC)

            then("DepartmentNotFoundException이 발생한다") {
                shouldThrow<DepartmentNotFoundException> {
                    service.moveTo(departmentId = 999L, newParentId = null, actorEmploymentId = null, now = now)
                }
            }
        }

        `when`("존재하지 않는 newParentId로 이동하면 DepartmentNotFoundException 발생") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val targetDept = makeDepartment(id = 1L, path = "/1/")
            every { departmentRepository.findById(1L) } returns targetDept
            every { departmentRepository.findById(999L) } returns null
            val now = ZonedDateTime.now(ZoneOffset.UTC)

            then("DepartmentNotFoundException이 발생한다") {
                shouldThrow<DepartmentNotFoundException> {
                    service.moveTo(departmentId = 1L, newParentId = 999L, actorEmploymentId = null, now = now)
                }
            }
        }
    }

    given("DepartmentDomainService.assignHead") {
        `when`("ACTIVE 직원을 부서장으로 지정하면 성공한다") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val dept = makeDepartment(id = 1L)
            val activeEmployment = makeEmployment(id = 50L, status = EmploymentStatus.ACTIVE)
            every { departmentRepository.findById(1L) } returns dept
            every { employmentRepository.findById(50L) } returns activeEmployment
            every { departmentRepository.save(any()) } answers { firstArg() }

            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val result = service.assignHead(departmentId = 1L, employmentId = 50L, actorEmploymentId = 10L, now = now)

            then("부서장이 지정된다") {
                result.headEmploymentId shouldBe 50L
            }
            then("DepartmentHeadChangedEvent가 발행된다") {
                verify(exactly = 1) { eventPublisher.publishAll(any()) }
            }
        }

        `when`("ON_LEAVE 직원을 부서장으로 지정하면 IneligibleHeadException 발생") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val dept = makeDepartment(id = 1L)
            val onLeaveEmployment = makeEmployment(id = 60L, status = EmploymentStatus.ON_LEAVE)
            every { departmentRepository.findById(1L) } returns dept
            every { employmentRepository.findById(60L) } returns onLeaveEmployment
            val now = ZonedDateTime.now(ZoneOffset.UTC)

            then("IneligibleHeadException이 발생한다") {
                shouldThrow<IneligibleHeadException> {
                    service.assignHead(departmentId = 1L, employmentId = 60L, actorEmploymentId = 10L, now = now)
                }
            }
        }

        `when`("RESIGNED 직원을 부서장으로 지정하면 IneligibleHeadException 발생") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val dept = makeDepartment(id = 1L)
            val resignedEmployment = makeEmployment(id = 70L, status = EmploymentStatus.RESIGNED)
            every { departmentRepository.findById(1L) } returns dept
            every { employmentRepository.findById(70L) } returns resignedEmployment
            val now = ZonedDateTime.now(ZoneOffset.UTC)

            then("IneligibleHeadException이 발생한다") {
                shouldThrow<IneligibleHeadException> {
                    service.assignHead(departmentId = 1L, employmentId = 70L, actorEmploymentId = 10L, now = now)
                }
            }
        }

        `when`("존재하지 않는 부서에 부서장 지정하면 DepartmentNotFoundException 발생") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            every { departmentRepository.findById(999L) } returns null
            val now = ZonedDateTime.now(ZoneOffset.UTC)

            then("DepartmentNotFoundException이 발생한다") {
                shouldThrow<DepartmentNotFoundException> {
                    service.assignHead(departmentId = 999L, employmentId = 50L, actorEmploymentId = null, now = now)
                }
            }
        }

        `when`("존재하지 않는 직원을 부서장으로 지정하면 EmploymentNotFoundException 발생") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val dept = makeDepartment(id = 1L)
            every { departmentRepository.findById(1L) } returns dept
            every { employmentRepository.findById(999L) } returns null
            val now = ZonedDateTime.now(ZoneOffset.UTC)

            then("EmploymentNotFoundException이 발생한다") {
                shouldThrow<EmploymentNotFoundException> {
                    service.assignHead(departmentId = 1L, employmentId = 999L, actorEmploymentId = null, now = now)
                }
            }
        }
    }

    given("DepartmentDomainService.removeHead") {
        `when`("부서장을 제거하면 headEmploymentId가 null이 된다") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val dept = makeDepartment(id = 1L, headEmploymentId = 50L)
            every { departmentRepository.findById(1L) } returns dept
            every { departmentRepository.save(any()) } answers { firstArg() }
            val now = ZonedDateTime.now(ZoneOffset.UTC)

            val result = service.removeHead(departmentId = 1L, actorEmploymentId = 10L, now = now)

            then("headEmploymentId가 null이 된다") {
                result.headEmploymentId shouldBe null
            }
            then("DepartmentHeadChangedEvent가 발행된다") {
                verify(exactly = 1) { eventPublisher.publishAll(any()) }
            }
        }

        `when`("부서장이 없는 부서에 removeHead를 호출하면 멱등하게 처리된다") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            val dept = makeDepartment(id = 1L, headEmploymentId = null)
            every { departmentRepository.findById(1L) } returns dept
            every { departmentRepository.save(any()) } answers { firstArg() }
            val now = ZonedDateTime.now(ZoneOffset.UTC)

            val result = service.removeHead(departmentId = 1L, actorEmploymentId = null, now = now)

            then("이벤트가 발행되지 않는다") {
                verify(exactly = 0) { eventPublisher.publishAll(any()) }
            }
            then("headEmploymentId는 여전히 null이다") {
                result.headEmploymentId shouldBe null
            }
        }

        `when`("존재하지 않는 부서에 removeHead를 호출하면 DepartmentNotFoundException 발생") {
            val (departmentRepository, employmentRepository, eventPublisher) = mocks()
            val service = DepartmentDomainService(departmentRepository, employmentRepository, eventPublisher)
            every { departmentRepository.findById(999L) } returns null
            val now = ZonedDateTime.now(ZoneOffset.UTC)

            then("DepartmentNotFoundException이 발생한다") {
                shouldThrow<DepartmentNotFoundException> {
                    service.removeHead(departmentId = 999L, actorEmploymentId = null, now = now)
                }
            }
        }
    }
})
