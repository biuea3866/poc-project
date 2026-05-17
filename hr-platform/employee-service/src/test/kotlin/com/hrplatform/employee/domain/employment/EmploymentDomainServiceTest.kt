package com.hrplatform.employee.domain.employment

import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.employee.domain.history.EmploymentHistory
import com.hrplatform.employee.domain.history.EmploymentHistoryEventType
import com.hrplatform.employee.domain.history.EmploymentHistoryRepository
import com.hrplatform.employee.domain.person.PersonDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EmploymentDomainServiceTest : BehaviorSpec({

    val employmentRepository = mockk<EmploymentRepository>()
    val personDomainService = mockk<PersonDomainService>()
    val historyRepository = mockk<EmploymentHistoryRepository>()
    val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)
    val employmentDomainService = EmploymentDomainService(
        employmentRepository = employmentRepository,
        personDomainService = personDomainService,
        historyRepository = historyRepository,
        eventPublisher = eventPublisher,
    )

    val now = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC)

    given("hire") {
        `when`("유효한 HireCommand로 입사 처리를 요청하면") {
            val command = HireCommand(
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

            val savedPerson = mockk<com.hrplatform.employee.domain.person.Person> {
                every { id } returns 10L
            }
            every {
                personDomainService.findOrCreate(
                    personalEmail = command.personalEmail,
                    name = command.name,
                    birthDate = command.birthDate,
                    nationality = command.nationality,
                    gender = command.gender,
                )
            } returns savedPerson

            val employmentSlot = slot<Employment>()
            every { employmentRepository.save(capture(employmentSlot)) } answers {
                val emp = firstArg<Employment>()
                val idField = emp.javaClass.superclass.superclass.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(emp, 1L)
                emp
            }

            val historySlot = slot<EmploymentHistory>()
            every { historyRepository.save(capture(historySlot)) } answers { firstArg() }

            then("Person 1건 + Employment 1건 + EmploymentHistory(HIRE) 1건 + DomainEvent 발행이 이루어진다") {
                val result = employmentDomainService.hire(command, now)

                result shouldNotBe null
                result.personId shouldBe 10L
                result.employeeNumber shouldBe "EMP-001"
                result.status shouldBe EmploymentStatus.ACTIVE

                historySlot.captured.eventType shouldBe EmploymentHistoryEventType.HIRE

                verify(exactly = 1) { personDomainService.findOrCreate(any(), any(), any(), any(), any()) }
                verify(exactly = 1) { employmentRepository.save(any()) }
                verify(exactly = 1) { historyRepository.save(any()) }
                verify(atLeast = 1) { eventPublisher.publishAll(any()) }
            }
        }
    }

    given("suspend") {
        `when`("ACTIVE Employment에 휴직 처리를 요청하면") {
            val employment = buildActiveEmployment(id = 1L)
            every { employmentRepository.findById(1L) } returns employment
            every { employmentRepository.save(any()) } answers { firstArg<Employment>().also { emp ->
                val idField = emp.javaClass.superclass.superclass.getDeclaredField("id")
                idField.isAccessible = true
                if (idField.get(emp) == null) idField.set(emp, 1L)
            }}
            every { historyRepository.save(any()) } answers { firstArg() }

            then("status가 ON_LEAVE로 변경되고 이력과 이벤트가 기록된다") {
                val result = employmentDomainService.suspend(
                    employmentId = 1L,
                    reason = "개인 사정",
                    until = LocalDate.of(2026, 3, 1),
                    actorEmploymentId = 100L,
                    now = now,
                )
                result.status shouldBe EmploymentStatus.ON_LEAVE
                verify(atLeast = 1) { historyRepository.save(any()) }
                verify(atLeast = 1) { eventPublisher.publishAll(any()) }
            }
        }

        `when`("RESIGNED Employment에 휴직 처리를 요청하면") {
            val resignedEmployment = buildResignedEmployment(id = 2L)
            every { employmentRepository.findById(2L) } returns resignedEmployment

            then("InvalidStateTransitionException이 발생한다") {
                shouldThrow<InvalidStateTransitionException> {
                    employmentDomainService.suspend(
                        employmentId = 2L,
                        reason = "이유",
                        until = null,
                        actorEmploymentId = 100L,
                        now = now,
                    )
                }
            }
        }
    }

    given("resume") {
        `when`("ON_LEAVE Employment에 복직 처리를 요청하면") {
            val onLeaveEmployment = buildOnLeaveEmployment(id = 3L)
            every { employmentRepository.findById(3L) } returns onLeaveEmployment
            every { employmentRepository.save(any()) } answers { firstArg<Employment>().also { emp ->
                val idField = emp.javaClass.superclass.superclass.getDeclaredField("id")
                idField.isAccessible = true
                if (idField.get(emp) == null) idField.set(emp, 3L)
            }}
            every { historyRepository.save(any()) } answers { firstArg() }

            then("status가 ACTIVE로 변경된다") {
                val result = employmentDomainService.resume(
                    employmentId = 3L,
                    actorEmploymentId = 100L,
                    now = now,
                )
                result.status shouldBe EmploymentStatus.ACTIVE
                verify(atLeast = 1) { eventPublisher.publishAll(any()) }
            }
        }
    }

    given("resign") {
        `when`("ACTIVE Employment에 퇴사 처리를 요청하면") {
            val employment = buildActiveEmployment(id = 4L)
            every { employmentRepository.findById(4L) } returns employment
            every { employmentRepository.save(any()) } answers { firstArg<Employment>().also { emp ->
                val idField = emp.javaClass.superclass.superclass.getDeclaredField("id")
                idField.isAccessible = true
                if (idField.get(emp) == null) idField.set(emp, 4L)
            }}
            every { historyRepository.save(any()) } answers { firstArg() }

            then("status가 RESIGNED로 변경된다") {
                val result = employmentDomainService.resign(
                    employmentId = 4L,
                    reason = "자발적 퇴사",
                    actorEmploymentId = 100L,
                    now = now,
                )
                result.status shouldBe EmploymentStatus.RESIGNED
                verify(atLeast = 1) { eventPublisher.publishAll(any()) }
            }
        }
    }

    given("cancelEvent") {
        `when`("마지막 이력이 RESIGN인 경우 취소를 시도하면") {
            val employment = buildResignedEmployment(id = 5L)
            every { employmentRepository.findById(5L) } returns employment
            val lastHistory = mockk<EmploymentHistory> {
                every { id } returns 50L
                every { eventType } returns EmploymentHistoryEventType.RESIGN
                every { cancelledAt } returns null
                every { oldValue } returns null
                every { newValue } returns mapOf("status" to "RESIGNED")
            }
            every { historyRepository.findLastByEmploymentId(5L) } returns lastHistory

            then("IneligibleCancellationException이 발생한다") {
                shouldThrow<IneligibleCancellationException> {
                    employmentDomainService.cancelEvent(
                        employmentId = 5L,
                        historyId = 50L,
                        cancellationReason = "오기재",
                        actorEmploymentId = 100L,
                        now = now,
                    )
                }
            }
        }

        `when`("요청한 historyId가 마지막 이력이 아니면") {
            val employment = buildActiveEmployment(id = 6L)
            every { employmentRepository.findById(6L) } returns employment
            val lastHistory = mockk<EmploymentHistory> {
                every { id } returns 60L
                every { eventType } returns EmploymentHistoryEventType.DEPT_CHANGE
                every { cancelledAt } returns null
            }
            every { historyRepository.findLastByEmploymentId(6L) } returns lastHistory

            then("IneligibleCancellationException이 발생한다") {
                shouldThrow<IneligibleCancellationException> {
                    employmentDomainService.cancelEvent(
                        employmentId = 6L,
                        historyId = 999L,
                        cancellationReason = "오기재",
                        actorEmploymentId = 100L,
                        now = now,
                    )
                }
            }
        }
    }
})

private fun buildActiveEmployment(id: Long): Employment {
    val employment = Employment(
        personId = 10L,
        companyId = 1L,
        employeeNumber = "EMP-00$id",
        employmentType = EmploymentType.REGULAR,
        status = EmploymentStatus.PRE_HIRED,
        startDate = LocalDate.of(2025, 1, 1),
        country = "KR",
        currency = "KRW",
        timezone = "Asia/Seoul",
    )
    employment.activate(ZonedDateTime.now(ZoneOffset.UTC), null)
    employment.pullDomainEvents()

    val idField = employment.javaClass.superclass.superclass.getDeclaredField("id")
    idField.isAccessible = true
    idField.set(employment, id)

    return employment
}

private fun buildOnLeaveEmployment(id: Long): Employment {
    val employment = buildActiveEmployment(id)
    employment.suspend("휴직 사유", null, ZonedDateTime.now(ZoneOffset.UTC), null)
    employment.pullDomainEvents()
    return employment
}

private fun buildResignedEmployment(id: Long): Employment {
    val employment = buildActiveEmployment(id)
    employment.resign(ZonedDateTime.now(ZoneOffset.UTC), null, null)
    employment.pullDomainEvents()
    return employment
}
