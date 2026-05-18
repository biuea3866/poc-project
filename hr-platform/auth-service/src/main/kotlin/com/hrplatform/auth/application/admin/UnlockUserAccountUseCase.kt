package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.admin.service.AdminAuthDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UnlockUserAccountCommand(
    val userAccountId: Long,
    val actorEmploymentId: Long?,
)

@Service
class UnlockUserAccountUseCase(
    private val adminAuthDomainService: AdminAuthDomainService,
) {

    @Transactional
    fun execute(command: UnlockUserAccountCommand) {
        adminAuthDomainService.unlock(command.userAccountId, command.actorEmploymentId)
    }
}
