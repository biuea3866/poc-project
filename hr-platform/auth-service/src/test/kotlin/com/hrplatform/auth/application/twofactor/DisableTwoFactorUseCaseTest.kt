package com.hrplatform.auth.application.twofactor

import com.hrplatform.auth.domain.twofactor.service.TwoFactorDomainService
import com.hrplatform.core.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class DisableTwoFactorUseCaseTest : BehaviorSpec({

    val twoFactorDomainService = mockk<TwoFactorDomainService>()
    val useCase = DisableTwoFactorUseCase(twoFactorDomainService)

    given("DisableTwoFactorUseCase — 정상 호출") {
        then("disable 호출 시 DomainService.disable이 실행된다") {
            val command = DisableTwoFactorCommand(userAccountId = 1L, actorEmploymentId = 100L)

            justRun { twoFactorDomainService.disable(1L, 100L) }

            useCase.execute(command)

            verify { twoFactorDomainService.disable(1L, 100L) }
        }
    }

    given("DisableTwoFactorUseCase — 존재하지 않는 계정") {
        then("존재하지 않는 userAccountId로 disable 호출 시 NotFoundException을 전파한다") {
            val command = DisableTwoFactorCommand(userAccountId = 999L, actorEmploymentId = null)

            every { twoFactorDomainService.disable(999L, null) } throws NotFoundException(
                errorCode = "USER_ACCOUNT_NOT_FOUND",
                message = "UserAccount를 찾을 수 없습니다: 999",
            )

            shouldThrow<NotFoundException> {
                useCase.execute(command)
            }
        }
    }
})
