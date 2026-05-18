package com.hrplatform.auth.presentation.auth

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * JWT 인증이 완료된 사용자의 Spring Security Authentication 구현체.
 * principal = userAccountId (Long).
 */
class JwtAuthenticationToken(
    val userAccountId: Long,
    val employmentId: Long?,
    authorities: Collection<GrantedAuthority> = emptyList(),
) : AbstractAuthenticationToken(authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): Long = userAccountId
}
