package com.hrplatform.auth.domain.role

import com.hrplatform.core.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "user_account_roles",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_account_id", "role_id"])],
)
class UserAccountRole(
    @Column(name = "user_account_id", nullable = false)
    val userAccountId: Long,

    @Column(name = "role_id", nullable = false)
    val roleId: Long,

    @Column(name = "assigned_at", nullable = false)
    val assignedAt: ZonedDateTime,

    @Column(name = "assigned_by")
    val assignedBy: Long?,
) : BaseEntity()
