package com.hrplatform.auth.application.role

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.role.Role
import com.hrplatform.auth.domain.role.RoleRepository
import com.hrplatform.auth.domain.role.UserAccountRole
import com.hrplatform.auth.domain.role.UserAccountRoleRepository
import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.core.exception.BusinessException
import com.hrplatform.core.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class RoleDomainServiceTest : BehaviorSpec({

    fun buildUserAccount(id: Long = 1L): UserAccount {
        val account = UserAccount.create(
            employmentId = 100L,
            companyId = 1L,
            email = "test@example.com",
            passwordHash = "hash",
        )
        val idField = UserAccount::class.java.superclass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(account, id)
        return account
    }

    fun buildRole(id: Long = 10L, code: String = "EMPLOYEE", isSystem: Boolean = false): Role {
        val role = Role(companyId = 0L, code = code, name = code, description = null, isSystemRole = isSystem)
        val idField = Role::class.java.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(role, id)
        return role
    }

    given("assignRole — 정상 케이스") {
        then("유효한 userAccountId, roleId로 assignRole 호출 시 UserAccountRole이 저장되고 이벤트가 발행된다") {
            val userAccount = buildUserAccount()
            val role = buildRole()

            val userAccountRepository = mockk<UserAccountRepository>()
            val roleRepository = mockk<RoleRepository>()
            val userAccountRoleRepository = mockk<UserAccountRoleRepository>()
            val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)

            every { userAccountRepository.findById(1L) } returns userAccount
            every { roleRepository.findById(10L) } returns role
            every { userAccountRoleRepository.findByUserAccountIdAndRoleId(1L, 10L) } returns null
            every { userAccountRoleRepository.save(any()) } answers { firstArg() }

            val service = RoleDomainService(roleRepository, userAccountRoleRepository, userAccountRepository, eventPublisher)

            service.assignRole(1L, 10L, null)

            verify { userAccountRoleRepository.save(any()) }
            verify { eventPublisher.publish(any()) }
        }
    }

    given("assignRole — 이미 부여된 역할") {
        then("이미 부여된 역할을 재부여하면 BusinessException이 발생한다") {
            val userAccount = buildUserAccount()
            val role = buildRole()
            val existingRole = UserAccountRole(userAccountId = 1L, roleId = 10L, assignedAt = ZonedDateTime.now(), assignedBy = null)

            val userAccountRepository = mockk<UserAccountRepository>()
            val roleRepository = mockk<RoleRepository>()
            val userAccountRoleRepository = mockk<UserAccountRoleRepository>()
            val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)

            every { userAccountRepository.findById(1L) } returns userAccount
            every { roleRepository.findById(10L) } returns role
            every { userAccountRoleRepository.findByUserAccountIdAndRoleId(1L, 10L) } returns existingRole

            val service = RoleDomainService(roleRepository, userAccountRoleRepository, userAccountRepository, eventPublisher)

            shouldThrow<BusinessException> {
                service.assignRole(1L, 10L, null)
            }
        }
    }

    given("revokeRole — 시스템 역할") {
        then("시스템 역할 revoke 시도 시 BusinessException이 발생한다") {
            val userAccount = buildUserAccount()
            val systemRole = buildRole(code = "ADMIN", isSystem = true)

            val userAccountRepository = mockk<UserAccountRepository>()
            val roleRepository = mockk<RoleRepository>()
            val userAccountRoleRepository = mockk<UserAccountRoleRepository>()
            val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)

            every { userAccountRepository.findById(1L) } returns userAccount
            every { roleRepository.findById(10L) } returns systemRole

            val service = RoleDomainService(roleRepository, userAccountRoleRepository, userAccountRepository, eventPublisher)

            shouldThrow<BusinessException> {
                service.revokeRole(1L, 10L, null)
            }
        }
    }

    given("findUserRoles") {
        then("userAccountId로 부여된 역할 목록이 반환된다") {
            val role1 = buildRole(id = 10L, code = "EMPLOYEE")
            val role2 = buildRole(id = 11L, code = "TEAM_LEAD")
            val mapping1 = UserAccountRole(userAccountId = 1L, roleId = 10L, assignedAt = ZonedDateTime.now(), assignedBy = null)
            val mapping2 = UserAccountRole(userAccountId = 1L, roleId = 11L, assignedAt = ZonedDateTime.now(), assignedBy = null)

            val roleRepository = mockk<RoleRepository>()
            val userAccountRoleRepository = mockk<UserAccountRoleRepository>()

            every { userAccountRoleRepository.findByUserAccountId(1L) } returns listOf(mapping1, mapping2)
            every { roleRepository.findById(10L) } returns role1
            every { roleRepository.findById(11L) } returns role2

            val service = RoleDomainService(roleRepository, userAccountRoleRepository, mockk(), mockk(relaxed = true))

            val roles = service.findUserRoles(1L)
            roles.size shouldBe 2
            roles.map { it.code }.toSet() shouldBe setOf("EMPLOYEE", "TEAM_LEAD")
        }
    }
})
