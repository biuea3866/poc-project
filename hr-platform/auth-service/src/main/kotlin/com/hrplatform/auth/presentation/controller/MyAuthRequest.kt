package com.hrplatform.auth.presentation.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank val oldPassword: String,
    @field:NotBlank @field:Size(min = 10) val newPassword: String,
)
