package com.hrplatform.employee.infrastructure.department

import com.hrplatform.employee.domain.department.Department
import com.hrplatform.employee.domain.department.DepartmentRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class DepartmentRepositoryImpl(
    val departmentJpaRepository: DepartmentJpaRepository,
) : DepartmentRepository {

    override fun save(department: Department): Department {
        val entity = DepartmentEntity.fromDomain(department)
        val saved = departmentJpaRepository.saveAndFlush(entity)
        return saved.toDomain()
    }

    override fun findById(id: Long): Department? =
        departmentJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByPathPrefix(prefix: String): List<Department> =
        departmentJpaRepository.findByPathPrefixQuery(prefix).map { it.toDomain() }

    override fun findByParentId(parentId: Long): List<Department> =
        departmentJpaRepository.findByParentIdQuery(parentId).map { it.toDomain() }
}
