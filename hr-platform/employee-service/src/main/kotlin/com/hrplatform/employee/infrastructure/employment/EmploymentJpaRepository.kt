package com.hrplatform.employee.infrastructure.employment

import com.hrplatform.employee.domain.employment.Employment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmploymentJpaRepository : JpaRepository<Employment, Long>, EmploymentCustomRepository
