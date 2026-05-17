package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.UpdatePersonalInfoCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdatePersonalInfoRequest(
    @field:Email
    @field:NotBlank
    val personalEmail: String,

    val phoneNumber: String?,
) {
    fun toCommand(viewerEmploymentId: Long, personId: Long): UpdatePersonalInfoCommand =
        UpdatePersonalInfoCommand(
            viewerEmploymentId = viewerEmploymentId,
            personId = personId,
            personalEmail = personalEmail,
            phoneNumber = phoneNumber,
        )
}
