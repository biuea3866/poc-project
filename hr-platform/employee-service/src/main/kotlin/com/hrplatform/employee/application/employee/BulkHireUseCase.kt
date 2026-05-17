package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class BulkHireUseCase(
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional
    fun execute(command: BulkHireCommand, now: ZonedDateTime = ZonedDateTime.now()): BulkResult {
        command.commands.forEach { employmentDomainService.hire(it.toDomainCommand(), now) }
        return BulkResult.success(command.commands.size)
    }
}
