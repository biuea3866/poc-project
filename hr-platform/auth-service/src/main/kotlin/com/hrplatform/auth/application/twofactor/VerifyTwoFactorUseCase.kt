package com.hrplatform.auth.application.twofactor

import com.hrplatform.auth.domain.twofactor.service.TwoFactorDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class VerifyTwoFactorCommand(
    val userAccountId: Long,
    val otp: String,
)

@Service
class VerifyTwoFactorUseCase(
    private val twoFactorDomainService: TwoFactorDomainService,
) {

    @Transactional(readOnly = true)
    fun execute(command: VerifyTwoFactorCommand): Boolean =
        twoFactorDomainService.verify(command.userAccountId, command.otp)
}
