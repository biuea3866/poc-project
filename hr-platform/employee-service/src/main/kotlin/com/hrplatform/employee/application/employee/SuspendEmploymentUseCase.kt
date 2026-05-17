package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class SuspendEmploymentUseCase(
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional
    fun execute(command: SuspendEmploymentCommand, now: ZonedDateTime = ZonedDateTime.now()): SuspendEmploymentResult {
        val employment = employmentDomainService.suspend(
            employmentId = command.employmentId,
            reason = command.reason,
            until = command.until,
            actorEmploymentId = command.actorEmploymentId,
            now = now,
        )
        return SuspendEmploymentResult.of(employment)
    }
}
