package com.hrplatform.auth.presentation.controller

import com.hrplatform.auth.domain.auth.service.ApiTokenResult
import com.hrplatform.auth.domain.role.Role
import java.time.ZonedDateTime

data class RoleResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val isSystemRole: Boolean,
) {
    companion object {
        fun of(role: Role): RoleResponse = RoleResponse(
            id = requireNotNull(role.id),
            code = role.code,
            name = role.name,
            description = role.description,
            isSystemRole = role.isSystemRole,
        )
    }
}

data class ApiTokenIssueResponse(
    val apiTokenId: Long,
    val rawToken: String,
    val name: String,
    val scopes: List<String>,
    val expiresAt: ZonedDateTime?,
) {
    companion object {
        fun of(result: ApiTokenResult): ApiTokenIssueResponse = ApiTokenIssueResponse(
            apiTokenId = result.apiTokenId,
            rawToken = result.rawToken,
            name = result.name,
            scopes = result.scopes,
            expiresAt = result.expiresAt,
        )
    }
}
