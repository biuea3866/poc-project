package com.hrplatform.employee.application.department

import com.hrplatform.employee.domain.department.DepartmentDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateDepartmentUseCase(
    private val departmentDomainService: DepartmentDomainService,
) {
    @Transactional
    fun execute(command: CreateDepartmentUseCaseCommand): DepartmentResult {
        val department = departmentDomainService.create(command.toDomainCommand())
        return DepartmentResult.of(department)
    }
}
