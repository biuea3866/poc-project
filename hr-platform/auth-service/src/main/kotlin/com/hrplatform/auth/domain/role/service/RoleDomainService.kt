package com.hrplatform.auth.domain.role.service

import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.event.UserRoleAssignedEvent
import com.hrplatform.auth.domain.account.event.UserRoleRevokedEvent
import com.hrplatform.auth.domain.role.Role
import com.hrplatform.auth.domain.role.RoleRepository
import com.hrplatform.auth.domain.role.UserAccountRole
import com.hrplatform.auth.domain.role.UserAccountRoleRepository
import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.core.exception.BusinessException
import com.hrplatform.core.exception.NotFoundException
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class RoleDomainService(
    private val roleRepository: RoleRepository,
    private val userAccountRoleRepository: UserAccountRoleRepository,
    private val userAccountRepository: UserAccountRepository,
    private val eventPublisher: DomainEventPublisher,
) {

    fun assignRole(userAccountId: Long, roleId: Long, actorEmploymentId: Long?) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findById(userAccountId)
            ?: throw NotFoundException(errorCode = "USER_ACCOUNT_NOT_FOUND", message = "UserAccount를 찾을 수 없습니다: $userAccountId")
        val role = roleRepository.findById(roleId)
            ?: throw NotFoundException(errorCode = "ROLE_NOT_FOUND", message = "Role을 찾을 수 없습니다: $roleId")

        validateRoleNotAlreadyAssigned(userAccountId, roleId, role.code)

        userAccountRoleRepository.save(
            UserAccountRole(
                userAccountId = userAccountId,
                roleId = roleId,
                assignedAt = now,
                assignedBy = actorEmploymentId,
            ),
        )

        val event = UserRoleAssignedEvent(
            userAccountId = requireNotNull(userAccount.id),
            companyIdValue = userAccount.companyId,
            employmentId = userAccount.employmentId,
            email = userAccount.email,
            twoFactorEnabled = userAccount.twoFactorEnabled,
            lockedUntil = userAccount.lockedUntil,
            roleId = roleId,
            roleCode = role.code,
            actorEmploymentId = actorEmploymentId,
            occurredAt = now,
        )
        eventPublisher.publish(event)
    }

    fun revokeRole(userAccountId: Long, roleId: Long, actorEmploymentId: Long?) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findById(userAccountId)
            ?: throw NotFoundException(errorCode = "USER_ACCOUNT_NOT_FOUND", message = "UserAccount를 찾을 수 없습니다: $userAccountId")
        val role = roleRepository.findById(roleId)
            ?: throw NotFoundException(errorCode = "ROLE_NOT_FOUND", message = "Role을 찾을 수 없습니다: $roleId")

        role.validateNotSystem()

        userAccountRoleRepository.deleteByUserAccountIdAndRoleId(userAccountId, roleId, actorEmploymentId, now)

        val event = UserRoleRevokedEvent(
            userAccountId = requireNotNull(userAccount.id),
            companyIdValue = userAccount.companyId,
            employmentId = userAccount.employmentId,
            email = userAccount.email,
            twoFactorEnabled = userAccount.twoFactorEnabled,
            lockedUntil = userAccount.lockedUntil,
            roleId = roleId,
            roleCode = role.code,
            actorEmploymentId = actorEmploymentId,
            occurredAt = now,
        )
        eventPublisher.publish(event)
    }

    fun findUserRoles(userAccountId: Long): List<Role> {
        val userAccountRoles = userAccountRoleRepository.findByUserAccountId(userAccountId)
        return userAccountRoles.mapNotNull { roleRepository.findById(it.roleId) }
    }

    fun findAllRoles(companyId: Long): List<Role> =
        roleRepository.findAllByCompanyId(companyId)

    private fun validateRoleNotAlreadyAssigned(userAccountId: Long, roleId: Long, roleCode: String) {
        val existing = userAccountRoleRepository.findByUserAccountIdAndRoleId(userAccountId, roleId)
        if (existing != null) {
            throw BusinessException(
                errorCode = "ROLE_ALREADY_ASSIGNED",
                message = "이미 부여된 역할입니다: $roleCode",
            )
        }
    }
}
