package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.InvalidStateTransitionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZoneOffset
import java.time.ZonedDateTime

class SuspendAndResumeEmploymentUseCaseTest : BehaviorSpec({

    val employmentDomainService = mockk<EmploymentDomainService>()
    val suspendUseCase = SuspendEmploymentUseCase(employmentDomainService)
    val resumeUseCase = ResumeEmploymentUseCase(employmentDomainService)

    val now = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC)

    given("SuspendEmploymentUseCase.execute") {
        val command = SuspendEmploymentCommand(
            employmentId = 1L,
            reason = "병가",
            until = null,
            actorEmploymentId = 100L,
        )

        `when`("정상적인 SuspendEmploymentCommand가 주어지면") {
            val mockEmployment = mockk<Employment> {
                every { id } returns 1L
                every { status } returns EmploymentStatus.ON_LEAVE
            }
            every {
                employmentDomainService.suspend(
                    employmentId = 1L,
                    reason = "병가",
                    until = null,
                    actorEmploymentId = 100L,
                    now = now,
                )
            } returns mockEmployment

            then("DomainService.suspend를 정확히 1회 호출하고 ON_LEAVE 상태를 반환한다") {
                val result = suspendUseCase.execute(command, now)

                verify(exactly = 1) {
                    employmentDomainService.suspend(
                        employmentId = 1L,
                        reason = "병가",
                        until = null,
                        actorEmploymentId = 100L,
                        now = now,
                    )
                }
                result.status shouldBe EmploymentStatus.ON_LEAVE
            }
        }

        `when`("RESIGNED Employment에 대해 호출하면") {
            every {
                employmentDomainService.suspend(any(), any(), any(), any(), any())
            } throws InvalidStateTransitionException(EmploymentStatus.RESIGNED, EmploymentStatus.ON_LEAVE)

            then("InvalidStateTransitionException이 전파된다") {
                shouldThrow<InvalidStateTransitionException> { suspendUseCase.execute(command, now) }
            }
        }
    }

    given("ResumeEmploymentUseCase.execute") {
        val command = ResumeEmploymentCommand(
            employmentId = 1L,
            actorEmploymentId = 100L,
        )

        `when`("정상적인 ResumeEmploymentCommand가 주어지면") {
            val mockEmployment = mockk<Employment> {
                every { id } returns 1L
                every { status } returns EmploymentStatus.ACTIVE
            }
            every {
                employmentDomainService.resume(
                    employmentId = 1L,
                    actorEmploymentId = 100L,
                    now = now,
                )
            } returns mockEmployment

            then("DomainService.resume를 정확히 1회 호출하고 ACTIVE 상태를 반환한다") {
                val result = resumeUseCase.execute(command, now)

                verify(exactly = 1) {
                    employmentDomainService.resume(employmentId = 1L, actorEmploymentId = 100L, now = now)
                }
                result.status shouldBe EmploymentStatus.ACTIVE
            }
        }

        `when`("DomainService가 예외를 던지면") {
            every {
                employmentDomainService.resume(any(), any(), any())
            } throws RuntimeException("도메인 예외")

            then("예외가 그대로 전파된다") {
                shouldThrow<RuntimeException> { resumeUseCase.execute(command, now) }
            }
        }
    }
})
