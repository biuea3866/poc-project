package com.hrplatform.employee.application.department

import com.hrplatform.employee.domain.department.DepartmentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MoveDepartmentUseCase(
    private val departmentDomainService: DepartmentDomainService,
) {
    @Transactional
    fun execute(command: MoveDepartmentCommand): DepartmentResult {
        val department = departmentDomainService.moveTo(
            departmentId = command.departmentId,
            newParentId = command.newParentId,
            actorEmploymentId = command.actorEmploymentId,
        )
        return DepartmentResult.of(department)
    }
}
