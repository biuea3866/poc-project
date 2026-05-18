package com.hrplatform.auth.presentation.controller

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String,
    val deviceInfo: String?,
    val ipAddress: String?,
    val userAgent: String?,
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String,
)

data class LogoutRequest(
    @field:NotBlank val refreshToken: String,
)

data class PasswordResetRequest(
    @field:Email @field:NotBlank val email: String,
)

data class PasswordResetConfirmRequest(
    @field:NotBlank val token: String,
    @field:NotBlank @field:Size(min = 10) val newPassword: String,
)

data class TwoFactorVerifyRequest(
    @field:NotBlank val otp: String,
)
