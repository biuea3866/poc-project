package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.auth.service.AuthDomainService
import com.hrplatform.auth.domain.role.Role
import com.hrplatform.auth.domain.role.service.RoleDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GetMeCommand(
    val userAccountId: Long,
)

data class MeResult(
    val userAccount: UserAccount,
    val roles: List<Role>,
)

@Service
class GetMeUseCase(
    private val authDomainService: AuthDomainService,
    private val roleDomainService: RoleDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(command: GetMeCommand): MeResult {
        val userAccount = authDomainService.getMe(command.userAccountId)
        val roles = roleDomainService.findUserRoles(command.userAccountId)
        return MeResult(userAccount = userAccount, roles = roles)
    }
}
