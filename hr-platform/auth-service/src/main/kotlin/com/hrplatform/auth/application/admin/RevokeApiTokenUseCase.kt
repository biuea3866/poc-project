package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.admin.service.AdminAuthDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RevokeApiTokenCommand(
    val apiTokenId: Long,
    val actorEmploymentId: Long?,
)

@Service
class RevokeApiTokenUseCase(
    private val adminAuthDomainService: AdminAuthDomainService,
) {

    @Transactional
    fun execute(command: RevokeApiTokenCommand) {
        adminAuthDomainService.revokeApiToken(command.apiTokenId, command.actorEmploymentId)
    }
}
