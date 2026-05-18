package com.hrplatform.auth.domain.twofactor.service

import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCode
import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCodeRepository
import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.core.exception.BusinessException
import com.hrplatform.core.exception.NotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

private const val BACKUP_CODE_COUNT = 5

data class TwoFactorEnrollmentResult(
    val qrCodeDataUri: String,
    val secret: String,
    val backupCodes: List<String>,
)

@Service
class TwoFactorDomainService(
    private val userAccountRepository: UserAccountRepository,
    private val backupCodeRepository: TwoFactorBackupCodeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val totpService: TotpService,
    private val eventPublisher: DomainEventPublisher,
) {

    fun enroll(userAccountId: Long, actorEmploymentId: Long?): TwoFactorEnrollmentResult {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findById(userAccountId)
            ?: throw NotFoundException(errorCode = "USER_ACCOUNT_NOT_FOUND", message = "UserAccount를 찾을 수 없습니다: $userAccountId")

        if (userAccount.status != UserAccountStatus.ACTIVE) {
            throw BusinessException(
                errorCode = "ACCOUNT_NOT_ACTIVE",
                message = "ACTIVE 상태에서만 2FA를 등록할 수 있습니다",
            )
        }

        val secret = totpService.generateSecret()
        val setup = totpService.buildQrCode("user-${userAccount.employmentId}", secret)

        val rawBackupCodes = (1..BACKUP_CODE_COUNT).map { UUID.randomUUID().toString().take(10) }
        val backupCodeEntities = rawBackupCodes.map { rawCode ->
            TwoFactorBackupCode(
                userAccountId = userAccountId,
                codeHash = passwordEncoder.encode(rawCode),
                usedAt = null,
            )
        }
        backupCodeRepository.deleteAllByUserAccountId(userAccountId, actorEmploymentId, now)
        backupCodeRepository.saveAll(backupCodeEntities)

        userAccount.enrollTwoFactor(setup.secret, actorEmploymentId, now)
        userAccountRepository.save(userAccount)
        eventPublisher.publishAll(userAccount.pullDomainEvents())

        return TwoFactorEnrollmentResult(
            qrCodeDataUri = setup.qrCodeDataUri,
            secret = secret,
            backupCodes = rawBackupCodes,
        )
    }

    fun verify(userAccountId: Long, otp: String): Boolean {
        val userAccount = userAccountRepository.findById(userAccountId)
            ?: throw NotFoundException(errorCode = "USER_ACCOUNT_NOT_FOUND", message = "UserAccount를 찾을 수 없습니다: $userAccountId")

        val encryptedSecret = userAccount.twoFactorSecret
            ?: throw BusinessException(errorCode = "2FA_NOT_ENROLLED", message = "2FA가 등록되지 않은 계정입니다")

        return totpService.verify(encryptedSecret, otp)
    }

    fun disable(userAccountId: Long, actorEmploymentId: Long?) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findById(userAccountId)
            ?: throw NotFoundException(errorCode = "USER_ACCOUNT_NOT_FOUND", message = "UserAccount를 찾을 수 없습니다: $userAccountId")

        userAccount.disableTwoFactor(actorEmploymentId, now)
        backupCodeRepository.deleteAllByUserAccountId(userAccountId, actorEmploymentId, now)
        userAccountRepository.save(userAccount)
        eventPublisher.publishAll(userAccount.pullDomainEvents())
    }

    fun useBackupCode(userAccountId: Long, rawCode: String): Boolean {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val unusedCodes = backupCodeRepository.findUnused(userAccountId)

        val matchingCode = unusedCodes.firstOrNull { passwordEncoder.matches(rawCode, it.codeHash) }
            ?: return false

        matchingCode.use(now)
        backupCodeRepository.save(matchingCode)
        return true
    }
}
