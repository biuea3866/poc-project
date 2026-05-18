package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class RequestPasswordResetUseCaseTest : BehaviorSpec({

    val authDomainService = mockk<AuthDomainService>()
    val useCase = RequestPasswordResetUseCase(authDomainService)

    given("RequestPasswordResetUseCase — 정상 호출") {
        then("requestPasswordReset 호출 시 rawToken을 반환한다") {
            val command = RequestPasswordResetCommand(email = "user@example.com")

            every { authDomainService.requestPasswordReset("user@example.com") } returns "raw-token-abc"

            val result = useCase.execute(command)

            result shouldBe "raw-token-abc"
        }
    }

    given("RequestPasswordResetUseCase — 존재하지 않는 이메일") {
        then("존재하지 않는 이메일이어도 정상 토큰을 반환한다 (timing attack 방어)") {
            val command = RequestPasswordResetCommand(email = "unknown@example.com")

            every { authDomainService.requestPasswordReset("unknown@example.com") } returns "fake-token"

            val result = useCase.execute(command)

            result shouldBe "fake-token"
        }
    }
})
