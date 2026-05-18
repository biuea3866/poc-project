package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ConfirmPasswordResetCommand(
    val token: String,
    val newPassword: String,
)

@Service
class ConfirmPasswordResetUseCase(
    private val authDomainService: AuthDomainService,
) {

    @Transactional
    fun execute(command: ConfirmPasswordResetCommand) {
        authDomainService.confirmPasswordReset(command.token, command.newPassword)
    }
}
