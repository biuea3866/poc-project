package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import com.hrplatform.auth.infrastructure.passwordreset.PasswordResetTokenStore
import com.hrplatform.core.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class ConfirmPasswordResetUseCaseTest : BehaviorSpec({

    val authDomainService = mockk<AuthDomainService>()
    val passwordResetTokenStore = mockk<PasswordResetTokenStore>()
    val useCase = ConfirmPasswordResetUseCase(authDomainService, passwordResetTokenStore)

    given("ConfirmPasswordResetUseCase — 유효한 토큰") {
        then("유효한 토큰과 새 비밀번호로 confirmPasswordReset 호출 시 정상 처리된다") {
            val command = ConfirmPasswordResetCommand(
                token = "valid-token",
                newPassword = "NewPassword123!",
            )

            every { passwordResetTokenStore.validate("valid-token") } returns Unit
            justRun { authDomainService.confirmPasswordReset("valid-token", "NewPassword123!") }
            justRun { passwordResetTokenStore.consume("valid-token") }

            useCase.execute(command)

            verify { passwordResetTokenStore.validate("valid-token") }
            verify { authDomainService.confirmPasswordReset("valid-token", "NewPassword123!") }
            verify { passwordResetTokenStore.consume("valid-token") }
        }
    }

    given("ConfirmPasswordResetUseCase — 유효하지 않은 토큰") {
        then("유효하지 않은 토큰으로 호출 시 BusinessException을 전파한다") {
            val command = ConfirmPasswordResetCommand(
                token = "invalid-token",
                newPassword = "NewPassword123!",
            )

            every { passwordResetTokenStore.validate("invalid-token") } throws BusinessException(
                errorCode = "INVALID_RESET_TOKEN",
                message = "유효하지 않거나 만료된 비밀번호 재설정 토큰입니다",
            )

            shouldThrow<BusinessException> {
                useCase.execute(command)
            }
        }
    }
})
