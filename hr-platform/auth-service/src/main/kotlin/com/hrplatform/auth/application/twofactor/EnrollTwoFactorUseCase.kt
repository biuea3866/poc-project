package com.hrplatform.auth.application.twofactor

import com.hrplatform.auth.domain.twofactor.service.TwoFactorDomainService
import com.hrplatform.auth.domain.twofactor.service.TwoFactorEnrollmentResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class EnrollTwoFactorCommand(
    val userAccountId: Long,
    val actorEmploymentId: Long?,
)

@Service
class EnrollTwoFactorUseCase(
    private val twoFactorDomainService: TwoFactorDomainService,
) {

    @Transactional
    fun execute(command: EnrollTwoFactorCommand): TwoFactorEnrollmentResult =
        twoFactorDomainService.enroll(command.userAccountId, command.actorEmploymentId)
}
