package com.hrplatform.auth.domain.token

interface ApiTokenRepository {
    fun save(apiToken: ApiToken): ApiToken
    fun findById(id: Long): ApiToken?
    fun findByTokenHash(tokenHash: String): ApiToken?
    fun findByUserAccountId(userAccountId: Long): List<ApiToken>
}
