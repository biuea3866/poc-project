package com.biuea.wiki.presentation.user.response

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val user: UserResponse,
)

data class RefreshResponse(
    val accessToken: String,
    val tokenType: String,
)
