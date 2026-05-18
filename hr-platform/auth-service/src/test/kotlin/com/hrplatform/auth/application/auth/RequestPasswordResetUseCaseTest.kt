package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import com.hrplatform.auth.infrastructure.passwordreset.PasswordResetTokenStore
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class RequestPasswordResetUseCaseTest : BehaviorSpec({

    val authDomainService = mockk<AuthDomainService>()
    val passwordResetTokenStore = mockk<PasswordResetTokenStore>()
    val useCase = RequestPasswordResetUseCase(authDomainService, passwordResetTokenStore)

    given("RequestPasswordResetUseCase — 정상 호출") {
        then("requestPasswordReset 호출 시 토큰을 저장하고 반환한다") {
            val command = RequestPasswordResetCommand(email = "user@example.com")

            every { authDomainService.requestPasswordReset("user@example.com") } returns "reset-token-123"
            justRun { passwordResetTokenStore.store("reset-token-123") }

            val result = useCase.execute(command)

            result shouldBe "reset-token-123"
            verify { authDomainService.requestPasswordReset("user@example.com") }
            verify { passwordResetTokenStore.store("reset-token-123") }
        }
    }

    given("RequestPasswordResetUseCase — 존재하지 않는 이메일") {
        then("존재하지 않는 이메일이어도 정상 토큰을 반환한다 (timing attack 방어)") {
            val command = RequestPasswordResetCommand(email = "unknown@example.com")

            every { authDomainService.requestPasswordReset("unknown@example.com") } returns "fake-token"
            justRun { passwordResetTokenStore.store("fake-token") }

            val result = useCase.execute(command)

            result shouldBe "fake-token"
        }
    }
})
