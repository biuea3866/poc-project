package com.hrplatform.auth.infrastructure.persistence.token

import com.hrplatform.auth.domain.token.ApiToken
import com.hrplatform.auth.domain.token.ApiTokenRepository
import org.springframework.stereotype.Repository

@Repository
class ApiTokenRepositoryImpl(
    private val jpaRepository: ApiTokenJpaRepository,
) : ApiTokenRepository {

    override fun save(apiToken: ApiToken): ApiToken =
        jpaRepository.save(apiToken)

    override fun findById(id: Long): ApiToken? =
        jpaRepository.findById(id).orElse(null)

    override fun findByTokenHash(tokenHash: String): ApiToken? =
        jpaRepository.findByTokenHash(tokenHash)

    override fun findByUserAccountId(userAccountId: Long): List<ApiToken> =
        jpaRepository.findByUserAccountIdAndDeletedAtIsNull(userAccountId)
}
