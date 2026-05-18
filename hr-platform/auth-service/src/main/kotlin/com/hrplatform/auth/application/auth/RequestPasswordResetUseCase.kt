package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import com.hrplatform.auth.infrastructure.passwordreset.PasswordResetTokenStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RequestPasswordResetCommand(
    val email: String,
)

@Service
class RequestPasswordResetUseCase(
    private val authDomainService: AuthDomainService,
    private val passwordResetTokenStore: PasswordResetTokenStore,
) {

    @Transactional
    fun execute(command: RequestPasswordResetCommand): String {
        val token = authDomainService.requestPasswordReset(command.email)
        passwordResetTokenStore.store(token)
        return token
    }
}
