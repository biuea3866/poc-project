package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.login.LoginAttempt
import com.hrplatform.auth.domain.login.LoginAttemptRepository
import com.hrplatform.auth.domain.login.LoginFailureReason
import com.hrplatform.auth.domain.token.JtiBlacklist
import com.hrplatform.auth.domain.token.RefreshToken
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.core.exception.BusinessException
import com.hrplatform.core.exception.UnauthorizedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

/**
 * 인증 핵심 도메인 서비스.
 * @Transactional 없음 — UseCase(application 레이어)에서 선언.
 */
@Service
class AuthDomainService(
    private val userAccountRepository: UserAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val loginAttemptRepository: LoginAttemptRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService,
    private val jtiBlacklist: JtiBlacklist,
    private val eventPublisher: DomainEventPublisher,
) {

    fun login(
        email: String,
        rawPassword: String,
        deviceInfo: String?,
        ipAddress: String?,
        userAgent: String?,
    ): LoginResult {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findByEmail(email)

        if (userAccount == null) {
            loginAttemptRepository.save(
                LoginAttempt.failure(null, email, LoginFailureReason.EMAIL_NOT_FOUND, ipAddress, userAgent, now),
            )
            throw UnauthorizedException(errorCode = "UNAUTHORIZED", message = "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        validateAccountAccessible(userAccount, now)

        if (!passwordEncoder.matches(rawPassword, userAccount.passwordHash)) {
            userAccount.recordFailedAttempt(now)
            userAccountRepository.save(userAccount)
            loginAttemptRepository.save(
                LoginAttempt.failure(userAccount.id, email, LoginFailureReason.BAD_PASSWORD, ipAddress, userAgent, now),
            )
            eventPublisher.publishAll(userAccount.pullDomainEvents())
            throw UnauthorizedException(errorCode = "UNAUTHORIZED", message = "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        if (userAccount.twoFactorEnabled) {
            return LoginResult(
                userAccountId = requireNotNull(userAccount.id),
                accessToken = "",
                refreshToken = "",
                expiresAt = now,
                requiresTwoFactor = true,
            )
        }

        return completeLogin(userAccount, deviceInfo, ipAddress, userAgent, now)
    }

    fun loginWithOtp(email: String, rawPassword: String, otp: String): LoginResult {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findByEmail(email)
            ?: throw UnauthorizedException(errorCode = "UNAUTHORIZED", message = "이메일 또는 비밀번호가 올바르지 않습니다")

        validateAccountAccessible(userAccount, now)

        if (!passwordEncoder.matches(rawPassword, userAccount.passwordHash)) {
            throw UnauthorizedException(errorCode = "UNAUTHORIZED", message = "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        return completeLogin(userAccount, null, null, null, now)
    }

    fun refresh(rawRefreshToken: String): TokenPair {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val tokenHash = jwtTokenService.hashRefreshToken(rawRefreshToken)
        val refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw UnauthorizedException(errorCode = "INVALID_REFRESH_TOKEN", message = "유효하지 않은 Refresh Token입니다")

        if (!refreshToken.isValid(now)) {
            throw UnauthorizedException(errorCode = "EXPIRED_REFRESH_TOKEN", message = "만료되거나 폐기된 Refresh Token입니다")
        }

        val userAccount = userAccountRepository.findById(refreshToken.userAccountId)
            ?: throw UnauthorizedException(errorCode = "ACCOUNT_NOT_FOUND", message = "계정을 찾을 수 없습니다")

        if (userAccount.status == UserAccountStatus.DEACTIVATED) {
            throw UnauthorizedException(errorCode = "ACCOUNT_DEACTIVATED", message = "비활성화된 계정입니다")
        }

        val newPair = jwtTokenService.issueTokenPair(requireNotNull(userAccount.id), now)
        refreshToken.rotate(newPair.refreshTokenHash)
        refreshTokenRepository.save(refreshToken)

        return newPair
    }

    fun logout(rawRefreshToken: String) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val tokenHash = jwtTokenService.hashRefreshToken(rawRefreshToken)
        val refreshToken = refreshTokenRepository.findByTokenHash(tokenHash) ?: return
        if (refreshToken.revokedAt == null) {
            refreshToken.revoke("LOGOUT", now)
            refreshTokenRepository.save(refreshToken)
        }
    }

    fun changePassword(
        userAccountId: Long,
        oldRawPassword: String,
        newRawPassword: String,
        actorEmploymentId: Long?,
    ) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = findActiveOrThrow(userAccountId)

        if (!passwordEncoder.matches(oldRawPassword, userAccount.passwordHash)) {
            throw UnauthorizedException(errorCode = "WRONG_PASSWORD", message = "현재 비밀번호가 올바르지 않습니다")
        }

        val newHash = passwordEncoder.encode(newRawPassword)
        userAccount.changePassword(newHash, "SELF_CHANGE", actorEmploymentId, now)
        userAccountRepository.save(userAccount)
        eventPublisher.publishAll(userAccount.pullDomainEvents())
    }

    fun requestPasswordReset(email: String): String {
        userAccountRepository.findByEmail(email) ?: return UUID.randomUUID().toString()
        return UUID.randomUUID().toString()
    }

    fun confirmPasswordReset(token: String, newRawPassword: String) {
        throw BusinessException(
            errorCode = "NOT_IMPLEMENTED",
            message = "비밀번호 재설정 토큰 검증은 별도 토큰 저장소 구현 필요",
        )
    }

    fun revokeAllSessions(userAccountId: Long, reason: String) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val activeTokens = refreshTokenRepository.findActiveByUserAccountId(userAccountId)
        val jtis = activeTokens.mapNotNull { it.revokedReason }
        if (jtis.isNotEmpty()) {
            jtiBlacklist.addAll(jtis, Duration.ofDays(15))
        }
        refreshTokenRepository.revokeAllByUserAccountId(userAccountId, reason, now)
    }

    private fun completeLogin(
        userAccount: UserAccount,
        deviceInfo: String?,
        ipAddress: String?,
        userAgent: String?,
        now: ZonedDateTime,
    ): LoginResult {
        userAccount.recordSuccessfulLogin(now)
        userAccountRepository.save(userAccount)

        val tokenPair = jwtTokenService.issueTokenPair(requireNotNull(userAccount.id), now)
        val refreshTokenEntity = RefreshToken(
            userAccountId = requireNotNull(userAccount.id),
            tokenHash = tokenPair.refreshTokenHash,
            expiresAt = tokenPair.refreshTokenExpiresAt,
            deviceInfo = deviceInfo,
            ipAddress = ipAddress,
            revokedAt = null,
            revokedReason = null,
        )
        refreshTokenRepository.save(refreshTokenEntity)

        loginAttemptRepository.save(
            LoginAttempt.success(requireNotNull(userAccount.id), userAccount.email, ipAddress, userAgent, now),
        )

        eventPublisher.publishAll(userAccount.pullDomainEvents())

        return LoginResult(
            userAccountId = requireNotNull(userAccount.id),
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            expiresAt = tokenPair.refreshTokenExpiresAt,
            requiresTwoFactor = false,
        )
    }

    private fun validateAccountAccessible(userAccount: UserAccount, now: ZonedDateTime) {
        when (userAccount.status) {
            UserAccountStatus.DEACTIVATED ->
                throw UnauthorizedException(errorCode = "ACCOUNT_DEACTIVATED", message = "비활성화된 계정입니다")
            UserAccountStatus.SUSPENDED -> {
                loginAttemptRepository.save(
                    LoginAttempt.failure(
                        userAccount.id, userAccount.email,
                        LoginFailureReason.ACCOUNT_SUSPENDED, null, null, now,
                    ),
                )
                throw UnauthorizedException(errorCode = "ACCOUNT_SUSPENDED", message = "정지된 계정입니다")
            }
            UserAccountStatus.LOCKED -> {
                val autoUnlocked = userAccount.tryAutoUnlock(now)
                if (!autoUnlocked) {
                    loginAttemptRepository.save(
                        LoginAttempt.failure(
                            userAccount.id, userAccount.email,
                            LoginFailureReason.ACCOUNT_LOCKED, null, null, now,
                        ),
                    )
                    throw UnauthorizedException(
                        errorCode = "ACCOUNT_LOCKED",
                        message = "잠긴 계정입니다. 잠금 해제 시각: ${userAccount.lockedUntil}",
                    )
                }
                userAccountRepository.save(userAccount)
                eventPublisher.publishAll(userAccount.pullDomainEvents())
            }
            UserAccountStatus.ACTIVE -> Unit
        }
    }

    private fun findActiveOrThrow(userAccountId: Long): UserAccount {
        val userAccount = userAccountRepository.findById(userAccountId)
            ?: throw UnauthorizedException(errorCode = "ACCOUNT_NOT_FOUND", message = "계정을 찾을 수 없습니다")
        if (userAccount.status != UserAccountStatus.ACTIVE) {
            throw UnauthorizedException(
                errorCode = "ACCOUNT_NOT_ACTIVE",
                message = "활성 계정만 이 작업을 수행할 수 있습니다. 현재 상태: ${userAccount.status}",
            )
        }
        return userAccount
    }
}
