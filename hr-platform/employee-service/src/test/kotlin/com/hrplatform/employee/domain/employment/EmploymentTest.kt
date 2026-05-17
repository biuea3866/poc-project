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
import com.hrplatform.employee.domain.employment.exception.CrossCompanyAccessException
import com.hrplatform.employee.domain.employment.exception.IneligibleCancellationException
import com.hrplatform.employee.domain.employment.exception.InvalidStateTransitionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.ZonedDateTime

class EmploymentTest : BehaviorSpec({

    val now: ZonedDateTime = ZonedDateTime.now()

    fun makeSpec(
        companyId: Long = 10L,
        personId: Long = 100L,
        managerEmploymentId: Long? = null,
    ) = EmploymentSpec(
        personId = personId,
        companyId = companyId,
        employeeNumber = "EMP-001",
        employmentType = EmploymentType.REGULAR,
        startDate = LocalDate.now(),
        country = "KR",
        currency = "KRW",
        timezone = "Asia/Seoul",
        managerEmploymentId = managerEmploymentId,
    )

    fun createPreHiredEmployment(
        id: Long = 1L,
        companyId: Long = 10L,
        departmentId: Long? = null,
        positionId: Long? = null,
        baseSalary: Long? = null,
    ) = Employment(
        id = id,
        spec = makeSpec(companyId = companyId),
        status = EmploymentStatus.PRE_HIRED,
        departmentId = departmentId,
        positionId = positionId,
        baseSalary = baseSalary,
        createdAt = now,
        updatedAt = now,
    )

    fun createActiveEmployment(
        id: Long = 1L,
        companyId: Long = 10L,
        departmentId: Long? = 5L,
        positionId: Long? = 3L,
        baseSalary: Long? = 3_000_000L,
        managerEmploymentId: Long? = null,
    ) = Employment(
        id = id,
        spec = makeSpec(companyId = companyId, managerEmploymentId = managerEmploymentId),
        status = EmploymentStatus.ACTIVE,
        departmentId = departmentId,
        positionId = positionId,
        baseSalary = baseSalary,
        createdAt = now,
        updatedAt = now,
    )

    fun createOnLeaveEmployment(
        id: Long = 1L,
        companyId: Long = 10L,
        departmentId: Long? = 5L,
    ) = Employment(
        id = id,
        spec = makeSpec(companyId = companyId),
        status = EmploymentStatus.ON_LEAVE,
        departmentId = departmentId,
        createdAt = now,
        updatedAt = now,
    )

    fun createResignedEmployment(
        id: Long = 1L,
        companyId: Long = 10L,
    ) = Employment(
        id = id,
        spec = makeSpec(companyId = companyId),
        status = EmploymentStatus.RESIGNED,
        createdAt = now,
        updatedAt = now,
    )

    // ─────────────────────────────────────────────────────────────────
    // activate
    // ─────────────────────────────────────────────────────────────────
    given("PRE_HIRED 상태의 Employment에 activate를 호출하면") {
        val employment = createPreHiredEmployment()

        employment.activate(now)

        `when`("status 검증") {
            then("status가 ACTIVE로 변경된다") {
                employment.status shouldBe EmploymentStatus.ACTIVE
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeHiredEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0] as? EmployeeHiredEvent shouldNotBe null
            }
        }
    }

    given("ACTIVE 상태의 Employment에 activate를 호출하면") {
        val employment = createActiveEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.activate(now)
            }
        }
    }

    given("RESIGNED 상태의 Employment에 activate를 호출하면") {
        val employment = createResignedEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.activate(now)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // suspend
    // ─────────────────────────────────────────────────────────────────
    given("ACTIVE 상태의 Employment에 suspend를 호출하면") {
        val employment = createActiveEmployment()
        val until = LocalDate.of(2026, 8, 31)

        employment.suspend(reason = "건강 문제", until = until, now = now)

        `when`("status 검증") {
            then("status가 ON_LEAVE로 변경된다") {
                employment.status shouldBe EmploymentStatus.ON_LEAVE
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeSuspendedEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                val suspendedEvent = events[0] as EmployeeSuspendedEvent
                suspendedEvent.reason shouldBe "건강 문제"
                suspendedEvent.until shouldBe until
            }
        }
    }

    given("ON_LEAVE 상태의 Employment에 suspend를 호출하면") {
        val employment = createOnLeaveEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.suspend(reason = "사유", until = null, now = now)
            }
        }
    }

    given("RESIGNED 상태의 Employment에 suspend를 호출하면") {
        val employment = createResignedEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.suspend(reason = "사유", until = null, now = now)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // resume
    // ─────────────────────────────────────────────────────────────────
    given("ON_LEAVE 상태의 Employment에 resume을 호출하면") {
        val employment = createOnLeaveEmployment()

        employment.resume(now)

        `when`("status 검증") {
            then("status가 ACTIVE로 변경된다") {
                employment.status shouldBe EmploymentStatus.ACTIVE
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeResumedEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0] as? EmployeeResumedEvent shouldNotBe null
            }
        }
    }

    given("ACTIVE 상태의 Employment에 resume을 호출하면") {
        val employment = createActiveEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.resume(now)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // resign
    // ─────────────────────────────────────────────────────────────────
    given("ACTIVE 상태의 Employment에 resign을 호출하면") {
        val employment = createActiveEmployment()

        employment.resign(now = now, reason = "개인 사유")

        `when`("status 검증") {
            then("status가 RESIGNED로 변경된다") {
                employment.status shouldBe EmploymentStatus.RESIGNED
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeResignedEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                val resignedEvent = events[0] as EmployeeResignedEvent
                resignedEvent.reason shouldBe "개인 사유"
            }
        }
    }

    given("RESIGNED 상태의 Employment에 resign을 호출하면") {
        val employment = createResignedEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.resign(now = now, reason = null)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // resignDuringLeave
    // ─────────────────────────────────────────────────────────────────
    given("ON_LEAVE 상태의 Employment에 resignDuringLeave를 호출하면") {
        val employment = createOnLeaveEmployment()

        employment.resignDuringLeave(now = now, reason = "개인 사유")

        `when`("status 검증") {
            then("status가 RESIGNED로 변경된다") {
                employment.status shouldBe EmploymentStatus.RESIGNED
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeResignedEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0] as? EmployeeResignedEvent shouldNotBe null
            }
        }
    }

    given("ACTIVE 상태의 Employment에 resignDuringLeave를 호출하면") {
        val employment = createActiveEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.resignDuringLeave(now = now, reason = null)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // transferTo
    // ─────────────────────────────────────────────────────────────────
    given("ACTIVE 상태의 Employment에 transferTo를 호출하면") {
        val employment = createActiveEmployment(departmentId = 5L)

        employment.transferTo(newDepartmentId = 10L, now = now)

        `when`("departmentId 검증") {
            then("departmentId가 변경된다") {
                employment.departmentId shouldBe 10L
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeTransferredEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                val transferredEvent = events[0] as EmployeeTransferredEvent
                transferredEvent.previousDepartmentId shouldBe 5L
                transferredEvent.newDepartmentId shouldBe 10L
            }
        }
    }

    given("RESIGNED 상태의 Employment에 transferTo를 호출하면") {
        val employment = createResignedEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.transferTo(newDepartmentId = 10L, now = now)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // promote
    // ─────────────────────────────────────────────────────────────────
    given("ACTIVE 상태의 Employment에 promote를 호출하면") {
        val employment = createActiveEmployment(positionId = 3L)

        employment.promote(newPositionId = 4L, now = now)

        `when`("positionId 검증") {
            then("positionId가 변경된다") {
                employment.positionId shouldBe 4L
            }
        }

        `when`("이벤트 검증") {
            then("EmployeePromotedEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                val promotedEvent = events[0] as EmployeePromotedEvent
                promotedEvent.previousPositionId shouldBe 3L
                promotedEvent.newPositionId shouldBe 4L
            }
        }
    }

    given("RESIGNED 상태의 Employment에 promote를 호출하면") {
        val employment = createResignedEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.promote(newPositionId = 4L, now = now)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // changeCompensation
    // ─────────────────────────────────────────────────────────────────
    given("ACTIVE 상태의 Employment에 changeCompensation을 호출하면") {
        val employment = createActiveEmployment(baseSalary = 3_000_000L)

        employment.changeCompensation(newBaseSalary = 3_500_000L, newCurrency = "KRW", now = now)

        `when`("baseSalary 검증") {
            then("baseSalary가 변경된다") {
                employment.baseSalary shouldBe 3_500_000L
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeSalaryChangedEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                val salaryChangedEvent = events[0] as EmployeeSalaryChangedEvent
                salaryChangedEvent.previousBaseSalary shouldBe 3_000_000L
                salaryChangedEvent.newBaseSalary shouldBe 3_500_000L
                salaryChangedEvent.currency shouldBe "KRW"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 발령 취소 — cancelLastTransfer
    // ─────────────────────────────────────────────────────────────────
    given("마지막 발령이 transferTo인 Employment에 cancelLastTransfer를 호출하면") {
        val employment = createActiveEmployment(departmentId = 5L)
        employment.transferTo(newDepartmentId = 10L, now = now)
        employment.pullDomainEvents()

        employment.cancelLastTransfer(now = now)

        `when`("departmentId 검증") {
            then("departmentId가 이전 값으로 복원된다") {
                employment.departmentId shouldBe 5L
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeTransferredCancelledEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                val cancelledEvent = events[0] as EmployeeTransferredCancelledEvent
                cancelledEvent.cancelledDepartmentId shouldBe 10L
                cancelledEvent.restoredDepartmentId shouldBe 5L
            }
        }
    }

    given("마지막 발령이 transferTo가 아닌 Employment에 cancelLastTransfer를 호출하면") {
        val employment = createActiveEmployment(positionId = 3L)
        employment.promote(newPositionId = 4L, now = now)
        employment.pullDomainEvents()

        then("IneligibleCancellationException이 발생한다") {
            shouldThrow<IneligibleCancellationException> {
                employment.cancelLastTransfer(now = now)
            }
        }
    }

    given("아무 발령도 없는 Employment에 cancelLastTransfer를 호출하면") {
        val employment = createActiveEmployment()

        then("IneligibleCancellationException이 발생한다") {
            shouldThrow<IneligibleCancellationException> {
                employment.cancelLastTransfer(now = now)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 발령 취소 — cancelLastPromotion
    // ─────────────────────────────────────────────────────────────────
    given("마지막 발령이 promote인 Employment에 cancelLastPromotion을 호출하면") {
        val employment = createActiveEmployment(positionId = 3L)
        employment.promote(newPositionId = 4L, now = now)
        employment.pullDomainEvents()

        employment.cancelLastPromotion(now = now)

        `when`("positionId 검증") {
            then("positionId가 이전 값으로 복원된다") {
                employment.positionId shouldBe 3L
            }
        }

        `when`("이벤트 검증") {
            then("EmployeePromotedCancelledEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                val cancelledEvent = events[0] as EmployeePromotedCancelledEvent
                cancelledEvent.cancelledPositionId shouldBe 4L
                cancelledEvent.restoredPositionId shouldBe 3L
            }
        }
    }

    given("마지막 발령이 promote가 아닌 Employment에 cancelLastPromotion을 호출하면") {
        val employment = createActiveEmployment(departmentId = 5L)
        employment.transferTo(newDepartmentId = 10L, now = now)
        employment.pullDomainEvents()

        then("IneligibleCancellationException이 발생한다") {
            shouldThrow<IneligibleCancellationException> {
                employment.cancelLastPromotion(now = now)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 발령 취소 — cancelLastSalaryChange
    // ─────────────────────────────────────────────────────────────────
    given("마지막 발령이 changeCompensation인 Employment에 cancelLastSalaryChange를 호출하면") {
        val employment = createActiveEmployment(baseSalary = 3_000_000L)
        employment.changeCompensation(newBaseSalary = 3_500_000L, newCurrency = "KRW", now = now)
        employment.pullDomainEvents()

        employment.cancelLastSalaryChange(now = now)

        `when`("baseSalary 검증") {
            then("baseSalary가 이전 값으로 복원된다") {
                employment.baseSalary shouldBe 3_000_000L
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeSalaryChangedCancelledEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                val cancelledEvent = events[0] as EmployeeSalaryChangedCancelledEvent
                cancelledEvent.cancelledBaseSalary shouldBe 3_500_000L
                cancelledEvent.restoredBaseSalary shouldBe 3_000_000L
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 발령 취소 — cancelLastSuspend
    // ─────────────────────────────────────────────────────────────────
    given("마지막 발령이 suspend인 ON_LEAVE Employment에 cancelLastSuspend를 호출하면") {
        val employment = createActiveEmployment()
        employment.suspend(reason = "건강 문제", until = null, now = now)
        employment.pullDomainEvents()

        employment.cancelLastSuspend(now = now)

        `when`("status 검증") {
            then("status가 ACTIVE로 복원된다") {
                employment.status shouldBe EmploymentStatus.ACTIVE
            }
        }

        `when`("이벤트 검증") {
            then("EmployeeSuspendedCancelledEvent가 1건 적재된다") {
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0] as? EmployeeSuspendedCancelledEvent shouldNotBe null
            }
        }
    }

    given("ON_LEAVE가 아닌 Employment에 cancelLastSuspend를 호출하면") {
        val employment = createActiveEmployment()

        then("IneligibleCancellationException이 발생한다") {
            shouldThrow<IneligibleCancellationException> {
                employment.cancelLastSuspend(now = now)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // validateActive
    // ─────────────────────────────────────────────────────────────────
    given("ACTIVE 상태의 Employment에 validateActive를 호출하면") {
        val employment = createActiveEmployment()

        then("예외가 발생하지 않는다") {
            employment.validateActive()
        }
    }

    given("ON_LEAVE 상태의 Employment에 validateActive를 호출하면") {
        val employment = createOnLeaveEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.validateActive()
            }
        }
    }

    given("RESIGNED 상태의 Employment에 validateActive를 호출하면") {
        val employment = createResignedEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.validateActive()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // validateNotResigned
    // ─────────────────────────────────────────────────────────────────
    given("RESIGNED 상태의 Employment에 validateNotResigned를 호출하면") {
        val employment = createResignedEmployment()

        then("InvalidStateTransitionException이 발생한다") {
            shouldThrow<InvalidStateTransitionException> {
                employment.validateNotResigned()
            }
        }
    }

    given("ACTIVE 상태의 Employment에 validateNotResigned를 호출하면") {
        val employment = createActiveEmployment()

        then("예외가 발생하지 않는다") {
            employment.validateNotResigned()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // validateBelongsToCompany
    // ─────────────────────────────────────────────────────────────────
    given("같은 companyId를 가진 Employment에 validateBelongsToCompany를 호출하면") {
        val employment = createActiveEmployment(companyId = 10L)

        then("예외가 발생하지 않는다") {
            employment.validateBelongsToCompany(10L)
        }
    }

    given("다른 companyId로 validateBelongsToCompany를 호출하면") {
        val employment = createActiveEmployment(companyId = 10L)

        then("CrossCompanyAccessException이 발생한다") {
            shouldThrow<CrossCompanyAccessException> {
                employment.validateBelongsToCompany(99L)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // isAccessibleBy — TEAM_LEAD 범위
    // ─────────────────────────────────────────────────────────────────
    given("viewer가 같은 departmentId를 가진 경우") {
        val viewer = createActiveEmployment(id = 2L, departmentId = 5L)
        val target = createActiveEmployment(id = 1L, departmentId = 5L)

        then("isAccessibleBy가 true를 반환한다") {
            target.isAccessibleBy(viewer) shouldBe true
        }
    }

    given("viewer가 target의 직속 상관인 경우 (target.managerEmploymentId == viewer.id)") {
        val viewer = createActiveEmployment(id = 2L, departmentId = 99L)
        val target = Employment(
            id = 1L,
            spec = EmploymentSpec(
                personId = 100L,
                companyId = 10L,
                employeeNumber = "EMP-001",
                employmentType = EmploymentType.REGULAR,
                startDate = LocalDate.now(),
                country = "KR",
                currency = "KRW",
                timezone = "Asia/Seoul",
                managerEmploymentId = 2L,
            ),
            status = EmploymentStatus.ACTIVE,
            departmentId = 5L,
            createdAt = now,
            updatedAt = now,
        )

        then("isAccessibleBy가 true를 반환한다") {
            target.isAccessibleBy(viewer) shouldBe true
        }
    }

    given("viewer가 본인인 경우") {
        val viewer = createActiveEmployment(id = 1L, departmentId = 5L)
        val target = createActiveEmployment(id = 1L, departmentId = 5L)

        then("isAccessibleBy가 true를 반환한다") {
            target.isAccessibleBy(viewer) shouldBe true
        }
    }

    given("viewer가 다른 부서이고 직속 상관도 아닌 경우") {
        val viewer = createActiveEmployment(id = 2L, departmentId = 99L)
        val target = createActiveEmployment(id = 1L, departmentId = 5L, managerEmploymentId = null)

        then("isAccessibleBy가 false를 반환한다") {
            target.isAccessibleBy(viewer) shouldBe false
        }
    }

    given("viewer와 target이 다른 회사인 경우") {
        val viewer = createActiveEmployment(id = 2L, companyId = 10L, departmentId = 5L)
        val target = createActiveEmployment(id = 1L, companyId = 99L, departmentId = 5L)

        then("isAccessibleBy가 false를 반환한다") {
            target.isAccessibleBy(viewer) shouldBe false
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 금지 전이 확인
    // ─────────────────────────────────────────────────────────────────
    given("RESIGNED Employment에 어떤 상태 전이도 시도하면") {
        `when`("activate 시도") {
            then("InvalidStateTransitionException이 발생한다") {
                val employment = createResignedEmployment()
                shouldThrow<InvalidStateTransitionException> { employment.activate(now) }
            }
        }
        `when`("suspend 시도") {
            then("InvalidStateTransitionException이 발생한다") {
                val employment = createResignedEmployment()
                shouldThrow<InvalidStateTransitionException> {
                    employment.suspend(reason = "사유", until = null, now = now)
                }
            }
        }
        `when`("resume 시도") {
            then("InvalidStateTransitionException이 발생한다") {
                val employment = createResignedEmployment()
                shouldThrow<InvalidStateTransitionException> { employment.resume(now) }
            }
        }
        `when`("resign 시도") {
            then("InvalidStateTransitionException이 발생한다") {
                val employment = createResignedEmployment()
                shouldThrow<InvalidStateTransitionException> { employment.resign(now = now, reason = null) }
            }
        }
        `when`("resignDuringLeave 시도") {
            then("InvalidStateTransitionException이 발생한다") {
                val employment = createResignedEmployment()
                shouldThrow<InvalidStateTransitionException> { employment.resignDuringLeave(now = now, reason = null) }
            }
        }
        `when`("transferTo 시도") {
            then("InvalidStateTransitionException이 발생한다") {
                val employment = createResignedEmployment()
                shouldThrow<InvalidStateTransitionException> { employment.transferTo(newDepartmentId = 10L, now = now) }
            }
        }
        `when`("promote 시도") {
            then("InvalidStateTransitionException이 발생한다") {
                val employment = createResignedEmployment()
                shouldThrow<InvalidStateTransitionException> { employment.promote(newPositionId = 4L, now = now) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 티켓 BE-02a 명시 테스트 케이스
    // ─────────────────────────────────────────────────────────────────
    given("티켓 BE-02a 테스트 케이스 — ACTIVE Employment에 suspend(reason, until) 호출 시") {
        val employment = createActiveEmployment()
        val until = LocalDate.of(2026, 8, 31)

        employment.suspend(reason = "건강 문제", until = until, now = now)

        then("status=ON_LEAVE 이다") {
            employment.status shouldBe EmploymentStatus.ON_LEAVE
        }

        then("EmployeeSuspendedEvent 1건이 적재된다") {
            val events = employment.pullDomainEvents()
            events shouldHaveSize 1
            events[0] as? EmployeeSuspendedEvent shouldNotBe null
        }
    }
})
