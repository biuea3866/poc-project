package com.hrplatform.auth.domain.role

import com.hrplatform.core.domain.BaseEntity
import com.hrplatform.core.exception.BusinessException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "roles")
class Role(
    @Column(name = "company_id")
    val companyId: Long?,

    @Column(nullable = false, length = 50)
    val code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "is_system_role", nullable = false)
    val isSystemRole: Boolean,
) : BaseEntity() {

    fun validateNotSystem() {
        if (isSystemRole) {
            throw BusinessException(
                errorCode = "SYSTEM_ROLE_MODIFICATION_FORBIDDEN",
                message = "시스템 역할은 수정할 수 없습니다: $code",
            )
        }
    }

    fun updateInfo(name: String, description: String?) {
        validateNotSystem()
        this.name = name
        this.description = description
    }
}
