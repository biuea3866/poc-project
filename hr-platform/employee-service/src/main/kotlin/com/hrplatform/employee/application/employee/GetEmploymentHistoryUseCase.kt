package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.history.EmploymentHistoryDomainService
import com.hrplatform.employee.domain.query.EmployeeQueryDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetEmploymentHistoryUseCase(
    private val employmentHistoryDomainService: EmploymentHistoryDomainService,
    private val employeeQueryDomainService: EmployeeQueryDomainService,
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional(readOnly = true)
    fun execute(command: GetEmploymentHistoryCommand): List<EmploymentHistoryResult> {
        val viewer = employmentDomainService.getById(command.viewerEmploymentId)
        employeeQueryDomainService.getEmployee(viewer, command.employmentId)
        val histories = employmentHistoryDomainService.findByEmployment(command.employmentId)
        return histories.map { EmploymentHistoryResult.of(it) }
    }
}
