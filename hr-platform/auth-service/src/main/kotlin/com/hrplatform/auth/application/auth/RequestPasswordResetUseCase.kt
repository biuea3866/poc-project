package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RequestPasswordResetCommand(
    val email: String,
)

@Service
class RequestPasswordResetUseCase(
    private val authDomainService: AuthDomainService,
) {

    @Transactional
    fun execute(command: RequestPasswordResetCommand): String =
        authDomainService.requestPasswordReset(command.email)
}
