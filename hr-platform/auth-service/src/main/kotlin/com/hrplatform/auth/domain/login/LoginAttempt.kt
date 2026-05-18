package com.hrplatform.auth.domain.login

import com.hrplatform.core.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

/**
 * 로그인 시도 이력 (append-only).
 * setter 노출 0. companion factory 메서드만 사용.
 */
@Entity
@Table(name = "login_attempts")
class LoginAttempt private constructor(
    @Column(name = "user_account_id")
    val userAccountId: Long?,

    @Column(nullable = false)
    val email: String,

    @Column(name = "attempted_at", nullable = false)
    val attemptedAt: ZonedDateTime,

    @Column(nullable = false)
    val success: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 100)
    val failureReason: LoginFailureReason?,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String?,

    @Column(name = "user_agent", length = 500)
    val userAgent: String?,
) : BaseEntity() {

    companion object {
        fun success(
            userAccountId: Long,
            email: String,
            ipAddress: String?,
            userAgent: String?,
            now: ZonedDateTime,
        ): LoginAttempt = LoginAttempt(
            userAccountId = userAccountId,
            email = email,
            attemptedAt = now,
            success = true,
            failureReason = null,
            ipAddress = ipAddress,
            userAgent = userAgent,
        )

        fun failure(
            userAccountId: Long?,
            email: String,
            failureReason: LoginFailureReason,
            ipAddress: String?,
            userAgent: String?,
            now: ZonedDateTime,
        ): LoginAttempt = LoginAttempt(
            userAccountId = userAccountId,
            email = email,
            attemptedAt = now,
            success = false,
            failureReason = failureReason,
            ipAddress = ipAddress,
            userAgent = userAgent,
        )
    }
}
