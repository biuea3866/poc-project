package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class ResumeEmploymentUseCase(
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional
    fun execute(command: ResumeEmploymentCommand, now: ZonedDateTime = ZonedDateTime.now()): ResumeEmploymentResult {
        val employment = employmentDomainService.resume(
            employmentId = command.employmentId,
            actorEmploymentId = command.actorEmploymentId,
            now = now,
        )
        return ResumeEmploymentResult.of(employment)
    }
}
