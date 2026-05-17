package com.hrplatform.employee.infrastructure.history

import org.springframework.data.jpa.repository.JpaRepository

interface EmploymentHistoryJpaRepository :
    JpaRepository<EmploymentHistoryEntity, Long>,
    EmploymentHistoryCustomRepository
