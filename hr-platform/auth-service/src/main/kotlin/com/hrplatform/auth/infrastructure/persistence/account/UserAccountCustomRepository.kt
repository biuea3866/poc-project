package com.hrplatform.auth.infrastructure.persistence.account

import com.hrplatform.auth.domain.account.UserAccount

interface UserAccountCustomRepository {
    fun findByEmploymentId(employmentId: Long): UserAccount?
}
