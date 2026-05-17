package com.hrplatform.employee.infrastructure.employment

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentRepository
import com.hrplatform.employee.domain.employment.EmploymentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class EmploymentRepositoryImpl(
    private val jpaRepository: EmploymentJpaRepository,
) : EmploymentRepository {

    override fun save(employment: Employment): Employment =
        jpaRepository.save(employment)

    override fun findById(id: Long): Employment? =
        jpaRepository.findById(id).orElse(null)

    override fun findByCompanyIdAndEmployeeNumber(companyId: Long, employeeNumber: String): Employment? =
        jpaRepository.findByCompanyIdAndEmployeeNumber(companyId, employeeNumber)

    override fun findByPersonId(personId: Long): List<Employment> =
        jpaRepository.findByPersonId(personId)

    override fun findByDepartmentTreePath(pathPrefix: String): List<Employment> =
        jpaRepository.findByDepartmentTreePath(pathPrefix)

    override fun findManagedBy(managerEmploymentId: Long): List<Employment> =
        jpaRepository.findManagedBy(managerEmploymentId)

    override fun findByCompanyIdAndStatus(companyId: Long, status: EmploymentStatus): List<Employment> =
        jpaRepository.findByCompanyIdAndStatus(companyId, status)

    override fun findByCompanyIdWithPage(
        companyId: Long,
        departmentPathPrefix: String?,
        employmentId: Long?,
        pageable: Pageable,
    ): Page<Employment> =
        jpaRepository.findByCompanyIdWithPage(companyId, departmentPathPrefix, employmentId, pageable)
}
