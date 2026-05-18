package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import com.hrplatform.auth.infrastructure.passwordreset.PasswordResetTokenStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ConfirmPasswordResetCommand(
    val token: String,
    val newPassword: String,
)

@Service
class ConfirmPasswordResetUseCase(
    private val authDomainService: AuthDomainService,
    private val passwordResetTokenStore: PasswordResetTokenStore,
) {

    @Transactional
    fun execute(command: ConfirmPasswordResetCommand) {
        passwordResetTokenStore.validate(command.token)
        authDomainService.confirmPasswordReset(command.token, command.newPassword)
        passwordResetTokenStore.consume(command.token)
    }
}
