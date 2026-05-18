package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.admin.service.AdminAuthDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class LogoutAllSessionsCommand(
    val userAccountId: Long,
    val actorEmploymentId: Long?,
)

@Service
class LogoutAllSessionsUseCase(
    private val adminAuthDomainService: AdminAuthDomainService,
) {

    @Transactional
    fun execute(command: LogoutAllSessionsCommand) {
        adminAuthDomainService.terminateAllSessions(command.userAccountId, command.actorEmploymentId)
    }
}
