package com.hrplatform.auth.domain.auth

import com.hrplatform.auth.domain.account.EmailHashService
import com.hrplatform.auth.domain.account.PasswordResetToken
import com.hrplatform.auth.domain.account.PasswordResetTokenRepository
import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.auth.service.AuthDomainService
import com.hrplatform.auth.domain.auth.service.JwtTokenService
import com.hrplatform.auth.domain.auth.service.TokenPair
import com.hrplatform.auth.domain.login.LoginAttempt
import com.hrplatform.auth.domain.login.LoginAttemptRepository
import com.hrplatform.auth.domain.token.JtiBlacklist
import com.hrplatform.auth.domain.token.RefreshToken
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.auth.domain.twofactor.service.TotpService
import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.core.exception.BusinessException
import com.hrplatform.core.exception.UnauthorizedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AuthDomainServiceTest : BehaviorSpec({

    val testEmailHash = "a".repeat(64)
    val testEmailHashService = mockk<EmailHashService>().also {
        every { it.hash(any()) } returns testEmailHash
    }

    fun buildActiveUserAccount(
        id: Long = 1L,
        email: String = "test@example.com",
        passwordHash: String = "hashed",
    ): UserAccount {
        val account = UserAccount.create(
            employmentId = 100L,
            companyId = 1L,
            email = email,
            emailHash = testEmailHash,
            passwordHash = passwordHash,
        )

        val field = UserAccount::class.java.superclass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(account, id)
        return account
    }

    fun buildAuthDomainService(
        userAccountRepository: UserAccountRepository = mockk(),
        refreshTokenRepository: RefreshTokenRepository = mockk(),
        loginAttemptRepository: LoginAttemptRepository = mockk(),
        passwordResetTokenRepository: PasswordResetTokenRepository = mockk(relaxed = true),
        emailHashService: EmailHashService = testEmailHashService,
        passwordEncoder: PasswordEncoder = mockk(),
        jwtTokenService: JwtTokenService = mockk(),
        totpService: TotpService = mockk(),
        jtiBlacklist: JtiBlacklist = mockk(),
        eventPublisher: DomainEventPublisher = mockk(relaxed = true),
    ): AuthDomainService = AuthDomainService(
        userAccountRepository = userAccountRepository,
        refreshTokenRepository = refreshTokenRepository,
        loginAttemptRepository = loginAttemptRepository,
        passwordResetTokenRepository = passwordResetTokenRepository,
        emailHashService = emailHashService,
        passwordEncoder = passwordEncoder,
        jwtTokenService = jwtTokenService,
        totpService = totpService,
        jtiBlacklist = jtiBlacklist,
        eventPublisher = eventPublisher,
    )

    given("로그인 — 정상 케이스") {
        val email = "user@example.com"
        val rawPassword = "password123"

        then("유효한 이메일/비밀번호로 로그인하면 LoginResult가 반환된다") {
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val userAccount = buildActiveUserAccount(email = email, passwordHash = "bcrypt-hashed")
            val tokenPair = TokenPair(
                accessToken = "access-token",
                refreshToken = "raw-refresh",
                refreshTokenHash = "hash",
                refreshTokenExpiresAt = now.plusDays(14),
                jti = "jti-uuid",
            )

            val userAccountRepository = mockk<UserAccountRepository>()
            val refreshTokenRepository = mockk<RefreshTokenRepository>()
            val loginAttemptRepository = mockk<LoginAttemptRepository>()
            val passwordEncoder = mockk<PasswordEncoder>()
            val jwtTokenService = mockk<JwtTokenService>()
            val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)
            val jtiBlacklist = mockk<JtiBlacklist>()

            every { userAccountRepository.findByEmailHash(testEmailHash) } returns userAccount
            every { passwordEncoder.matches(rawPassword, "bcrypt-hashed") } returns true
            every { jwtTokenService.issueTokenPair(1L, any()) } returns tokenPair
            every { refreshTokenRepository.save(any()) } answers { firstArg() }
            every { loginAttemptRepository.save(any()) } answers { firstArg() }
            every { userAccountRepository.save(any()) } answers { firstArg() }

            val service = buildAuthDomainService(
                userAccountRepository = userAccountRepository,
                refreshTokenRepository = refreshTokenRepository,
                loginAttemptRepository = loginAttemptRepository,
                passwordEncoder = passwordEncoder,
                jwtTokenService = jwtTokenService,
                jtiBlacklist = jtiBlacklist,
                eventPublisher = eventPublisher,
            )

            val result = service.login(email, rawPassword, null, null, null)

            result.userAccountId shouldBe 1L
            result.accessToken shouldBe "access-token"
            result.requiresTwoFactor shouldBe false
        }
    }

    given("로그인 — 이메일 없는 경우") {
        then("존재하지 않는 이메일로 로그인하면 UnauthorizedException이 발생한다") {
            val userAccountRepository = mockk<UserAccountRepository>()
            val loginAttemptRepository = mockk<LoginAttemptRepository>()

            every { userAccountRepository.findByEmailHash(any()) } returns null
            every { loginAttemptRepository.save(any()) } answers { firstArg() }

            val service = buildAuthDomainService(
                userAccountRepository = userAccountRepository,
                loginAttemptRepository = loginAttemptRepository,
            )

            shouldThrow<UnauthorizedException> {
                service.login("notfound@example.com", "pw", null, null, null)
            }
        }
    }

    given("로그인 — 비밀번호 불일치") {
        then("틀린 비밀번호로 로그인하면 UnauthorizedException이 발생하고 실패 횟수가 증가한다") {
            val email = "user@example.com"
            val userAccount = buildActiveUserAccount(email = email, passwordHash = "hashed")
            val userAccountRepository = mockk<UserAccountRepository>()
            val loginAttemptRepository = mockk<LoginAttemptRepository>()
            val passwordEncoder = mockk<PasswordEncoder>()
            val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)

            every { userAccountRepository.findByEmailHash(testEmailHash) } returns userAccount
            every { passwordEncoder.matches("wrong", "hashed") } returns false
            every { userAccountRepository.save(any()) } answers { firstArg() }
            every { loginAttemptRepository.save(any()) } answers { firstArg() }

            val service = buildAuthDomainService(
                userAccountRepository = userAccountRepository,
                loginAttemptRepository = loginAttemptRepository,
                passwordEncoder = passwordEncoder,
                eventPublisher = eventPublisher,
            )

            shouldThrow<UnauthorizedException> {
                service.login(email, "wrong", null, null, null)
            }

            userAccount.failedLoginAttempts shouldBe 1
        }
    }

    given("로그인 — 잠긴 계정 (자동 해제 가능)") {
        then("잠금 해제 시각이 지났으면 자동 해제 후 로그인 성공") {
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val email = "locked@example.com"
            val userAccount = buildActiveUserAccount(email = email, passwordHash = "hashed")

            val field = UserAccount::class.java.getDeclaredField("status")
            field.isAccessible = true
            field.set(userAccount, UserAccountStatus.LOCKED)
            val lockedField = UserAccount::class.java.getDeclaredField("lockedUntil")
            lockedField.isAccessible = true
            lockedField.set(userAccount, now.minusMinutes(1))

            val userAccountRepository = mockk<UserAccountRepository>()
            val loginAttemptRepository = mockk<LoginAttemptRepository>()
            val passwordEncoder = mockk<PasswordEncoder>()
            val refreshTokenRepository = mockk<RefreshTokenRepository>()
            val jwtTokenService = mockk<JwtTokenService>()
            val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)
            val jtiBlacklist = mockk<JtiBlacklist>()

            val tokenPair = TokenPair(
                accessToken = "access-token",
                refreshToken = "raw-refresh",
                refreshTokenHash = "hash",
                refreshTokenExpiresAt = now.plusDays(14),
                jti = "jti-uuid",
            )

            every { userAccountRepository.findByEmailHash(testEmailHash) } returns userAccount
            every { userAccountRepository.save(any()) } answers { firstArg() }
            every { passwordEncoder.matches(any(), "hashed") } returns true
            every { jwtTokenService.issueTokenPair(any(), any()) } returns tokenPair
            every { refreshTokenRepository.save(any()) } answers { firstArg() }
            every { loginAttemptRepository.save(any()) } answers { firstArg() }

            val service = buildAuthDomainService(
                userAccountRepository = userAccountRepository,
                refreshTokenRepository = refreshTokenRepository,
                loginAttemptRepository = loginAttemptRepository,
                passwordEncoder = passwordEncoder,
                jwtTokenService = jwtTokenService,
                jtiBlacklist = jtiBlacklist,
                eventPublisher = eventPublisher,
            )

            val result = service.login(email, "any", null, null, null)
            result.requiresTwoFactor shouldBe false
            userAccount.status shouldBe UserAccountStatus.ACTIVE
        }
    }

    given("로그인 — 2FA 활성화 계정") {
        then("2FA 활성화 계정은 requiresTwoFactor=true를 반환한다") {
            val email = "2fa@example.com"
            val userAccount = buildActiveUserAccount(email = email, passwordHash = "hashed")

            val field = UserAccount::class.java.getDeclaredField("twoFactorEnabled")
            field.isAccessible = true
            field.set(userAccount, true)

            val userAccountRepository = mockk<UserAccountRepository>()
            val loginAttemptRepository = mockk<LoginAttemptRepository>()
            val passwordEncoder = mockk<PasswordEncoder>()

            every { userAccountRepository.findByEmailHash(testEmailHash) } returns userAccount
            every { passwordEncoder.matches(any(), "hashed") } returns true

            val service = buildAuthDomainService(
                userAccountRepository = userAccountRepository,
                loginAttemptRepository = loginAttemptRepository,
                passwordEncoder = passwordEncoder,
            )

            val result = service.login(email, "password", null, null, null)
            result.requiresTwoFactor shouldBe true
            result.accessToken shouldBe ""
        }
    }

    given("logout") {
        then("유효한 refresh token으로 logout하면 token이 폐기되고 jti가 blacklist에 추가된다") {
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val rawToken = "raw-refresh-token"
            val tokenHash = "hash"
            val refreshToken = RefreshToken(
                userAccountId = 1L,
                tokenHash = tokenHash,
                accessJti = "test-jti",
                expiresAt = now.plusDays(14),
                deviceInfo = null,
                ipAddress = null,
                revokedAt = null,
                revokedReason = null,
            )

            val refreshTokenRepository = mockk<RefreshTokenRepository>()
            val jwtTokenService = mockk<JwtTokenService>()
            val jtiBlacklist = mockk<JtiBlacklist>()

            every { jwtTokenService.hashRefreshToken(rawToken) } returns tokenHash
            every { jwtTokenService.accessTokenExpirySeconds() } returns 1800L
            every { refreshTokenRepository.findByTokenHash(tokenHash) } returns refreshToken
            every { refreshTokenRepository.save(any()) } answers { firstArg() }
            every { jtiBlacklist.add(any(), any()) } returns Unit

            val service = buildAuthDomainService(
                refreshTokenRepository = refreshTokenRepository,
                jwtTokenService = jwtTokenService,
                jtiBlacklist = jtiBlacklist,
            )

            service.logout(rawToken)

            verify { refreshTokenRepository.save(any()) }
            verify { jtiBlacklist.add("test-jti", any()) }
            refreshToken.revokedAt shouldNotBe null
        }
    }

    given("requestPasswordReset — 존재하는 계정") {
        then("계정이 존재하면 rawToken을 반환하고 repository에 tokenHash를 저장한다") {
            val email = "user@example.com"
            val userAccount = buildActiveUserAccount(email = email)

            val userAccountRepository = mockk<UserAccountRepository>()
            val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>()

            every { userAccountRepository.findByEmailHash(testEmailHash) } returns userAccount
            justRun { passwordResetTokenRepository.save(any(), any(), any()) }

            val service = buildAuthDomainService(
                userAccountRepository = userAccountRepository,
                passwordResetTokenRepository = passwordResetTokenRepository,
            )

            val rawToken = service.requestPasswordReset(email)

            rawToken.isNotBlank() shouldBe true
            verify { passwordResetTokenRepository.save(any(), 1L, any()) }
        }
    }

    given("requestPasswordReset — 존재하지 않는 계정") {
        then("계정이 없어도 rawToken을 반환한다 (timing attack 방어)") {
            val userAccountRepository = mockk<UserAccountRepository>()
            val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>(relaxed = true)

            every { userAccountRepository.findByEmailHash(testEmailHash) } returns null

            val service = buildAuthDomainService(
                userAccountRepository = userAccountRepository,
                passwordResetTokenRepository = passwordResetTokenRepository,
            )

            val rawToken = service.requestPasswordReset("unknown@example.com")

            rawToken.isNotBlank() shouldBe true
            verify(exactly = 0) { passwordResetTokenRepository.save(any(), any(), any()) }
        }
    }

    given("confirmPasswordReset — 유효한 토큰") {
        then("유효한 rawToken으로 confirmPasswordReset 호출 시 비밀번호가 변경되고 세션이 무효화된다") {
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val userAccount = buildActiveUserAccount()

            val userAccountRepository = mockk<UserAccountRepository>()
            val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>()
            val passwordEncoder = mockk<PasswordEncoder>()
            val refreshTokenRepository = mockk<RefreshTokenRepository>()
            val jwtTokenService = mockk<JwtTokenService>()
            val jtiBlacklist = mockk<JtiBlacklist>(relaxed = true)

            every { passwordResetTokenRepository.findByHash(any()) } returns PasswordResetToken(
                tokenHash = "any-hash",
                userAccountId = 1L,
                expiresAt = Instant.now().plusSeconds(1800),
                usedAt = null,
            )
            justRun { passwordResetTokenRepository.markUsed(any()) }
            every { userAccountRepository.findById(1L) } returns userAccount
            every { userAccountRepository.save(any()) } answers { firstArg() }
            every { passwordEncoder.encode(any()) } returns "new-hash"
            every { refreshTokenRepository.findActiveByUserAccountId(1L) } returns emptyList()
            justRun { refreshTokenRepository.revokeAllByUserAccountId(any(), any(), any()) }
            every { jwtTokenService.accessTokenExpirySeconds() } returns 1800L

            val service = buildAuthDomainService(
                userAccountRepository = userAccountRepository,
                passwordResetTokenRepository = passwordResetTokenRepository,
                passwordEncoder = passwordEncoder,
                refreshTokenRepository = refreshTokenRepository,
                jwtTokenService = jwtTokenService,
                jtiBlacklist = jtiBlacklist,
            )

            service.confirmPasswordReset("valid-raw-token", "NewPassword123!")

            verify { passwordResetTokenRepository.markUsed(any()) }
            verify { refreshTokenRepository.revokeAllByUserAccountId(1L, "PASSWORD_RESET", any()) }
            userAccount.passwordHash shouldBe "new-hash"
        }
    }

    given("confirmPasswordReset — 유효하지 않은 토큰") {
        then("존재하지 않는 토큰으로 호출 시 BusinessException이 발생한다") {
            val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>()

            every { passwordResetTokenRepository.findByHash(any()) } returns null

            val service = buildAuthDomainService(
                passwordResetTokenRepository = passwordResetTokenRepository,
            )

            shouldThrow<BusinessException> {
                service.confirmPasswordReset("invalid-token", "NewPassword123!")
            }
        }
    }

    given("confirmPasswordReset — 만료된 토큰") {
        then("만료된 토큰으로 호출 시 BusinessException이 발생한다") {
            val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>()

            every { passwordResetTokenRepository.findByHash(any()) } returns PasswordResetToken(
                tokenHash = "any-hash",
                userAccountId = 1L,
                expiresAt = Instant.now().minusSeconds(1),
                usedAt = null,
            )

            val service = buildAuthDomainService(
                passwordResetTokenRepository = passwordResetTokenRepository,
            )

            shouldThrow<BusinessException> {
                service.confirmPasswordReset("expired-token", "NewPassword123!")
            }
        }
    }

    given("confirmPasswordReset — 이미 사용된 토큰") {
        then("이미 사용된 토큰으로 호출 시 BusinessException이 발생한다") {
            val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>()

            every { passwordResetTokenRepository.findByHash(any()) } returns PasswordResetToken(
                tokenHash = "any-hash",
                userAccountId = 1L,
                expiresAt = Instant.now().plusSeconds(1800),
                usedAt = Instant.now().minusSeconds(60),
            )

            val service = buildAuthDomainService(
                passwordResetTokenRepository = passwordResetTokenRepository,
            )

            shouldThrow<BusinessException> {
                service.confirmPasswordReset("used-token", "NewPassword123!")
            }
        }
    }
})
