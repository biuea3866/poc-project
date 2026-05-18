package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.role.service.RoleDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AssignRoleCommand(
    val userAccountId: Long,
    val roleId: Long,
    val actorEmploymentId: Long?,
)

@Service
class AssignRoleUseCase(
    private val roleDomainService: RoleDomainService,
) {

    @Transactional
    fun execute(command: AssignRoleCommand) {
        roleDomainService.assignRole(command.userAccountId, command.roleId, command.actorEmploymentId)
    }
}
