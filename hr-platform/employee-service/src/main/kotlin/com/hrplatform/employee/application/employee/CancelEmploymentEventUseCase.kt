package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class CancelEmploymentEventUseCase(
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional
    fun execute(
        command: CancelEmploymentEventCommand,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): CancelEmploymentEventResult {
        val employment = employmentDomainService.cancelEvent(
            employmentId = command.employmentId,
            historyId = command.historyId,
            cancellationReason = command.cancellationReason,
            actorEmploymentId = command.actorEmploymentId,
            now = now,
        )
        return CancelEmploymentEventResult.of(employment)
    }
}
