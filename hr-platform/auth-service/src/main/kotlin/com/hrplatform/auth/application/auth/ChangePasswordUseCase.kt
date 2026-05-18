package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ChangePasswordCommand(
    val userAccountId: Long,
    val oldRawPassword: String,
    val newRawPassword: String,
    val actorEmploymentId: Long?,
)

@Service
class ChangePasswordUseCase(
    private val authDomainService: AuthDomainService,
) {

    @Transactional
    fun execute(command: ChangePasswordCommand) {
        authDomainService.changePassword(
            userAccountId = command.userAccountId,
            oldRawPassword = command.oldRawPassword,
            newRawPassword = command.newRawPassword,
            actorEmploymentId = command.actorEmploymentId,
        )
    }
}
