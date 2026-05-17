package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class RecordEmploymentEventUseCase(
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional
    fun execute(
        command: RecordEmploymentEventCommand,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): RecordEmploymentEventResult {
        val employment = employmentDomainService.recordEvent(command.toDomainCommand(), now)
        return RecordEmploymentEventResult.of(employment)
    }
}
