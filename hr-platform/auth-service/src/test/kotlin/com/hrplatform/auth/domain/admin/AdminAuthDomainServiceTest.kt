package com.hrplatform.auth.domain.admin

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.admin.service.AdminAuthDomainService
import com.hrplatform.auth.domain.token.ApiToken
import com.hrplatform.auth.domain.token.ApiTokenRepository
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.core.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AdminAuthDomainServiceTest : BehaviorSpec({

    fun buildLockedUserAccount(id: Long = 1L): UserAccount {
        val account = UserAccount.create(
            employmentId = 100L,
            companyId = 1L,
            email = "test@example.com",
            passwordHash = "hash",
        )
        val idField = UserAccount::class.java.superclass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(account, id)
        val statusField = UserAccount::class.java.getDeclaredField("status")
        statusField.isAccessible = true
        statusField.set(account, UserAccountStatus.LOCKED)
        val lockedUntilField = UserAccount::class.java.getDeclaredField("lockedUntil")
        lockedUntilField.isAccessible = true
        lockedUntilField.set(account, ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10))
        return account
    }

    given("unlock — 잠긴 계정 해제") {
        then("LOCKED 계정을 unlock 하면 ACTIVE 상태로 변경되고 이벤트가 발행된다") {
            val userAccount = buildLockedUserAccount()
            val userAccountRepository = mockk<UserAccountRepository>()
            val refreshTokenRepository = mockk<RefreshTokenRepository>()
            val apiTokenRepository = mockk<ApiTokenRepository>()
            val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)

            every { userAccountRepository.findById(1L) } returns userAccount
            every { userAccountRepository.save(any()) } answers { firstArg() }

            val service = AdminAuthDomainService(
                userAccountRepository, refreshTokenRepository, apiTokenRepository, eventPublisher,
            )

            service.unlock(1L, null)

            userAccount.status shouldBe UserAccountStatus.ACTIVE
            verify { eventPublisher.publishAll(any()) }
        }
    }

    given("unlock — 존재하지 않는 계정") {
        then("존재하지 않는 userAccountId로 unlock 시 NotFoundException이 발생한다") {
            val userAccountRepository = mockk<UserAccountRepository>()

            every { userAccountRepository.findById(999L) } returns null

            val service = AdminAuthDomainService(
                userAccountRepository, mockk(), mockk(), mockk(relaxed = true),
            )

            shouldThrow<NotFoundException> {
                service.unlock(999L, null)
            }
        }
    }

    given("terminateAllSessions") {
        then("terminateAllSessions 호출 시 모든 refresh token이 폐기된다") {
            val userAccount = buildLockedUserAccount()
            val userAccountRepository = mockk<UserAccountRepository>()
            val refreshTokenRepository = mockk<RefreshTokenRepository>()

            every { userAccountRepository.findById(1L) } returns userAccount
            every { refreshTokenRepository.revokeAllByUserAccountId(1L, any(), any()) } returns Unit

            val service = AdminAuthDomainService(
                userAccountRepository, refreshTokenRepository, mockk(), mockk(relaxed = true),
            )

            service.terminateAllSessions(1L, null)

            verify { refreshTokenRepository.revokeAllByUserAccountId(1L, "ADMIN_TERMINATE_ALL", any()) }
        }
    }

    given("issueApiToken") {
        then("issueApiToken 호출 시 hrp_ prefix 토큰이 1회 반환되고 DB에는 hash가 저장된다") {
            val userAccount = buildLockedUserAccount()
            val userAccountRepository = mockk<UserAccountRepository>()
            val apiTokenRepository = mockk<ApiTokenRepository>()

            every { userAccountRepository.findById(1L) } returns userAccount
            every { apiTokenRepository.save(any()) } answers {
                val apiToken = firstArg<ApiToken>()
                val idField = ApiToken::class.java.superclass.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(apiToken, 42L)
                apiToken
            }

            val service = AdminAuthDomainService(
                userAccountRepository, mockk(), apiTokenRepository, mockk(relaxed = true),
            )

            val result = service.issueApiToken(
                userAccountId = 1L,
                name = "test-token",
                scopes = listOf("read", "write"),
                expiresAt = null,
                actorEmploymentId = null,
            )

            result.rawToken shouldStartWith "hrp_"
            result.name shouldBe "test-token"
            result.scopes shouldBe listOf("read", "write")
            result.apiTokenId shouldBe 42L
        }
    }

    given("revokeApiToken") {
        then("유효한 apiTokenId로 revokeApiToken 호출 시 토큰이 폐기된다") {
            val apiToken = ApiToken(
                userAccountId = 1L,
                name = "test",
                tokenHash = "hash",
                scopes = emptyList(),
                expiresAt = null,
                lastUsedAt = null,
                revokedAt = null,
            )
            val apiTokenRepository = mockk<ApiTokenRepository>()

            every { apiTokenRepository.findById(42L) } returns apiToken
            every { apiTokenRepository.save(any()) } answers { firstArg() }

            val service = AdminAuthDomainService(
                mockk(), mockk(), apiTokenRepository, mockk(relaxed = true),
            )

            service.revokeApiToken(42L, null)

            apiToken.revokedAt shouldNotBe null
            verify { apiTokenRepository.save(any()) }
        }
    }
})
