package com.hrplatform.auth.domain.token

import com.hrplatform.core.domain.BaseEntity
import io.hypersistence.utils.hibernate.type.json.JsonStringType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.ZonedDateTime

@Entity
@Table(name = "api_tokens")
class ApiToken(
    @Column(name = "user_account_id", nullable = false)
    val userAccountId: Long,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(name = "token_hash", nullable = false, unique = true)
    val tokenHash: String,

    @Type(JsonStringType::class)
    @Column(columnDefinition = "TEXT")
    val scopes: List<String>,

    @Column(name = "expires_at")
    val expiresAt: ZonedDateTime?,

    @Column(name = "last_used_at")
    var lastUsedAt: ZonedDateTime?,

    @Column(name = "revoked_at")
    var revokedAt: ZonedDateTime?,
) : BaseEntity() {

    fun revoke(now: ZonedDateTime) {
        check(revokedAt == null) { "이미 폐기된 ApiToken입니다" }
        revokedAt = now
    }

    fun recordUsage(now: ZonedDateTime) {
        lastUsedAt = now
    }

    fun isExpired(now: ZonedDateTime): Boolean = expiresAt?.isBefore(now) ?: false

    fun isActive(now: ZonedDateTime): Boolean = revokedAt == null && !isExpired(now) && !isDeleted
}
