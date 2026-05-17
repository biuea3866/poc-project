package com.hrplatform.employee.infrastructure.department

import com.hrplatform.employee.domain.department.Department
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DepartmentJpaRepository : JpaRepository<Department, Long>
