package com.hrplatform.auth.domain.admin.service

import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.auth.service.ApiTokenResult
import com.hrplatform.auth.domain.token.ApiToken
import com.hrplatform.auth.domain.token.ApiTokenRepository
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.core.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Base64

private const val API_TOKEN_PREFIX = "hrp_"

@Service
class AdminAuthDomainService(
    private val userAccountRepository: UserAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val apiTokenRepository: ApiTokenRepository,
    private val eventPublisher: DomainEventPublisher,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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

        val reason = if (actorEmploymentId != null) "ADMIN_TERMINATE_ALL:$actorEmploymentId" else "ADMIN_TERMINATE_ALL"
        refreshTokenRepository.revokeAllByUserAccountId(userAccountId, reason, now)
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

        logger.info("issueApiToken: userAccountId={}, name={}, actorEmploymentId={}", userAccountId, name, actorEmploymentId)

        val randomBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val rawSuffix = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        val rawToken = "$API_TOKEN_PREFIX$rawSuffix"
        val tokenHash = sha256(rawSuffix)

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

        logger.info("revokeApiToken: apiTokenId={}, actorEmploymentId={}", apiTokenId, actorEmploymentId)
        apiToken.revoke(now)
        apiTokenRepository.save(apiToken)
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
