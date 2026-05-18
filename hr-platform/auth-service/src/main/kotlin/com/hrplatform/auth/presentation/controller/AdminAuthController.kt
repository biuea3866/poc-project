package com.hrplatform.auth.presentation.controller

import com.hrplatform.auth.application.admin.AssignRoleCommand
import com.hrplatform.auth.application.admin.AssignRoleUseCase
import com.hrplatform.auth.application.admin.IssueApiTokenCommand
import com.hrplatform.auth.application.admin.IssueApiTokenUseCase
import com.hrplatform.auth.application.admin.ListRolesCommand
import com.hrplatform.auth.application.admin.ListRolesUseCase
import com.hrplatform.auth.application.admin.LogoutAllSessionsCommand
import com.hrplatform.auth.application.admin.LogoutAllSessionsUseCase
import com.hrplatform.auth.application.admin.RevokeApiTokenCommand
import com.hrplatform.auth.application.admin.RevokeApiTokenUseCase
import com.hrplatform.auth.application.admin.RevokeRoleCommand
import com.hrplatform.auth.application.admin.RevokeRoleUseCase
import com.hrplatform.auth.application.admin.UnlockUserAccountCommand
import com.hrplatform.auth.application.admin.UnlockUserAccountUseCase
import com.hrplatform.auth.presentation.auth.AuthEmploymentId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/admin")
class AdminAuthController(
    private val unlockUseCase: UnlockUserAccountUseCase,
    private val logoutAllUseCase: LogoutAllSessionsUseCase,
    private val issueApiTokenUseCase: IssueApiTokenUseCase,
    private val revokeApiTokenUseCase: RevokeApiTokenUseCase,
    private val assignRoleUseCase: AssignRoleUseCase,
    private val revokeRoleUseCase: RevokeRoleUseCase,
    private val listRolesUseCase: ListRolesUseCase,
) {

    @PostMapping("/users/{id}/unlock")
    fun unlock(
        @PathVariable id: Long,
        @AuthEmploymentId actorId: Long,
    ): ResponseEntity<Void> {
        unlockUseCase.execute(UnlockUserAccountCommand(userAccountId = id, actorEmploymentId = actorId))
        return ResponseEntity.ok().build()
    }

    @PostMapping("/users/{id}/sessions/logout-all")
    fun logoutAll(
        @PathVariable id: Long,
        @AuthEmploymentId actorId: Long,
    ): ResponseEntity<Void> {
        logoutAllUseCase.execute(LogoutAllSessionsCommand(userAccountId = id, actorEmploymentId = actorId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/users/{id}/roles")
    fun assignRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignRoleRequest,
        @AuthEmploymentId actorId: Long,
    ): ResponseEntity<Void> {
        assignRoleUseCase.execute(AssignRoleCommand(userAccountId = id, roleId = request.roleId, actorEmploymentId = actorId))
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/users/{id}/roles/{roleId}")
    fun revokeRole(
        @PathVariable id: Long,
        @PathVariable roleId: Long,
        @AuthEmploymentId actorId: Long,
    ): ResponseEntity<Void> {
        revokeRoleUseCase.execute(RevokeRoleCommand(userAccountId = id, roleId = roleId, actorEmploymentId = actorId))
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/roles")
    fun listRoles(@RequestParam companyId: Long): List<RoleResponse> {
        return listRolesUseCase.execute(ListRolesCommand(companyId = companyId)).map { RoleResponse.of(it) }
    }

    @PostMapping("/api-tokens")
    fun issueApiToken(
        @Valid @RequestBody request: IssueApiTokenRequest,
        @AuthEmploymentId actorId: Long,
    ): ApiTokenIssueResponse {
        val result = issueApiTokenUseCase.execute(
            IssueApiTokenCommand(
                userAccountId = actorId,
                name = request.name,
                scopes = request.scopes,
                expiresAt = request.expiresAt,
                actorEmploymentId = actorId,
            ),
        )
        return ApiTokenIssueResponse.of(result)
    }

    @DeleteMapping("/api-tokens/{id}")
    fun revokeApiToken(
        @PathVariable id: Long,
        @AuthEmploymentId actorId: Long,
    ): ResponseEntity<Void> {
        revokeApiTokenUseCase.execute(RevokeApiTokenCommand(apiTokenId = id, actorEmploymentId = actorId))
        return ResponseEntity.noContent().build()
    }
}
