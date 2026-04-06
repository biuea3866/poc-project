package com.closet.member.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 인증 필터 (Spring Security 없이 수동 처리)
 * - Authorization 헤더에서 Bearer 토큰 추출
 * - 유효한 토큰이면 request attribute에 memberId 설정
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {
    companion object {
        const val MEMBER_ID_ATTRIBUTE = "memberId"
        private const val BEARER_PREFIX = "Bearer "

        /** 인증이 필요 없는 경로 */
        private val PERMIT_ALL_PATHS =
            listOf(
                "/api/v1/members/register",
                "/api/v1/members/login",
                "/api/v1/auth/refresh",
            )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = request.requestURI

        // 인증 불필요 경로는 바로 통과
        if (PERMIT_ALL_PATHS.any { path.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val token = resolveToken(request)

        if (token != null && jwtTokenProvider.validate(token)) {
            val memberId = jwtTokenProvider.extractMemberId(token)
            request.setAttribute(MEMBER_ID_ATTRIBUTE, memberId)
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"success":false,"error":{"code":"C004","message":"인증이 필요합니다"}}""")
        }
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null
        return if (bearer.startsWith(BEARER_PREFIX)) {
            bearer.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
