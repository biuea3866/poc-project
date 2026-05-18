package com.hrplatform.auth.domain.account

import com.hrplatform.auth.domain.account.event.UserDeactivatedEvent
import com.hrplatform.auth.domain.account.event.UserLockedEvent
import com.hrplatform.auth.domain.account.event.UserPasswordChangedEvent
import com.hrplatform.auth.domain.account.event.UserReactivatedEvent
import com.hrplatform.auth.domain.account.event.UserSuspendedEvent
import com.hrplatform.auth.domain.account.event.UserTwoFactorDisabledEvent
import com.hrplatform.auth.domain.account.event.UserTwoFactorEnrolledEvent
import com.hrplatform.auth.domain.account.event.UserUnlockedEvent
import com.hrplatform.core.domain.AggregateRoot
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "user_accounts",
    uniqueConstraints = [UniqueConstraint(columnNames = ["email"])],
)
class UserAccount(
    @Column(name = "employment_id", nullable = false)
    val employmentId: Long,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserAccountStatus,

    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int,

    @Column(name = "locked_until")
    var lockedUntil: ZonedDateTime?,

    @Column(name = "last_login_at")
    var lastLoginAt: ZonedDateTime?,

    @Column(name = "two_factor_enabled", nullable = false)
    var twoFactorEnabled: Boolean,

    @Convert(converter = com.hrplatform.auth.infrastructure.crypto.AesGcmStringConverter::class)
    @Column(name = "two_factor_secret")
    var twoFactorSecret: String?,
) : AggregateRoot() {

    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCK_DURATION_MINUTES = 15L

        fun create(
            employmentId: Long,
            companyId: Long,
            email: String,
            passwordHash: String,
        ): UserAccount = UserAccount(
            employmentId = employmentId,
            companyId = companyId,
            email = email,
            passwordHash = passwordHash,
            status = UserAccountStatus.ACTIVE,
            failedLoginAttempts = 0,
            lockedUntil = null,
            lastLoginAt = null,
            twoFactorEnabled = false,
            twoFactorSecret = null,
        )
    }

    fun recordSuccessfulLogin(now: ZonedDateTime) {
        check(status == UserAccountStatus.ACTIVE) {
            "ACTIVE 상태에서만 로그인 성공 처리가 가능합니다. 현재 상태: $status"
        }
        failedLoginAttempts = 0
        lastLoginAt = now
    }

    fun recordFailedAttempt(now: ZonedDateTime) {
        check(status != UserAccountStatus.DEACTIVATED) {
            "비활성화된 계정입니다"
        }
        failedLoginAttempts += 1
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            val until = now.plusMinutes(LOCK_DURATION_MINUTES)
            lock(until, now)
        }
    }

    fun lock(until: ZonedDateTime, now: ZonedDateTime) {
        requireTransition(UserAccountStatus.LOCKED)
        status = UserAccountStatus.LOCKED
        lockedUntil = until
        addDomainEvent(
            UserLockedEvent(
                userAccountId = id ?: 0L,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                failedAttempts = failedLoginAttempts,
                lockedUntil = until,
                twoFactorEnabled = twoFactorEnabled,
                actorEmploymentId = null,
                occurredAt = now,
            ),
        )
    }

    /**
     * 잠금 해제 (수동). 관리자 호출.
     * @return UserUnlockedEvent가 적재됨
     */
    fun unlock(actorEmploymentId: Long?, now: ZonedDateTime) {
        requireTransition(UserAccountStatus.ACTIVE)
        status = UserAccountStatus.ACTIVE
        failedLoginAttempts = 0
        lockedUntil = null
        addDomainEvent(
            UserUnlockedEvent(
                userAccountId = id ?: 0L,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                twoFactorEnabled = twoFactorEnabled,
                trigger = "MANUAL",
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    /**
     * 잠금 만료 자동 해제 시도.
     * @return true if 자동 해제됨, false if 아직 잠금 중 또는 LOCKED 아님
     */
    fun tryAutoUnlock(now: ZonedDateTime): Boolean {
        if (status != UserAccountStatus.LOCKED) return false
        val until = lockedUntil ?: return false
        if (until.isAfter(now)) return false

        status = UserAccountStatus.ACTIVE
        failedLoginAttempts = 0
        lockedUntil = null
        addDomainEvent(
            UserUnlockedEvent(
                userAccountId = id ?: 0L,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                twoFactorEnabled = twoFactorEnabled,
                trigger = "AUTO",
                actorEmploymentId = null,
                occurredAt = now,
            ),
        )
        return true
    }

    fun suspend(reason: String, actorEmploymentId: Long?, now: ZonedDateTime) {
        requireTransition(UserAccountStatus.SUSPENDED)
        status = UserAccountStatus.SUSPENDED
        addDomainEvent(
            UserSuspendedEvent(
                userAccountId = id ?: 0L,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                twoFactorEnabled = twoFactorEnabled,
                reason = reason,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun reactivate(actorEmploymentId: Long?, now: ZonedDateTime) {
        requireTransition(UserAccountStatus.ACTIVE)
        status = UserAccountStatus.ACTIVE
        addDomainEvent(
            UserReactivatedEvent(
                userAccountId = id ?: 0L,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                twoFactorEnabled = twoFactorEnabled,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun deactivate(reason: String, actorEmploymentId: Long?, now: ZonedDateTime) {
        requireTransition(UserAccountStatus.DEACTIVATED)
        status = UserAccountStatus.DEACTIVATED
        addDomainEvent(
            UserDeactivatedEvent(
                userAccountId = id ?: 0L,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                twoFactorEnabled = twoFactorEnabled,
                reason = reason,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun changePassword(newPasswordHash: String, trigger: String, actorEmploymentId: Long?, now: ZonedDateTime) {
        check(status == UserAccountStatus.ACTIVE) {
            "ACTIVE 상태에서만 비밀번호를 변경할 수 있습니다. 현재 상태: $status"
        }
        passwordHash = newPasswordHash
        addDomainEvent(
            UserPasswordChangedEvent(
                userAccountId = id ?: 0L,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                twoFactorEnabled = twoFactorEnabled,
                trigger = trigger,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun enrollTwoFactor(secretEncrypted: String, actorEmploymentId: Long?, now: ZonedDateTime) {
        check(status == UserAccountStatus.ACTIVE) {
            "ACTIVE 상태에서만 2FA를 등록할 수 있습니다. 현재 상태: $status"
        }
        twoFactorSecret = secretEncrypted
        twoFactorEnabled = true
        addDomainEvent(
            UserTwoFactorEnrolledEvent(
                userAccountId = id ?: 0L,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun disableTwoFactor(actorEmploymentId: Long?, now: ZonedDateTime) {
        check(twoFactorEnabled) { "2FA가 활성화되어 있지 않습니다" }
        twoFactorSecret = null
        twoFactorEnabled = false
        addDomainEvent(
            UserTwoFactorDisabledEvent(
                userAccountId = id ?: 0L,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    private fun requireTransition(target: UserAccountStatus) {
        if (!status.canTransitTo(target)) {
            throw IllegalStateTransitionException(status, target)
        }
    }
}
