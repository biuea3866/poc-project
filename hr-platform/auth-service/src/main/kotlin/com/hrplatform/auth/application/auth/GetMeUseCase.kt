package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.role.Role
import com.hrplatform.auth.domain.role.service.RoleDomainService
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.core.exception.NotFoundException
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
    private val userAccountRepository: UserAccountRepository,
    private val roleDomainService: RoleDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(command: GetMeCommand): MeResult {
        val userAccount = userAccountRepository.findById(command.userAccountId)
            ?: throw NotFoundException(errorCode = "USER_ACCOUNT_NOT_FOUND", message = "UserAccount를 찾을 수 없습니다: ${command.userAccountId}")
        val roles = roleDomainService.findUserRoles(command.userAccountId)
        return MeResult(userAccount = userAccount, roles = roles)
    }
}
