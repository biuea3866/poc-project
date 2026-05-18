package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.admin.service.AdminAuthDomainService
import com.hrplatform.core.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class RevokeApiTokenUseCaseTest : BehaviorSpec({

    val adminAuthDomainService = mockk<AdminAuthDomainService>()
    val useCase = RevokeApiTokenUseCase(adminAuthDomainService)

    given("RevokeApiTokenUseCase — 정상 호출") {
        then("revokeApiToken 호출 시 DomainService.revokeApiToken이 실행된다") {
            val command = RevokeApiTokenCommand(apiTokenId = 42L, actorEmploymentId = 100L)

            justRun { adminAuthDomainService.revokeApiToken(42L, 100L) }

            useCase.execute(command)

            verify { adminAuthDomainService.revokeApiToken(42L, 100L) }
        }
    }

    given("RevokeApiTokenUseCase — 존재하지 않는 토큰") {
        then("존재하지 않는 apiTokenId로 호출 시 NotFoundException을 전파한다") {
            val command = RevokeApiTokenCommand(apiTokenId = 999L, actorEmploymentId = null)

            every { adminAuthDomainService.revokeApiToken(999L, null) } throws NotFoundException(
                errorCode = "API_TOKEN_NOT_FOUND",
                message = "ApiToken을 찾을 수 없습니다: 999",
            )

            shouldThrow<NotFoundException> {
                useCase.execute(command)
            }
        }
    }
})
