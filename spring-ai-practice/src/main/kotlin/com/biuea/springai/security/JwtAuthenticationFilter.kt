package com.biuea.springai.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * `Authorization: Bearer <jwt>` 헤더에서 JWT 를 추출하고 SecurityContext 에 인증 객체를 채운다.
 *
 * - 헤더 없음 → 그대로 통과 (permitAll 경로는 정상, 보호 경로는 EntryPoint 가 401 반환)
 * - 헤더 있음 + 검증 실패 → 401 + JSON 본문 응답 후 체인 중단
 * - 헤더 있음 + 검증 성공 → SimpleGrantedAuthority("SCOPE_<scope>") 부여
 */
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }
        val token = header.removePrefix("Bearer ").trim()
        val parsed = try {
            jwtService.parse(token)
        } catch (e: InvalidJwtException) {
            writeError(response, 401, "invalid_token", e.message ?: "invalid token")
            return
        }
        val authorities = parsed.scopes.map { SimpleGrantedAuthority("SCOPE_$it") }
        val authentication = UsernamePasswordAuthenticationToken(parsed.subject, null, authorities).apply {
            details = WebAuthenticationDetailsSource().buildDetails(request)
        }
        SecurityContextHolder.getContext().authentication = authentication
        filterChain.doFilter(request, response)
    }

    private fun writeError(response: HttpServletResponse, status: Int, code: String, message: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(mapOf("error" to code, "message" to message)))
    }
}
