package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.query.EmployeeQueryDomainService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchEmployeesUseCase(
    private val employeeQueryDomainService: EmployeeQueryDomainService,
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional(readOnly = true)
    fun execute(command: SearchEmployeesCommand): Page<EmployeeSummaryResult> {
        val viewer = employmentDomainService.getById(command.viewerEmploymentId)
        val page = employeeQueryDomainService.search(viewer, command.toCriteria(), command.pageable)
        return EmployeeSummaryResult.pageOf(page)
    }
}
