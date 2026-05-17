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

class ResignEmploymentUseCaseTest : BehaviorSpec({

    val employmentDomainService = mockk<EmploymentDomainService>()
    val useCase = ResignEmploymentUseCase(employmentDomainService)

    val now = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC)

    val command = ResignEmploymentCommand(
        employmentId = 1L,
        reason = "개인 사정",
        actorEmploymentId = 100L,
    )

    given("ResignEmploymentUseCase.execute") {
        `when`("정상적인 ResignEmploymentCommand가 주어지면") {
            val mockEmployment = mockk<Employment> {
                every { id } returns 1L
                every { status } returns EmploymentStatus.RESIGNED
            }
            every {
                employmentDomainService.resign(
                    employmentId = 1L,
                    reason = "개인 사정",
                    actorEmploymentId = 100L,
                    now = now,
                )
            } returns mockEmployment

            then("DomainService.resign를 정확히 1회 호출하고 결과를 반환한다") {
                val result = useCase.execute(command, now)

                verify(exactly = 1) {
                    employmentDomainService.resign(employmentId = 1L, reason = "개인 사정", actorEmploymentId = 100L, now = now)
                }
                result.status shouldBe EmploymentStatus.RESIGNED
            }
        }

        `when`("DomainService가 InvalidStateTransitionException을 던지면") {
            every {
                employmentDomainService.resign(any(), any(), any(), any())
            } throws InvalidStateTransitionException(EmploymentStatus.RESIGNED, EmploymentStatus.ACTIVE)

            then("예외가 그대로 전파된다") {
                shouldThrow<InvalidStateTransitionException> {
                    useCase.execute(command, now)
                }
            }
        }
    }
})
