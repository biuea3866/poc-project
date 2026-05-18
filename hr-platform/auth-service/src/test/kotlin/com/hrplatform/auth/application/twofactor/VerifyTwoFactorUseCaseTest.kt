package com.hrplatform.auth.application.twofactor

import com.hrplatform.auth.domain.twofactor.service.TwoFactorDomainService
import com.hrplatform.core.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class VerifyTwoFactorUseCaseTest : BehaviorSpec({

    val twoFactorDomainService = mockk<TwoFactorDomainService>()
    val useCase = VerifyTwoFactorUseCase(twoFactorDomainService)

    given("VerifyTwoFactorUseCase — 올바른 OTP") {
        then("올바른 OTP로 verify 호출 시 true를 반환한다") {
            val command = VerifyTwoFactorCommand(userAccountId = 1L, otp = "123456")

            every { twoFactorDomainService.verify(1L, "123456") } returns true

            val result = useCase.execute(command)

            result shouldBe true
            verify { twoFactorDomainService.verify(1L, "123456") }
        }
    }

    given("VerifyTwoFactorUseCase — 2FA 미등록 계정") {
        then("2FA가 등록되지 않은 계정이면 BusinessException을 전파한다") {
            val command = VerifyTwoFactorCommand(userAccountId = 2L, otp = "000000")

            every { twoFactorDomainService.verify(2L, "000000") } throws BusinessException(
                errorCode = "2FA_NOT_ENROLLED",
                message = "2FA가 등록되지 않은 계정입니다",
            )

            shouldThrow<BusinessException> {
                useCase.execute(command)
            }
        }
    }
})
