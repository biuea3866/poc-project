package com.closet.gateway.filter

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import java.util.Date

/**
 * RoleExtractionFilter 단위 테스트.
 */
class RoleExtractionFilterTest {
    private val secret = "closet-member-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256"
    private val filter = RoleExtractionFilter(secret)

    private fun generateToken(
        memberId: Long,
        role: String? = null,
    ): String {
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        val builder =
            Jwts.builder()
                .subject(memberId.toString())
                .claim("type", "access")
                .issuedAt(Date())
                .expiration(Date(System.currentTimeMillis() + 1800000))
                .signWith(key)

        if (role != null) {
            builder.claim("role", role)
        }

        return builder.compact()
    }

    @Test
    fun `JWT에 role=SELLER claim이 있으면 X-Member-Role=SELLER 헤더를 추가한다`() {
        val token = generateToken(1L, "SELLER")
        val request =
            MockServerHttpRequest.get("/api/v1/shippings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .build()
        val exchange = MockServerWebExchange.from(request)

        var resultExchange: org.springframework.web.server.ServerWebExchange? = null
        val chain =
            GatewayFilterChain { ex ->
                resultExchange = ex
                Mono.empty()
            }

        filter.filter(exchange, chain).block()

        assertEquals(
            "SELLER",
            resultExchange!!.request.headers.getFirst(RoleExtractionFilter.HEADER_MEMBER_ROLE),
            "X-Member-Role 헤더에 SELLER가 전달되어야 한다",
        )
    }

    @Test
    fun `JWT에 role claim이 없으면 기본값 BUYER를 전달한다 (레거시 호환)`() {
        val token = generateToken(1L, role = null)
        val request =
            MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .build()
        val exchange = MockServerWebExchange.from(request)

        var resultExchange: org.springframework.web.server.ServerWebExchange? = null
        val chain =
            GatewayFilterChain { ex ->
                resultExchange = ex
                Mono.empty()
            }

        filter.filter(exchange, chain).block()

        assertEquals(
            "BUYER",
            resultExchange!!.request.headers.getFirst(RoleExtractionFilter.HEADER_MEMBER_ROLE),
            "role claim이 없으면 기본값 BUYER여야 한다",
        )
    }

    @Test
    fun `Authorization 헤더가 없으면 X-Member-Role 헤더를 추가하지 않는다`() {
        val request =
            MockServerHttpRequest.get("/api/v1/products")
                .build()
        val exchange = MockServerWebExchange.from(request)

        var resultExchange: org.springframework.web.server.ServerWebExchange? = null
        val chain =
            GatewayFilterChain { ex ->
                resultExchange = ex
                Mono.empty()
            }

        filter.filter(exchange, chain).block()

        assertFalse(
            resultExchange!!.request.headers.containsKey(RoleExtractionFilter.HEADER_MEMBER_ROLE),
            "Authorization이 없으면 X-Member-Role 헤더가 없어야 한다",
        )
    }

    @Test
    fun `JWT에 role=ADMIN claim이 있으면 X-Member-Role=ADMIN을 전달한다`() {
        val token = generateToken(3L, "ADMIN")
        val request =
            MockServerHttpRequest.get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .build()
        val exchange = MockServerWebExchange.from(request)

        var resultExchange: org.springframework.web.server.ServerWebExchange? = null
        val chain =
            GatewayFilterChain { ex ->
                resultExchange = ex
                Mono.empty()
            }

        filter.filter(exchange, chain).block()

        assertEquals(
            "ADMIN",
            resultExchange!!.request.headers.getFirst(RoleExtractionFilter.HEADER_MEMBER_ROLE),
        )
    }

    @Test
    fun `필터 우선순위가 0이다 (JwtAuthenticationFilter 이후 실행)`() {
        assertEquals(0, filter.order, "RoleExtractionFilter의 order는 0이어야 한다")
    }
}
