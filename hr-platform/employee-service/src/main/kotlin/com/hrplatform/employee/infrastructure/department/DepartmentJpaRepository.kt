package com.hrplatform.employee.infrastructure.department

import org.springframework.data.jpa.repository.JpaRepository

interface DepartmentJpaRepository : JpaRepository<DepartmentEntity, Long>, DepartmentCustomRepository
