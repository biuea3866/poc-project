package com.hrplatform.auth.presentation.auth

import com.hrplatform.auth.domain.auth.service.JwtTokenService
import com.hrplatform.auth.domain.token.JtiBlacklist
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService,
    private val jtiBlacklist: JtiBlacklist,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ")

        try {
            val claims = jwtTokenService.verifyAccessToken(token)
            if (jtiBlacklist.contains(claims.jti)) {
                log.warn("blacklisted jti 감지: jti={}", claims.jti)
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                return
            }
            val authentication = JwtAuthenticationToken(
                userAccountId = claims.userAccountId,
                employmentId = claims.employmentId,
            )
            SecurityContextHolder.getContext().authentication = authentication
        } catch (exception: JwtException) {
            log.debug("JWT 검증 실패: {}", exception.message)
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            return
        }

        chain.doFilter(request, response)
    }
}
