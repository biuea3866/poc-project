package com.hrplatform.auth.infrastructure.persistence.token

import com.hrplatform.auth.domain.token.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenJpaRepository :
    JpaRepository<RefreshToken, Long>,
    RefreshTokenCustomRepository {

    fun findByTokenHash(tokenHash: String): RefreshToken?
}
