package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.IneligibleCancellationException
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

class RecordAndCancelEmploymentEventUseCaseTest : BehaviorSpec({

    val employmentDomainService = mockk<EmploymentDomainService>()
    val recordUseCase = RecordEmploymentEventUseCase(employmentDomainService)
    val cancelUseCase = CancelEmploymentEventUseCase(employmentDomainService)

    val now = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC)

    given("RecordEmploymentEventUseCase.execute") {
        val command = RecordEmploymentEventCommand(
            employmentId = 1L,
            eventType = RecordableEventType.DEPT_CHANGE,
            newDepartmentId = 20L,
            effectiveDate = LocalDate.of(2026, 1, 10),
            actorEmploymentId = 100L,
        )

        `when`("정상적인 RecordEmploymentEventCommand가 주어지면") {
            val mockEmployment = mockk<Employment> {
                every { id } returns 1L
                every { status } returns EmploymentStatus.ACTIVE
                every { departmentId } returns 20L
                every { positionId } returns null
                every { baseSalary } returns null
            }
            every { employmentDomainService.recordEvent(any(), now) } returns mockEmployment

            then("DomainService.recordEvent를 정확히 1회 호출한다") {
                val result = recordUseCase.execute(command, now)

                verify(exactly = 1) { employmentDomainService.recordEvent(any(), now) }
                result.departmentId shouldBe 20L
            }
        }

        `when`("DomainService가 예외를 던지면") {
            every { employmentDomainService.recordEvent(any(), now) } throws RuntimeException("도메인 예외")

            then("예외가 그대로 전파된다") {
                shouldThrow<RuntimeException> { recordUseCase.execute(command, now) }
            }
        }
    }

    given("CancelEmploymentEventUseCase.execute") {
        val command = CancelEmploymentEventCommand(
            employmentId = 1L,
            historyId = 5L,
            cancellationReason = "오입력",
            actorEmploymentId = 100L,
        )

        `when`("정상적인 CancelEmploymentEventCommand가 주어지면") {
            val mockEmployment = mockk<Employment> {
                every { id } returns 1L
                every { status } returns EmploymentStatus.ACTIVE
            }
            every {
                employmentDomainService.cancelEvent(
                    employmentId = 1L,
                    historyId = 5L,
                    cancellationReason = "오입력",
                    actorEmploymentId = 100L,
                    now = now,
                )
            } returns mockEmployment

            then("DomainService.cancelEvent를 정확히 1회 호출한다") {
                val result = cancelUseCase.execute(command, now)

                verify(exactly = 1) {
                    employmentDomainService.cancelEvent(
                        employmentId = 1L,
                        historyId = 5L,
                        cancellationReason = "오입력",
                        actorEmploymentId = 100L,
                        now = now,
                    )
                }
                result.status shouldBe EmploymentStatus.ACTIVE
            }
        }

        `when`("직전이 아닌 이력에 대해 취소를 시도하면") {
            every {
                employmentDomainService.cancelEvent(any(), any(), any(), any(), any())
            } throws IneligibleCancellationException("직전 이력만 취소할 수 있습니다")

            then("IneligibleCancellationException이 전파된다") {
                shouldThrow<IneligibleCancellationException> { cancelUseCase.execute(command, now) }
            }
        }
    }
})
