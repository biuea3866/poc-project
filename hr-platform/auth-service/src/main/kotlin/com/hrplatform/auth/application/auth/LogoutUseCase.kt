package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class LogoutCommand(
    val rawRefreshToken: String,
)

@Service
class LogoutUseCase(
    private val authDomainService: AuthDomainService,
) {

    @Transactional
    fun execute(command: LogoutCommand) {
        authDomainService.logout(command.rawRefreshToken)
    }
}
