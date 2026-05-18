package com.hrplatform.auth.application.admin

import com.hrplatform.auth.application.auth.ApiTokenResult
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.token.ApiToken
import com.hrplatform.auth.domain.token.ApiTokenRepository
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.core.exception.NotFoundException
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Base64
import java.util.UUID

private const val API_TOKEN_PREFIX = "hrp_"

@Service
class AdminAuthDomainService(
    private val userAccountRepository: UserAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val apiTokenRepository: ApiTokenRepository,
    private val eventPublisher: DomainEventPublisher,
) {

    fun unlock(userAccountId: Long, actorEmploymentId: Long?) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findById(userAccountId)
            ?: throw NotFoundException(errorCode = "USER_ACCOUNT_NOT_FOUND", message = "UserAccount를 찾을 수 없습니다: $userAccountId")

        userAccount.unlock(actorEmploymentId, now)
        userAccountRepository.save(userAccount)
        eventPublisher.publishAll(userAccount.pullDomainEvents())
    }

    fun terminateAllSessions(userAccountId: Long, actorEmploymentId: Long?) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        userAccountRepository.findById(userAccountId)
            ?: throw NotFoundException(errorCode = "USER_ACCOUNT_NOT_FOUND", message = "UserAccount를 찾을 수 없습니다: $userAccountId")

        refreshTokenRepository.revokeAllByUserAccountId(userAccountId, "ADMIN_TERMINATE_ALL", now)
    }

    fun issueApiToken(
        userAccountId: Long,
        name: String,
        scopes: List<String>,
        expiresAt: ZonedDateTime?,
        actorEmploymentId: Long?,
    ): ApiTokenResult {
        userAccountRepository.findById(userAccountId)
            ?: throw NotFoundException(errorCode = "USER_ACCOUNT_NOT_FOUND", message = "UserAccount를 찾을 수 없습니다: $userAccountId")

        val rawToken = "$API_TOKEN_PREFIX${UUID.randomUUID()}"
        val tokenHash = sha256(rawToken.removePrefix(API_TOKEN_PREFIX))

        val apiToken = ApiToken(
            userAccountId = userAccountId,
            name = name,
            tokenHash = tokenHash,
            scopes = scopes,
            expiresAt = expiresAt,
            lastUsedAt = null,
            revokedAt = null,
        )
        val saved = apiTokenRepository.save(apiToken)

        return ApiTokenResult(
            apiTokenId = requireNotNull(saved.id),
            rawToken = rawToken,
            name = name,
            scopes = scopes,
            expiresAt = expiresAt,
        )
    }

    fun revokeApiToken(apiTokenId: Long, actorEmploymentId: Long?) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val apiToken = apiTokenRepository.findById(apiTokenId)
            ?: throw NotFoundException(errorCode = "API_TOKEN_NOT_FOUND", message = "ApiToken을 찾을 수 없습니다: $apiTokenId")

        apiToken.revoke(now)
        apiTokenRepository.save(apiToken)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.getEncoder().encodeToString(digest.digest(input.toByteArray(Charsets.UTF_8)))
    }
}
