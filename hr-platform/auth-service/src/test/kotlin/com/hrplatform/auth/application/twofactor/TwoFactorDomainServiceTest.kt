package com.hrplatform.auth.application.twofactor

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCode
import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCodeRepository
import com.hrplatform.auth.infrastructure.crypto.AesGcmStringConverter
import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.core.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.ZonedDateTime

class TwoFactorDomainServiceTest : BehaviorSpec({

    fun buildActiveUserAccount(id: Long = 1L): UserAccount {
        val account = UserAccount.create(
            employmentId = 100L,
            companyId = 1L,
            email = "test@example.com",
            passwordHash = "hash",
        )
        val idField = UserAccount::class.java.superclass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(account, id)
        return account
    }

    given("enroll — 정상 케이스") {
        then("ACTIVE 계정에 enroll 호출 시 QR코드, secret, 백업 코드 5개가 반환된다") {
            val userAccount = buildActiveUserAccount()
            val userAccountRepository = mockk<UserAccountRepository>()
            val backupCodeRepository = mockk<TwoFactorBackupCodeRepository>()
            val passwordEncoder = mockk<PasswordEncoder>()
            val totpService = mockk<TotpService>()
            val aesGcmStringConverter = mockk<AesGcmStringConverter>()
            val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)

            every { userAccountRepository.findById(1L) } returns userAccount
            every { totpService.generateSecret() } returns "JBSWY3DPEHPK3PXP"
            every { totpService.buildQrCode(any(), any()) } returns TotpSetup("JBSWY3DPEHPK3PXP", "data:image/png;base64,...")
            every { passwordEncoder.encode(any()) } returns "hashed-code"
            every { backupCodeRepository.deleteAllByUserAccountId(1L, any(), any()) } returns Unit
            every { backupCodeRepository.saveAll(any()) } answers { firstArg() }
            every { userAccountRepository.save(any()) } answers { firstArg() }

            val service = TwoFactorDomainService(
                userAccountRepository, backupCodeRepository, passwordEncoder,
                totpService, aesGcmStringConverter, eventPublisher,
            )

            val result = service.enroll(1L, null)

            result.backupCodes.size shouldBe 5
            result.secret shouldBe "JBSWY3DPEHPK3PXP"
            result.qrCodeDataUri.isNotEmpty() shouldBe true
            verify { eventPublisher.publishAll(any()) }
        }
    }

    given("verify — 올바른 OTP") {
        then("올바른 OTP로 verify 호출 시 true 반환") {
            val userAccount = buildActiveUserAccount()
            val secretField = UserAccount::class.java.getDeclaredField("twoFactorSecret")
            secretField.isAccessible = true
            secretField.set(userAccount, "SECRET")

            val userAccountRepository = mockk<UserAccountRepository>()
            val totpService = mockk<TotpService>()

            every { userAccountRepository.findById(1L) } returns userAccount
            every { totpService.verify("SECRET", "123456") } returns true

            val service = TwoFactorDomainService(
                userAccountRepository, mockk(), mockk(), totpService, mockk(), mockk(relaxed = true),
            )

            service.verify(1L, "123456") shouldBe true
        }
    }

    given("useBackupCode — 일치하는 코드") {
        then("미사용 백업 코드로 useBackupCode 호출 시 true 반환 후 코드가 사용 처리된다") {
            val backupCode = TwoFactorBackupCode(
                userAccountId = 1L,
                codeHash = "hashed",
                usedAt = null,
            )
            val backupCodeRepository = mockk<TwoFactorBackupCodeRepository>()
            val passwordEncoder = mockk<PasswordEncoder>()

            every { backupCodeRepository.findUnused(1L) } returns listOf(backupCode)
            every { passwordEncoder.matches("raw-code", "hashed") } returns true
            every { backupCodeRepository.save(any()) } answers { firstArg() }

            val service = TwoFactorDomainService(
                mockk(), backupCodeRepository, passwordEncoder, mockk(), mockk(), mockk(relaxed = true),
            )

            val result = service.useBackupCode(1L, "raw-code")
            result shouldBe true
            backupCode.isUsed() shouldBe true
        }
    }

    given("useBackupCode — 일치하는 코드 없음") {
        then("일치하는 백업 코드가 없으면 false 반환") {
            val backupCodeRepository = mockk<TwoFactorBackupCodeRepository>()
            val passwordEncoder = mockk<PasswordEncoder>()

            every { backupCodeRepository.findUnused(1L) } returns emptyList()

            val service = TwoFactorDomainService(
                mockk(), backupCodeRepository, passwordEncoder, mockk(), mockk(), mockk(relaxed = true),
            )

            service.useBackupCode(1L, "raw-code") shouldBe false
        }
    }

    given("disable — 2FA 해제") {
        then("2FA 활성화된 계정에 disable 호출 시 twoFactorEnabled=false가 된다") {
            val userAccount = buildActiveUserAccount()
            val twoFactorEnabledField = UserAccount::class.java.getDeclaredField("twoFactorEnabled")
            twoFactorEnabledField.isAccessible = true
            twoFactorEnabledField.set(userAccount, true)
            val twoFactorSecretField = UserAccount::class.java.getDeclaredField("twoFactorSecret")
            twoFactorSecretField.isAccessible = true
            twoFactorSecretField.set(userAccount, "SECRET")

            val userAccountRepository = mockk<UserAccountRepository>()
            val backupCodeRepository = mockk<TwoFactorBackupCodeRepository>()
            val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)

            every { userAccountRepository.findById(1L) } returns userAccount
            every { backupCodeRepository.deleteAllByUserAccountId(1L, any(), any()) } returns Unit
            every { userAccountRepository.save(any()) } answers { firstArg() }

            val service = TwoFactorDomainService(
                userAccountRepository, backupCodeRepository, mockk(), mockk(), mockk(), eventPublisher,
            )

            service.disable(1L, null)

            userAccount.twoFactorEnabled shouldBe false
            verify { eventPublisher.publishAll(any()) }
        }
    }
})
