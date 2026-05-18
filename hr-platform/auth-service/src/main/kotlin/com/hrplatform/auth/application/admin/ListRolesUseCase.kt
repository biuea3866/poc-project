package com.hrplatform.auth.application.admin

import com.hrplatform.auth.domain.role.Role
import com.hrplatform.auth.domain.role.service.RoleDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ListRolesCommand(
    val companyId: Long,
)

@Service
class ListRolesUseCase(
    private val roleDomainService: RoleDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(command: ListRolesCommand): List<Role> =
        roleDomainService.findAllRoles(command.companyId)
}
