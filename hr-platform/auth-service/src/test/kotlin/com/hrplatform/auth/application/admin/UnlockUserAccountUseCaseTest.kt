package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.admin.service.AdminAuthDomainService
import com.hrplatform.core.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class UnlockUserAccountUseCaseTest : BehaviorSpec({

    val adminAuthDomainService = mockk<AdminAuthDomainService>()
    val useCase = UnlockUserAccountUseCase(adminAuthDomainService)

    given("UnlockUserAccountUseCase — 정상 호출") {
        then("unlock 호출 시 DomainService.unlock이 실행된다") {
            val command = UnlockUserAccountCommand(userAccountId = 1L, actorEmploymentId = 100L)

            justRun { adminAuthDomainService.unlock(1L, 100L) }

            useCase.execute(command)

            verify { adminAuthDomainService.unlock(1L, 100L) }
        }
    }

    given("UnlockUserAccountUseCase — 존재하지 않는 계정") {
        then("존재하지 않는 userAccountId로 unlock 호출 시 NotFoundException을 전파한다") {
            val command = UnlockUserAccountCommand(userAccountId = 999L, actorEmploymentId = null)

            every { adminAuthDomainService.unlock(999L, null) } throws NotFoundException(
                errorCode = "USER_ACCOUNT_NOT_FOUND",
                message = "UserAccount를 찾을 수 없습니다: 999",
            )

            shouldThrow<NotFoundException> {
                useCase.execute(command)
            }
        }
    }
})
