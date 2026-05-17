package com.hrplatform.employee.domain.employment

import java.time.LocalDate

/**
 * Employment 생성 시 필요한 불변 스펙.
 *
 * LongParameterList detekt 규칙 준수를 위해 Employment 생성자를 EmploymentSpec으로 그룹핑.
 * 가변 필드(status, departmentId, positionId, baseSalary)는 Employment 본체에 직접 유지.
 */
data class EmploymentSpec(
    val personId: Long,
    val companyId: Long,
    val employeeNumber: String,
    val employmentType: EmploymentType,
    val startDate: LocalDate,
    val country: String,
    val currency: String,
    val timezone: String,
    val endDate: LocalDate? = null,
    val managerEmploymentId: Long? = null,
    val workSchedulePolicyId: Long? = null,
    val leavePolicyId: Long? = null,
    val compensationCurrency: String? = null,
)
