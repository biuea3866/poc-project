package com.hrplatform.auth.domain.auth.service

import com.hrplatform.auth.domain.account.EmailHashService
import com.hrplatform.auth.domain.account.PasswordResetTokenRepository
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
import com.hrplatform.core.exception.NotFoundException
import com.hrplatform.core.exception.UnauthorizedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Base64

/**
 * 인증 핵심 도메인 서비스.
 * @Transactional 없음 — UseCase(application 레이어)에서 선언.
 */
private val secureRandom = SecureRandom()
private val TOKEN_TTL = Duration.ofMinutes(30)

@Service
class AuthDomainService(
    private val userAccountRepository: UserAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val loginAttemptRepository: LoginAttemptRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
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
        val newPair = jwtTokenService.issueTokenPair(requireNotNull(userAccount.id), userAccount.employmentId, now)
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

    /**
     * 비밀번호 재설정 요청.
     * timing attack 방어: 계정 존재 여부와 무관하게 동일한 응답 반환(계정 열거 공격 차단).
     * rawToken을 반환하며, Controller가 notification-service에 이메일 발송을 위임해야 함.
     * TODO: 운영 Controller에서 rawToken을 응답 body에 포함하지 않고 notification-service에만 전달할 것.
     */
    fun requestPasswordReset(email: String): String {
        val emailHash = emailHashService.hash(email)
        val userAccount = userAccountRepository.findByEmailHash(emailHash)

        val rawToken = generateRawToken()
        val tokenHash = sha256Hex(rawToken)
        val expiresAt = Instant.now().plus(TOKEN_TTL)

        if (userAccount != null) {
            passwordResetTokenRepository.save(tokenHash, requireNotNull(userAccount.id), expiresAt)
        }

        return rawToken
    }

    /**
     * 비밀번호 재설정 확인.
     * rawToken을 sha256 해싱하여 저장소에서 검색, 만료/사용 여부 검증 후 비밀번호 변경.
     * 성공 시 모든 RefreshToken을 무효화하고 UserPasswordChangedEvent를 발행한다.
     */
    fun confirmPasswordReset(rawToken: String, newRawPassword: String) {
        val tokenHash = sha256Hex(rawToken)
        val resetToken = passwordResetTokenRepository.findByHash(tokenHash)
            ?: throw BusinessException(errorCode = "INVALID_RESET_TOKEN", message = "유효하지 않거나 만료된 비밀번호 재설정 토큰입니다")

        if (resetToken.usedAt != null) {
            throw BusinessException(errorCode = "INVALID_RESET_TOKEN", message = "이미 사용된 비밀번호 재설정 토큰입니다")
        }
        if (Instant.now().isAfter(resetToken.expiresAt)) {
            throw BusinessException(errorCode = "INVALID_RESET_TOKEN", message = "만료된 비밀번호 재설정 토큰입니다")
        }

        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findById(resetToken.userAccountId)
            ?: throw BusinessException(errorCode = "ACCOUNT_NOT_FOUND", message = "계정을 찾을 수 없습니다")

        PasswordPolicy.validate(newRawPassword)
        val newHash = passwordEncoder.encode(newRawPassword)
        userAccount.changePassword(newHash, "PASSWORD_RESET", null, now)
        userAccountRepository.save(userAccount)

        passwordResetTokenRepository.markUsed(tokenHash)
        revokeAllSessions(resetToken.userAccountId, "PASSWORD_RESET")
        eventPublisher.publishAll(userAccount.pullDomainEvents())
    }

    fun getMe(userAccountId: Long): UserAccount {
        return userAccountRepository.findById(userAccountId)
            ?: throw NotFoundException(
                errorCode = "USER_ACCOUNT_NOT_FOUND",
                message = "UserAccount를 찾을 수 없습니다: $userAccountId",
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

        val tokenPair = jwtTokenService.issueTokenPair(requireNotNull(userAccount.id), userAccount.employmentId, now)
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

    private fun generateRawToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
