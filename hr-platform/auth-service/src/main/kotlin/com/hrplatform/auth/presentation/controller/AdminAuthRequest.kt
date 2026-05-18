package com.hrplatform.auth.presentation.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime

data class AssignRoleRequest(
    @field:NotNull val roleId: Long,
)

data class IssueApiTokenRequest(
    @field:NotBlank val name: String,
    val scopes: List<String> = emptyList(),
    val expiresAt: ZonedDateTime?,
)
