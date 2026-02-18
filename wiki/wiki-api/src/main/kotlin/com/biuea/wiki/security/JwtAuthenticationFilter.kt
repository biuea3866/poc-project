package com.biuea.wiki.security

import com.biuea.wiki.domain.auth.JwtTokenProvider
import com.biuea.wiki.infrastructure.auth.RedisAuthTokenStore
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisAuthTokenStore: RedisAuthTokenStore,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = jwtTokenProvider.resolveToken(request.getHeader(HttpHeaders.AUTHORIZATION))

        if (token != null && !redisAuthTokenStore.isAccessTokenBlacklisted(token) && jwtTokenProvider.validateToken(token)) {
            val authentication = jwtTokenProvider.getAuthentication(token)
            if (authentication != null) {
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
