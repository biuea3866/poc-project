package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.admin.service.AdminAuthDomainService
import com.hrplatform.core.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class LogoutAllSessionsUseCaseTest : BehaviorSpec({

    val adminAuthDomainService = mockk<AdminAuthDomainService>()
    val useCase = LogoutAllSessionsUseCase(adminAuthDomainService)

    given("LogoutAllSessionsUseCase — 정상 호출") {
        then("terminateAllSessions 호출 시 DomainService.terminateAllSessions가 실행된다") {
            val command = LogoutAllSessionsCommand(userAccountId = 1L, actorEmploymentId = 100L)

            justRun { adminAuthDomainService.terminateAllSessions(1L, 100L) }

            useCase.execute(command)

            verify { adminAuthDomainService.terminateAllSessions(1L, 100L) }
        }
    }

    given("LogoutAllSessionsUseCase — 존재하지 않는 계정") {
        then("존재하지 않는 userAccountId로 호출 시 NotFoundException을 전파한다") {
            val command = LogoutAllSessionsCommand(userAccountId = 999L, actorEmploymentId = null)

            every { adminAuthDomainService.terminateAllSessions(999L, null) } throws NotFoundException(
                errorCode = "USER_ACCOUNT_NOT_FOUND",
                message = "UserAccount를 찾을 수 없습니다: 999",
            )

            shouldThrow<NotFoundException> {
                useCase.execute(command)
            }
        }
    }
})
