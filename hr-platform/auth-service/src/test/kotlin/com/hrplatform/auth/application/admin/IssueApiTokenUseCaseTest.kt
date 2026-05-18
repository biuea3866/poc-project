package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.admin.service.AdminAuthDomainService
import com.hrplatform.auth.domain.auth.service.ApiTokenResult
import com.hrplatform.core.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class IssueApiTokenUseCaseTest : BehaviorSpec({

    val adminAuthDomainService = mockk<AdminAuthDomainService>()
    val useCase = IssueApiTokenUseCase(adminAuthDomainService)

    given("IssueApiTokenUseCase — 정상 호출") {
        then("issueApiToken 호출 시 ApiTokenIssueResult를 반환한다") {
            val command = IssueApiTokenCommand(
                userAccountId = 1L,
                name = "test-token",
                scopes = listOf("read", "write"),
                expiresAt = null,
                actorEmploymentId = 100L,
            )
            val expected = ApiTokenResult(
                apiTokenId = 42L,
                rawToken = "hrp_abc123",
                name = "test-token",
                scopes = listOf("read", "write"),
                expiresAt = null,
            )

            every {
                adminAuthDomainService.issueApiToken(
                    userAccountId = 1L,
                    name = "test-token",
                    scopes = listOf("read", "write"),
                    expiresAt = null,
                    actorEmploymentId = 100L,
                )
            } returns expected

            val result = useCase.execute(command)

            result shouldBe expected
            verify {
                adminAuthDomainService.issueApiToken(
                    userAccountId = 1L,
                    name = "test-token",
                    scopes = listOf("read", "write"),
                    expiresAt = null,
                    actorEmploymentId = 100L,
                )
            }
        }
    }

    given("IssueApiTokenUseCase — 존재하지 않는 계정") {
        then("존재하지 않는 userAccountId로 호출 시 NotFoundException을 전파한다") {
            val command = IssueApiTokenCommand(
                userAccountId = 999L,
                name = "token",
                scopes = emptyList(),
                expiresAt = null,
                actorEmploymentId = null,
            )

            every {
                adminAuthDomainService.issueApiToken(
                    userAccountId = 999L,
                    name = "token",
                    scopes = emptyList(),
                    expiresAt = null,
                    actorEmploymentId = null,
                )
            } throws NotFoundException(
                errorCode = "USER_ACCOUNT_NOT_FOUND",
                message = "UserAccount를 찾을 수 없습니다: 999",
            )

            shouldThrow<NotFoundException> {
                useCase.execute(command)
            }
        }
    }
})
