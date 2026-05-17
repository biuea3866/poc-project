package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.query.EmployeeQueryDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetEmployeeUseCase(
    private val employeeQueryDomainService: EmployeeQueryDomainService,
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional(readOnly = true)
    fun execute(command: GetEmployeeCommand): GetEmployeeResult {
        val viewer = employmentDomainService.getById(command.viewerEmploymentId)
        val employment = employeeQueryDomainService.getEmployee(viewer, command.targetEmploymentId)
        return GetEmployeeResult.of(employment)
    }
}
