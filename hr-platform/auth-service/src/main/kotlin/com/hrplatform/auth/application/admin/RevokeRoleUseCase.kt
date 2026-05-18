package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.role.service.RoleDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RevokeRoleCommand(
    val userAccountId: Long,
    val roleId: Long,
    val actorEmploymentId: Long?,
)

@Service
class RevokeRoleUseCase(
    private val roleDomainService: RoleDomainService,
) {

    @Transactional
    fun execute(command: RevokeRoleCommand) {
        roleDomainService.revokeRole(command.userAccountId, command.roleId, command.actorEmploymentId)
    }
}
