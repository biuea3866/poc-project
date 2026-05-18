package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import com.hrplatform.core.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class ConfirmPasswordResetUseCaseTest : BehaviorSpec({

    val authDomainService = mockk<AuthDomainService>()
    val useCase = ConfirmPasswordResetUseCase(authDomainService)

    given("ConfirmPasswordResetUseCase — 유효한 토큰") {
        then("유효한 토큰과 새 비밀번호로 confirmPasswordReset 호출 시 정상 처리된다") {
            val command = ConfirmPasswordResetCommand(
                token = "valid-raw-token",
                newPassword = "NewPassword123!",
            )

            justRun { authDomainService.confirmPasswordReset("valid-raw-token", "NewPassword123!") }

            useCase.execute(command)

            verify { authDomainService.confirmPasswordReset("valid-raw-token", "NewPassword123!") }
        }
    }

    given("ConfirmPasswordResetUseCase — 유효하지 않은 토큰") {
        then("유효하지 않은 토큰으로 호출 시 DomainService에서 던진 BusinessException을 전파한다") {
            val command = ConfirmPasswordResetCommand(
                token = "invalid-token",
                newPassword = "NewPassword123!",
            )

            io.mockk.every {
                authDomainService.confirmPasswordReset("invalid-token", "NewPassword123!")
            } throws BusinessException(
                errorCode = "INVALID_RESET_TOKEN",
                message = "유효하지 않거나 만료된 비밀번호 재설정 토큰입니다",
            )

            shouldThrow<BusinessException> {
                useCase.execute(command)
            }
        }
    }

    given("ConfirmPasswordResetUseCase — 만료된 토큰") {
        then("만료된 토큰으로 호출 시 BusinessException을 전파한다") {
            val command = ConfirmPasswordResetCommand(
                token = "expired-token",
                newPassword = "NewPassword123!",
            )

            io.mockk.every {
                authDomainService.confirmPasswordReset("expired-token", "NewPassword123!")
            } throws BusinessException(
                errorCode = "INVALID_RESET_TOKEN",
                message = "만료된 비밀번호 재설정 토큰입니다",
            )

            shouldThrow<BusinessException> {
                useCase.execute(command)
            }
        }
    }
})
