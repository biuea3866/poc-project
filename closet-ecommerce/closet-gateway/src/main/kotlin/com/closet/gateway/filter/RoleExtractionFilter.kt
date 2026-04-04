package com.closet.gateway.filter

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

/**
 * JWT에서 role claim을 추출하여 X-Member-Role 헤더로 downstream 서비스에 전달하는 필터.
 *
 * JwtAuthenticationFilter(order = -1) 이후에 실행되어야 하므로 order = 0.
 * JWT가 없거나 role claim이 없는 레거시 토큰이면 기본값 BUYER를 전달한다.
 */
@Component
class RoleExtractionFilter(
    @Value("\${jwt.secret}") private val secret: String,
) : GlobalFilter, Ordered {

    companion object {
        const val HEADER_MEMBER_ROLE = "X-Member-Role"
        private const val BEARER_PREFIX = "Bearer "
        private const val DEFAULT_ROLE = "BUYER"
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        // Authorization 헤더가 없으면 (public 경로 등) role 없이 통과
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange)
        }

        val token = authHeader.substring(BEARER_PREFIX.length)
        val role = extractRoleFromToken(token)

        val mutatedRequest = request.mutate()
            .header(HEADER_MEMBER_ROLE, role)
            .build()

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
    }

    private fun extractRoleFromToken(token: String): String {
        return try {
            val claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.toByteArray()))
                .build()
                .parseSignedClaims(token)
                .payload

            val role = claims["role"] as? String
            if (role.isNullOrBlank()) {
                logger.debug { "JWT에 role claim이 없습니다. 기본값 BUYER 사용 (레거시 토큰 호환)" }
                DEFAULT_ROLE
            } else {
                role
            }
        } catch (e: Exception) {
            // JWT 검증 실패 시에는 JwtAuthenticationFilter에서 이미 처리하므로
            // 여기서는 기본값만 반환
            logger.debug { "JWT role 추출 실패: ${e.message}" }
            DEFAULT_ROLE
        }
    }

    override fun getOrder(): Int = 0
}
