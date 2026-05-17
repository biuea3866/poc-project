package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class HireEmployeeUseCase(
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional
    fun execute(command: HireEmployeeCommand, now: ZonedDateTime = ZonedDateTime.now()): HireEmployeeResult {
        val employment = employmentDomainService.hire(command.toDomainCommand(), now)
        return HireEmployeeResult.of(employment)
    }
}
