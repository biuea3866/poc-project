package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.AuthDomainService
import com.hrplatform.auth.domain.auth.service.TokenPair
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RefreshCommand(
    val rawRefreshToken: String,
)

@Service
class RefreshUseCase(
    private val authDomainService: AuthDomainService,
) {

    @Transactional
    fun execute(command: RefreshCommand): TokenPair =
        authDomainService.refresh(command.rawRefreshToken)
}
