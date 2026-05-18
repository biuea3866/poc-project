package com.hrplatform.auth.application.auth

import java.time.ZonedDateTime

data class LoginResult(
    val userAccountId: Long,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: ZonedDateTime,
    val requiresTwoFactor: Boolean,
)

data class ApiTokenResult(
    val apiTokenId: Long,
    val rawToken: String,
    val name: String,
    val scopes: List<String>,
    val expiresAt: ZonedDateTime?,
)
