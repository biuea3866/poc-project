package com.hrplatform.auth.application.twofactor

import com.hrplatform.auth.domain.twofactor.service.TwoFactorDomainService
import com.hrplatform.auth.domain.twofactor.service.TwoFactorEnrollmentResult
import com.hrplatform.core.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class EnrollTwoFactorUseCaseTest : BehaviorSpec({

    val twoFactorDomainService = mockk<TwoFactorDomainService>()
    val useCase = EnrollTwoFactorUseCase(twoFactorDomainService)

    given("EnrollTwoFactorUseCase — 정상 호출") {
        then("enroll 호출 시 TwoFactorEnrollmentResult를 반환한다") {
            val command = EnrollTwoFactorCommand(userAccountId = 1L, actorEmploymentId = null)
            val expected = TwoFactorEnrollmentResult(
                qrCodeDataUri = "data:image/png;base64,...",
                secret = "JBSWY3DPEHPK3PXP",
                backupCodes = listOf("code1", "code2", "code3", "code4", "code5"),
            )

            every { twoFactorDomainService.enroll(1L, null) } returns expected

            val result = useCase.execute(command)

            result shouldBe expected
            verify { twoFactorDomainService.enroll(1L, null) }
        }
    }

    given("EnrollTwoFactorUseCase — DomainService 예외 전파") {
        then("DomainService가 NotFoundException을 던지면 UseCase도 전파한다") {
            val command = EnrollTwoFactorCommand(userAccountId = 999L, actorEmploymentId = null)

            every { twoFactorDomainService.enroll(999L, null) } throws NotFoundException(
                errorCode = "USER_ACCOUNT_NOT_FOUND",
                message = "UserAccount를 찾을 수 없습니다: 999",
            )

            shouldThrow<NotFoundException> {
                useCase.execute(command)
            }
        }
    }
})
