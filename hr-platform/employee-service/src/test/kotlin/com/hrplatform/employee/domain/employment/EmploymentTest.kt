package com.hrplatform.employee.domain.employment

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.ZonedDateTime

class EmploymentTest : BehaviorSpec({

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
    )

    fun buildActiveEmployment(): Employment {
        val employment = buildPreHiredEmployment()
        employment.activate(now, actorId)
        employment.pullDomainEvents() // 이벤트 클리어
        return employment
    }

    // ========== 상태 전이 — 정상 경로 ==========

    given("PRE_HIRED Employment") {
        `when`("activate 호출") {
            val employment = buildPreHiredEmployment()
            then("상태가 ACTIVE로 변경되고 EmployeeHiredEvent가 적재된다") {
                employment.activate(now, actorId)
                employment.status shouldBe EmploymentStatus.ACTIVE
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeHired"
            }
        }
    }

    given("ACTIVE Employment") {
        `when`("suspend 호출") {
            val employment = buildActiveEmployment()
            then("상태가 ON_LEAVE로 변경되고 EmployeeSuspendedEvent가 적재된다") {
                employment.suspend("병가", LocalDate.of(2026, 2, 28), now, actorId)
                employment.status shouldBe EmploymentStatus.ON_LEAVE
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeSuspended"
            }
        }

        `when`("resign 호출") {
            val employment = buildActiveEmployment()
            then("상태가 RESIGNED로 변경되고 EmployeeResignedEvent가 적재된다") {
                employment.resign(now, "개인 사유", actorId)
                employment.status shouldBe EmploymentStatus.RESIGNED
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeResigned"
            }
        }

        `when`("transferTo 호출") {
            val employment = buildActiveEmployment()
            employment.departmentId = 5L
            then("departmentId 변경되고 EmployeeTransferredEvent가 적재된다") {
                employment.transferTo(newDepartmentId = 20L, now = now, actorEmploymentId = actorId)
                employment.departmentId shouldBe 20L
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeTransferred"
            }
        }

        `when`("promote 호출") {
            val employment = buildActiveEmployment()
            employment.positionId = 3L
            then("positionId 변경되고 EmployeePromotedEvent가 적재된다") {
                employment.promote(newPositionId = 7L, now = now, actorEmploymentId = actorId)
                employment.positionId shouldBe 7L
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeePromoted"
            }
        }

        `when`("changeCompensation 호출") {
            val employment = buildActiveEmployment()
            employment.baseSalary = 3_000_000L
            employment.compensationCurrency = "KRW"
            then("baseSalary 변경되고 EmployeeSalaryChangedEvent가 적재된다") {
                employment.changeCompensation(
                    newBaseSalary = 4_000_000L,
                    newCurrency = "KRW",
                    now = now,
                    actorEmploymentId = actorId,
                )
                employment.baseSalary shouldBe 4_000_000L
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeSalaryChanged"
            }
        }
    }

    given("ON_LEAVE Employment") {
        `when`("resume 호출") {
            val employment = buildActiveEmployment()
            employment.suspend("병가", null, now, actorId)
            employment.pullDomainEvents()
            then("상태가 ACTIVE로 변경되고 EmployeeResumedEvent가 적재된다") {
                employment.resume(now, actorId)
                employment.status shouldBe EmploymentStatus.ACTIVE
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeResumed"
            }
        }

        `when`("resign 호출") {
            val employment = buildActiveEmployment()
            employment.suspend("병가", null, now, actorId)
            employment.pullDomainEvents()
            then("상태가 RESIGNED로 변경되고 EmployeeResignedEvent가 적재된다") {
                employment.resign(now, "퇴사", actorId)
                employment.status shouldBe EmploymentStatus.RESIGNED
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeResigned"
            }
        }
    }

    // ========== 금지 전이 ==========

    given("PRE_HIRED Employment") {
        `when`("RESIGNED로 직접 전이 시도") {
            val employment = buildPreHiredEmployment()
            then("InvalidStateTransitionException throw") {
                shouldThrow<InvalidStateTransitionException> {
                    employment.resign(now, null, actorId)
                }
            }
        }
        `when`("ON_LEAVE로 직접 전이 시도") {
            val employment = buildPreHiredEmployment()
            then("InvalidStateTransitionException throw") {
                shouldThrow<InvalidStateTransitionException> {
                    employment.suspend("이유", null, now, actorId)
                }
            }
        }
    }

    given("RESIGNED Employment") {
        `when`("activate 시도") {
            val employment = buildActiveEmployment()
            employment.resign(now, null, actorId)
            employment.pullDomainEvents()
            then("InvalidStateTransitionException throw") {
                shouldThrow<InvalidStateTransitionException> {
                    employment.activate(now, actorId)
                }
            }
        }
        `when`("suspend 시도") {
            val employment = buildActiveEmployment()
            employment.resign(now, null, actorId)
            employment.pullDomainEvents()
            then("InvalidStateTransitionException throw") {
                shouldThrow<InvalidStateTransitionException> {
                    employment.suspend("이유", null, now, actorId)
                }
            }
        }
        `when`("cancelLastTransfer 시도") {
            val employment = buildActiveEmployment()
            employment.resign(now, null, actorId)
            employment.pullDomainEvents()
            then("validateNotResigned 차단으로 InvalidStateTransitionException throw") {
                shouldThrow<InvalidStateTransitionException> {
                    employment.cancelLastTransfer(
                        cancelledHistoryId = 1L,
                        cancellationReason = "오기재",
                        previousDepartmentId = 5L,
                        now = now,
                        actorEmploymentId = actorId,
                    )
                }
            }
        }
        `when`("cancelLastPromotion 시도") {
            val employment = buildActiveEmployment()
            employment.resign(now, null, actorId)
            employment.pullDomainEvents()
            then("validateNotResigned 차단으로 InvalidStateTransitionException throw") {
                shouldThrow<InvalidStateTransitionException> {
                    employment.cancelLastPromotion(
                        cancelledHistoryId = 1L,
                        cancellationReason = "오기재",
                        previousPositionId = 3L,
                        now = now,
                        actorEmploymentId = actorId,
                    )
                }
            }
        }
        `when`("cancelLastSalaryChange 시도") {
            val employment = buildActiveEmployment()
            employment.resign(now, null, actorId)
            employment.pullDomainEvents()
            then("validateNotResigned 차단으로 InvalidStateTransitionException throw") {
                shouldThrow<InvalidStateTransitionException> {
                    employment.cancelLastSalaryChange(
                        cancelledHistoryId = 1L,
                        cancellationReason = "오기재",
                        previousBaseSalary = 3_000_000L,
                        previousCurrency = "KRW",
                        now = now,
                        actorEmploymentId = actorId,
                    )
                }
            }
        }
        `when`("cancelLastSuspend 시도") {
            val employment = buildActiveEmployment()
            employment.resign(now, null, actorId)
            employment.pullDomainEvents()
            then("validateNotResigned 차단으로 InvalidStateTransitionException throw") {
                shouldThrow<InvalidStateTransitionException> {
                    employment.cancelLastSuspend(
                        cancelledHistoryId = 1L,
                        cancellationReason = "오기재",
                        previousSuspendReason = "병가",
                        previousSuspendUntil = null,
                        now = now,
                        actorEmploymentId = actorId,
                    )
                }
            }
        }
    }

    // ========== 발령 취소 4종 — 정상 경로 ==========

    given("ACTIVE Employment의 cancelLastTransfer") {
        val employment = buildActiveEmployment()
        employment.departmentId = 5L
        `when`("cancelLastTransfer 호출") {
            then("departmentId가 이전 값으로 복원되고 EmployeeTransferredCancelledEvent가 적재된다") {
                employment.cancelLastTransfer(
                    cancelledHistoryId = 99L,
                    cancellationReason = "오기재",
                    previousDepartmentId = 5L,
                    now = now,
                    actorEmploymentId = actorId,
                )
                employment.departmentId shouldBe 5L
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeTransferredCancelled"
            }
        }
    }

    given("ACTIVE Employment의 cancelLastPromotion") {
        val employment = buildActiveEmployment()
        employment.positionId = 7L
        `when`("cancelLastPromotion 호출") {
            then("positionId가 이전 값으로 복원되고 EmployeePromotedCancelledEvent가 적재된다") {
                employment.cancelLastPromotion(
                    cancelledHistoryId = 99L,
                    cancellationReason = "오기재",
                    previousPositionId = 3L,
                    now = now,
                    actorEmploymentId = actorId,
                )
                employment.positionId shouldBe 3L
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeePromotedCancelled"
            }
        }
    }

    given("ACTIVE Employment의 cancelLastSalaryChange") {
        val employment = buildActiveEmployment()
        employment.baseSalary = 4_000_000L
        employment.compensationCurrency = "KRW"
        `when`("cancelLastSalaryChange 호출") {
            then("baseSalary가 이전 값으로 복원되고 EmployeeSalaryChangedCancelledEvent가 적재된다") {
                employment.cancelLastSalaryChange(
                    cancelledHistoryId = 99L,
                    cancellationReason = "오기재",
                    previousBaseSalary = 3_000_000L,
                    previousCurrency = "KRW",
                    now = now,
                    actorEmploymentId = actorId,
                )
                employment.baseSalary shouldBe 3_000_000L
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeSalaryChangedCancelled"
            }
        }
    }

    given("ON_LEAVE Employment의 cancelLastSuspend") {
        val employment = buildActiveEmployment()
        employment.suspend("병가", LocalDate.of(2026, 2, 28), now, actorId)
        employment.pullDomainEvents()
        `when`("cancelLastSuspend 호출") {
            then("상태가 ACTIVE로 복원되고 EmployeeSuspendedCancelledEvent가 적재된다") {
                employment.cancelLastSuspend(
                    cancelledHistoryId = 99L,
                    cancellationReason = "오기재",
                    previousSuspendReason = "병가",
                    previousSuspendUntil = LocalDate.of(2026, 2, 28),
                    now = now,
                    actorEmploymentId = actorId,
                )
                employment.status shouldBe EmploymentStatus.ACTIVE
                val events = employment.pullDomainEvents()
                events shouldHaveSize 1
                events[0].eventType shouldBe "EmployeeSuspendedCancelled"
            }
        }
    }

    // ========== isAccessibleBy ==========

    given("isAccessibleBy 검증") {
        `when`("같은 companyId의 ACTIVE viewer") {
            val employment = buildActiveEmployment()
            val viewer = buildActiveEmployment()
            then("접근 허용") {
                employment.isAccessibleBy(viewer) shouldBe true
            }
        }

        `when`("다른 companyId의 viewer") {
            val employment = buildActiveEmployment()
            val viewer = Employment(
                personId = 2L,
                companyId = 99L, // 다른 회사
                employeeNumber = "EMP-002",
                employmentType = EmploymentType.REGULAR,
                status = EmploymentStatus.ACTIVE,
                startDate = LocalDate.of(2026, 1, 15),
                country = "KR",
                currency = "KRW",
                timezone = "Asia/Seoul",
            )
            then("접근 거부") {
                employment.isAccessibleBy(viewer) shouldBe false
            }
        }

        `when`("RESIGNED 상태의 viewer") {
            val employment = buildActiveEmployment()
            val viewer = buildActiveEmployment()
            viewer.resign(now, null, actorId)
            then("접근 거부 — RESIGNED viewer는 접근 불가") {
                employment.isAccessibleBy(viewer) shouldBe false
            }
        }
    }
})
