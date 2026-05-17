package com.hrplatform.employee.domain.employment

import com.hrplatform.employee.domain.person.Gender
import java.time.LocalDate

data class HireCommand(
    val personalEmail: String,
    val name: String,
    val birthDate: LocalDate?,
    val nationality: String?,
    val gender: Gender?,
    val companyId: Long,
    val employeeNumber: String,
    val employmentType: EmploymentType,
    val startDate: LocalDate,
    val country: String,
    val currency: String,
    val timezone: String,
    val departmentId: Long?,
    val managerEmploymentId: Long?,
    val actorEmploymentId: Long?,
)
