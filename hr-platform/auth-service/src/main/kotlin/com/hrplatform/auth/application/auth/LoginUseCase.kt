package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import com.hrplatform.auth.domain.auth.service.LoginResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class LoginCommand(
    val email: String,
    val rawPassword: String,
    val deviceInfo: String?,
    val ipAddress: String?,
    val userAgent: String?,
)

@Service
class LoginUseCase(
    private val authDomainService: AuthDomainService,
) {

    @Transactional
    fun execute(command: LoginCommand): LoginResult =
        authDomainService.login(
            email = command.email,
            rawPassword = command.rawPassword,
            deviceInfo = command.deviceInfo,
            ipAddress = command.ipAddress,
            userAgent = command.userAgent,
        )
}
