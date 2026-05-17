package com.hrplatform.employee.application.department

import com.hrplatform.employee.domain.department.DepartmentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssignDepartmentHeadUseCase(
    private val departmentDomainService: DepartmentDomainService,
) {
    @Transactional
    fun execute(command: AssignDepartmentHeadCommand): DepartmentResult {
        val department = departmentDomainService.assignHead(
            departmentId = command.departmentId,
            employmentId = command.employmentId,
            actorEmploymentId = command.actorEmploymentId,
        )
        return DepartmentResult.of(department)
    }
}
