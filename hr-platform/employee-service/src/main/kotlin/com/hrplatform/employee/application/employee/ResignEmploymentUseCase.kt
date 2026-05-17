package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class ResignEmploymentUseCase(
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional
    fun execute(command: ResignEmploymentCommand, now: ZonedDateTime = ZonedDateTime.now()): ResignEmploymentResult {
        val employment = employmentDomainService.resign(
            employmentId = command.employmentId,
            reason = command.reason,
            actorEmploymentId = command.actorEmploymentId,
            now = now,
        )
        return ResignEmploymentResult.of(employment)
    }
}
