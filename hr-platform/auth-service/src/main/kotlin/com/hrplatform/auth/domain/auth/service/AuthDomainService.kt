package com.hrplatform.auth.domain.auth.service

import com.hrplatform.auth.domain.account.EmailHashService
import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.login.LoginAttempt
import com.hrplatform.auth.domain.login.LoginAttemptRepository
import com.hrplatform.auth.domain.login.LoginFailureReason
import com.hrplatform.auth.domain.token.JtiBlacklist
import com.hrplatform.auth.domain.token.RefreshToken
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.auth.domain.twofactor.service.TotpService
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
    private val emailHashService: EmailHashService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService,
    private val totpService: TotpService,
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
        val emailHash = emailHashService.hash(email)
        val userAccount = userAccountRepository.findByEmailHash(emailHash)

        if (userAccount == null) {
            loginAttemptRepository.save(
                LoginAttempt.failure(null, emailHash, LoginFailureReason.EMAIL_NOT_FOUND, ipAddress, userAgent, now),
            )
            throw UnauthorizedException(errorCode = "UNAUTHORIZED", message = "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        validateAccountAccessibleBeforePassword(userAccount, now)

        if (!passwordEncoder.matches(rawPassword, userAccount.passwordHash)) {
            userAccount.recordFailedAttempt(now)
            userAccountRepository.save(userAccount)
            loginAttemptRepository.save(
                LoginAttempt.failure(userAccount.id, emailHash, LoginFailureReason.BAD_PASSWORD, ipAddress, userAgent, now),
            )
            eventPublisher.publishAll(userAccount.pullDomainEvents())
            throw UnauthorizedException(errorCode = "UNAUTHORIZED", message = "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        unlockIfExpired(userAccount, now)

        if (userAccount.twoFactorEnabled) {
            return LoginResult(
                userAccountId = requireNotNull(userAccount.id),
                accessToken = "",
                refreshToken = "",
                expiresAt = now,
                requiresTwoFactor = true,
            )
        }

        return completeLogin(userAccount, emailHash, deviceInfo, ipAddress, userAgent, now)
    }

    fun loginWithOtp(email: String, rawPassword: String, otp: String): LoginResult {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val emailHash = emailHashService.hash(email)
        val userAccount = userAccountRepository.findByEmailHash(emailHash)
            ?: throw UnauthorizedException(errorCode = "UNAUTHORIZED", message = "이메일 또는 비밀번호가 올바르지 않습니다")

        validateAccountAccessibleBeforePassword(userAccount, now)
        verifyPasswordOrThrow(rawPassword, userAccount.passwordHash)
        verifyOtpOrThrow(userAccount, otp)

        unlockIfExpired(userAccount, now)
        return completeLogin(userAccount, emailHash, null, null, null, now)
    }

    fun refresh(rawRefreshToken: String): TokenPair {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val tokenHash = jwtTokenService.hashRefreshToken(rawRefreshToken)
        val refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw UnauthorizedException(errorCode = "INVALID_REFRESH_TOKEN", message = "유효하지 않은 Refresh Token입니다")

        if (!refreshToken.isValid(now)) {
            throw UnauthorizedException(errorCode = "EXPIRED_REFRESH_TOKEN", message = "만료되거나 폐기된 Refresh Token입니다")
        }

        val userAccount = findAccountForRefreshOrThrow(refreshToken.userAccountId)
        val newPair = jwtTokenService.issueTokenPair(requireNotNull(userAccount.id), now)
        refreshToken.rotate(newPair.refreshTokenHash, newPair.jti)
        refreshTokenRepository.save(refreshToken)

        return newPair
    }

    fun logout(rawRefreshToken: String) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val tokenHash = jwtTokenService.hashRefreshToken(rawRefreshToken)
        val refreshToken = refreshTokenRepository.findByTokenHash(tokenHash) ?: return
        if (refreshToken.revokedAt == null) {
            val jti = refreshToken.accessJti
            if (jti != null) {
                jtiBlacklist.add(jti, Duration.ofMinutes(jwtTokenService.accessTokenExpirySeconds() / 60 + 5))
            }
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

        PasswordPolicy.validate(newRawPassword)
        val newHash = passwordEncoder.encode(newRawPassword)
        userAccount.changePassword(newHash, "SELF_CHANGE", actorEmploymentId, now)
        userAccountRepository.save(userAccount)
        eventPublisher.publishAll(userAccount.pullDomainEvents())
    }

    fun requestPasswordReset(email: String): String {
        val emailHash = emailHashService.hash(email)
        // timing attack 방어: 계정 존재 여부 무관하게 동일 응답 반환.
        // 조회 결과를 활용하지 않고 항상 새 토큰을 반환하여 계정 열거(account enumeration) 공격 차단.
        // 실제 이메일 발송은 notification-service가 담당 (별도 구현 예정).
        userAccountRepository.findByEmailHash(emailHash)
        return UUID.randomUUID().toString()
    }

    fun confirmPasswordReset(resetToken: String, newRawPassword: String) {
        throw BusinessException(
            errorCode = "NOT_IMPLEMENTED",
            message = "비밀번호 재설정 토큰(${resetToken.take(8)}...) 검증은 별도 토큰 저장소 구현 필요. 새 비밀번호 길이: ${newRawPassword.length}",
        )
    }

    fun revokeAllSessions(userAccountId: Long, reason: String) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val activeTokens = refreshTokenRepository.findActiveByUserAccountId(userAccountId)
        val jtis = activeTokens.mapNotNull { it.accessJti }
        if (jtis.isNotEmpty()) {
            jtiBlacklist.addAll(jtis, Duration.ofMinutes(jwtTokenService.accessTokenExpirySeconds() / 60 + 5))
        }
        refreshTokenRepository.revokeAllByUserAccountId(userAccountId, reason, now)
    }

    private fun completeLogin(
        userAccount: UserAccount,
        emailHash: String,
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
            accessJti = tokenPair.jti,
            expiresAt = tokenPair.refreshTokenExpiresAt,
            deviceInfo = deviceInfo,
            ipAddress = ipAddress,
            revokedAt = null,
            revokedReason = null,
        )
        refreshTokenRepository.save(refreshTokenEntity)

        loginAttemptRepository.save(
            LoginAttempt.success(requireNotNull(userAccount.id), emailHash, ipAddress, userAgent, now),
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

    /**
     * password 검증 이전: DEACTIVATED, SUSPENDED, LOCKED(아직 잠금 중) 상태 차단.
     * LOCKED이지만 잠금 해제 시각이 지난 경우는 password 검증 이후 unlock 처리.
     */
    private fun validateAccountAccessibleBeforePassword(userAccount: UserAccount, now: ZonedDateTime) {
        when (userAccount.status) {
            UserAccountStatus.DEACTIVATED ->
                throw UnauthorizedException(errorCode = "ACCOUNT_DEACTIVATED", message = "비활성화된 계정입니다")
            UserAccountStatus.SUSPENDED -> throwSuspendedAndRecord(userAccount, now)
            UserAccountStatus.LOCKED -> throwLockedAndRecordIfStillLocked(userAccount, now)
            UserAccountStatus.ACTIVE -> Unit
        }
    }

    private fun throwSuspendedAndRecord(userAccount: UserAccount, now: ZonedDateTime) {
        // emailHash가 null인 경우(backfill 미완료) 빈 문자열로 기록. 로그인 불가 상태이므로 영향 없음.
        val emailHash = userAccount.emailHash ?: ""
        loginAttemptRepository.save(
            LoginAttempt.failure(
                userAccount.id, emailHash,
                LoginFailureReason.ACCOUNT_SUSPENDED, null, null, now,
            ),
        )
        throw UnauthorizedException(errorCode = "ACCOUNT_SUSPENDED", message = "정지된 계정입니다")
    }

    private fun throwLockedAndRecordIfStillLocked(userAccount: UserAccount, now: ZonedDateTime) {
        val lockUntil = userAccount.lockedUntil
        if (lockUntil == null || lockUntil.isAfter(now)) {
            val emailHash = userAccount.emailHash ?: ""
            loginAttemptRepository.save(
                LoginAttempt.failure(
                    userAccount.id, emailHash,
                    LoginFailureReason.ACCOUNT_LOCKED, null, null, now,
                ),
            )
            throw UnauthorizedException(
                errorCode = "ACCOUNT_LOCKED",
                message = "잠긴 계정입니다. 잠금 해제 시각: ${userAccount.lockedUntil}",
            )
        }
        // 잠금 시각이 지났으면 password 검증 이후 unlock 처리
    }

    /**
     * password 검증 이후: LOCKED이지만 잠금 해제 시각이 지난 경우 unlock 처리.
     */
    private fun unlockIfExpired(userAccount: UserAccount, now: ZonedDateTime) {
        if (userAccount.status == UserAccountStatus.LOCKED) {
            userAccount.tryAutoUnlock(now)
            userAccountRepository.save(userAccount)
            eventPublisher.publishAll(userAccount.pullDomainEvents())
        }
    }

    private fun verifyPasswordOrThrow(rawPassword: String, passwordHash: String) {
        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            throw UnauthorizedException(errorCode = "UNAUTHORIZED", message = "이메일 또는 비밀번호가 올바르지 않습니다")
        }
    }

    private fun verifyOtpOrThrow(userAccount: UserAccount, otp: String) {
        val encryptedSecret = userAccount.twoFactorSecret
            ?: throw UnauthorizedException(errorCode = "2FA_NOT_ENROLLED", message = "2FA가 등록되지 않은 계정입니다")
        if (!totpService.verify(encryptedSecret, otp)) {
            throw UnauthorizedException(errorCode = "INVALID_OTP", message = "OTP가 올바르지 않습니다")
        }
    }

    private fun findAccountForRefreshOrThrow(userAccountId: Long): UserAccount {
        val userAccount = userAccountRepository.findById(userAccountId)
            ?: throw UnauthorizedException(errorCode = "ACCOUNT_NOT_FOUND", message = "계정을 찾을 수 없습니다")
        if (userAccount.status == UserAccountStatus.DEACTIVATED) {
            throw UnauthorizedException(errorCode = "ACCOUNT_DEACTIVATED", message = "비활성화된 계정입니다")
        }
        return userAccount
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
