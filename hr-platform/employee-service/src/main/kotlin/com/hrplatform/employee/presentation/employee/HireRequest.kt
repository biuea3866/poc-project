package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.HireEmployeeCommand
import com.hrplatform.employee.domain.employment.EmploymentType
import com.hrplatform.employee.domain.person.Gender
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

data class HireRequest(
    @field:Email
    @field:NotBlank
    val personalEmail: String,

    @field:NotBlank
    val name: String,

    val birthDate: LocalDate?,

    @field:Pattern(regexp = "[A-Z]{2}", message = "ISO 3166-1 alpha-2 국가 코드여야 합니다")
    val nationality: String?,

    val gender: Gender?,

    @field:NotNull
    val companyId: Long,

    @field:NotBlank
    val employeeNumber: String,

    @field:NotNull
    val employmentType: EmploymentType,

    @field:NotNull
    val startDate: LocalDate,

    @field:Pattern(regexp = "[A-Z]{2}", message = "ISO 3166-1 alpha-2 국가 코드여야 합니다")
    @field:NotBlank
    val country: String,

    @field:Pattern(regexp = "[A-Z]{3}", message = "ISO 4217 통화 코드여야 합니다")
    @field:NotBlank
    val currency: String,

    @field:NotBlank
    val timezone: String,

    val departmentId: Long?,
    val managerEmploymentId: Long?,
) {
    fun toCommand(actorEmploymentId: Long?): HireEmployeeCommand = HireEmployeeCommand(
        personalEmail = personalEmail,
        name = name,
        birthDate = birthDate,
        nationality = nationality,
        gender = gender,
        companyId = companyId,
        employeeNumber = employeeNumber,
        employmentType = employmentType,
        startDate = startDate,
        country = country,
        currency = currency,
        timezone = timezone,
        departmentId = departmentId,
        managerEmploymentId = managerEmploymentId,
        actorEmploymentId = actorEmploymentId,
    )
}
