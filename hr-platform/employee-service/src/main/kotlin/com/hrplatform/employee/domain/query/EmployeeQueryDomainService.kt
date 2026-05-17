package com.hrplatform.employee.domain.query

import com.hrplatform.core.exception.ForbiddenException
import com.hrplatform.employee.domain.department.DepartmentRepository
import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentNotFoundException
import com.hrplatform.employee.domain.employment.EmploymentRepository
import com.hrplatform.employee.domain.employment.EmploymentRole
import com.hrplatform.employee.domain.employment.EmploymentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class EmployeeQueryDomainService(
    private val employmentRepository: EmploymentRepository,
    private val departmentRepository: DepartmentRepository,
) {
    fun search(viewer: Employment, criteria: SearchCriteria, page: Pageable): Page<Employment> {
        if (viewer.status != EmploymentStatus.ACTIVE) {
            throw ForbiddenException("VIEWER_NOT_ACTIVE", "비활성 사용자는 검색할 수 없습니다")
        }
        return when {
            viewer.role.isAtLeast(EmploymentRole.HR_MANAGER) ->
                employmentRepository.findByCompanyIdWithPage(criteria.companyId, null, null, page)
            viewer.role == EmploymentRole.TEAM_LEAD -> {
                val departmentId = requireNotNull(viewer.departmentId) { "TEAM_LEAD는 부서에 속해야 합니다" }
                val department = departmentRepository.findById(departmentId)
                    ?: throw ForbiddenException("DEPARTMENT_NOT_FOUND", "부서를 찾을 수 없습니다")
                employmentRepository.findByCompanyIdWithPage(criteria.companyId, department.path, null, page)
            }
            else ->
                employmentRepository.findByCompanyIdWithPage(
                    criteria.companyId,
                    null,
                    requireNotNull(viewer.id) { "viewer id가 없습니다" },
                    page,
                )
        }
    }

    fun getEmployee(viewer: Employment, employmentId: Long): Employment {
        val target = employmentRepository.findById(employmentId)
            ?: throw EmploymentNotFoundException()
        if (!target.isAccessibleBy(viewer)) {
            throw ForbiddenException("ACCESS_DENIED", "해당 직원 정보에 접근할 수 없습니다")
        }
        return target
    }
}
