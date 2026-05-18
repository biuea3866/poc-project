package com.hrplatform.auth.presentation.controller

import com.hrplatform.auth.application.auth.MeResult
import com.hrplatform.auth.domain.account.UserAccountStatus
import java.time.ZonedDateTime

data class MeResponse(
    val userAccountId: Long,
    val employmentId: Long,
    val companyId: Long,
    val status: UserAccountStatus,
    val twoFactorEnabled: Boolean,
    val lastLoginAt: ZonedDateTime?,
    val roles: List<RoleResponse>,
) {
    companion object {
        fun of(result: MeResult): MeResponse {
            val account = result.userAccount
            return MeResponse(
                userAccountId = requireNotNull(account.id),
                employmentId = account.employmentId,
                companyId = account.companyId,
                status = account.status,
                twoFactorEnabled = account.twoFactorEnabled,
                lastLoginAt = account.lastLoginAt,
                roles = result.roles.map { RoleResponse.of(it) },
            )
        }
    }
}
