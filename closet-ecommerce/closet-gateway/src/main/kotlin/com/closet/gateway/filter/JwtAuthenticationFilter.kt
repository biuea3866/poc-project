package com.closet.gateway.filter

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(
    @Value("\${jwt.secret}") private val secret: String
) : GlobalFilter, Ordered {

    private val publicPaths = listOf(
        "/api/v1/members/register",
        "/api/v1/members/login",
        "/api/v1/members/auth/refresh",
        "/api/v1/products",
        "/api/v1/categories",
        "/api/v1/brands",
        "/api/v1/bff/products",
        "/api/v1/bff/auth",
    )

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.uri.path
        val method = request.method

        // Public endpoints skip auth
        if (isPublicPath(path, method)) {
            return chain.filter(exchange)
        }

        // Extract and validate JWT
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?: return onError(exchange, "Authorization header missing", HttpStatus.UNAUTHORIZED)

        if (!authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Invalid authorization format", HttpStatus.UNAUTHORIZED)
        }

        val token = authHeader.substring(7)
        return try {
            val claims = validateToken(token)
            val memberId = claims.subject

            // Forward memberId to downstream services
            val mutatedRequest = request.mutate()
                .header("X-Member-Id", memberId)
                .build()
            chain.filter(exchange.mutate().request(mutatedRequest).build())
        } catch (e: Exception) {
            onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED)
        }
    }

    private fun validateToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secret.toByteArray()))
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun isPublicPath(path: String, method: HttpMethod?): Boolean {
        if (publicPaths.any { path.startsWith(it) }) {
            // For product/category/brand/bff listings, only GET is public
            if (path.startsWith("/api/v1/products") ||
                path.startsWith("/api/v1/categories") ||
                path.startsWith("/api/v1/brands") ||
                path.startsWith("/api/v1/bff/products")
            ) {
                return method == HttpMethod.GET
            }
            return true
        }
        return false
    }

    private fun onError(exchange: ServerWebExchange, message: String, status: HttpStatus): Mono<Void> {
        exchange.response.statusCode = status
        exchange.response.headers.contentType = MediaType.APPLICATION_JSON
        val body = """{"success":false,"error":{"code":"AUTH001","message":"$message"}}"""
        val buffer = exchange.response.bufferFactory().wrap(body.toByteArray())
        return exchange.response.writeWith(Mono.just(buffer))
    }

    override fun getOrder(): Int = -1
}
