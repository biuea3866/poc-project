package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.admin.service.AdminAuthDomainService
import com.hrplatform.auth.domain.auth.service.ApiTokenResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

data class IssueApiTokenCommand(
    val userAccountId: Long,
    val name: String,
    val scopes: List<String>,
    val expiresAt: ZonedDateTime?,
    val actorEmploymentId: Long?,
)

@Service
class IssueApiTokenUseCase(
    private val adminAuthDomainService: AdminAuthDomainService,
) {

    @Transactional
    fun execute(command: IssueApiTokenCommand): ApiTokenResult =
        adminAuthDomainService.issueApiToken(
            userAccountId = command.userAccountId,
            name = command.name,
            scopes = command.scopes,
            expiresAt = command.expiresAt,
            actorEmploymentId = command.actorEmploymentId,
        )
}
