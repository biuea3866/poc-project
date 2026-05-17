package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class BulkRecordEmploymentEventsUseCase(
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional
    fun execute(command: BulkRecordEmploymentEventsCommand, now: ZonedDateTime = ZonedDateTime.now()): BulkResult {
        command.commands.forEach { employmentDomainService.recordEvent(it.toDomainCommand(), now) }
        return BulkResult.success(command.commands.size)
    }
}
