package com.hrplatform.auth.application.twofactor

import com.hrplatform.auth.domain.twofactor.service.TwoFactorDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class DisableTwoFactorCommand(
    val userAccountId: Long,
    val actorEmploymentId: Long?,
)

@Service
class DisableTwoFactorUseCase(
    private val twoFactorDomainService: TwoFactorDomainService,
) {

    @Transactional
    fun execute(command: DisableTwoFactorCommand) {
        twoFactorDomainService.disable(command.userAccountId, command.actorEmploymentId)
    }
}
