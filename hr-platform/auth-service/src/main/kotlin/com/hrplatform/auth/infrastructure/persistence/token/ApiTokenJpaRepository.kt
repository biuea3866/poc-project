package com.hrplatform.auth.infrastructure.persistence.token

import com.hrplatform.auth.domain.token.ApiToken
import org.springframework.data.jpa.repository.JpaRepository

interface ApiTokenJpaRepository : JpaRepository<ApiToken, Long> {
    fun findByTokenHash(tokenHash: String): ApiToken?
    fun findByUserAccountIdAndDeletedAtIsNull(userAccountId: Long): List<ApiToken>
}
