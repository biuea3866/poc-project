package com.biuea.wiki.domain.auth

import java.io.Serializable

data class AuthenticatedUser(
    val id: Long,
    val email: String,
    val name: String,
) : Serializable
