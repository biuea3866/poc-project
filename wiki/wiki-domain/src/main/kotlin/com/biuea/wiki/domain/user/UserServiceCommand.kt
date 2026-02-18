package com.biuea.wiki.domain.user

data class SignUpUserCommand(
    val email: String,
    val password: String,
    val name: String,
)

data class LoginUserCommand(
    val email: String,
    val password: String,
)

data class DeleteUserCommand(
    val userId: Long,
)
