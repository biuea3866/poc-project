package com.biuea.springai.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

/**
 * 인증 주체(JWT subject) 별 Resilience4j RateLimiter. 인증되지 않은 요청은 client IP 기준.
 *
 * - 동일 key 호출은 RateLimiterRegistry 가 캐싱한 RateLimiter 인스턴스를 재사용한다.
 * - 슬롯이 없으면 즉시 429 응답 (timeoutMillis=0 권장).
 */
@Component
class RateLimitFilter(
    private val properties: RateLimitProperties,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    private val registry: RateLimiterRegistry = RateLimiterRegistry.of(
        RateLimiterConfig.custom()
            .limitForPeriod(properties.limitForPeriod)
            .limitRefreshPeriod(Duration.ofSeconds(properties.refreshPeriodSeconds))
            .timeoutDuration(Duration.ofMillis(properties.timeoutMillis))
            .build(),
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Actuator 헬스체크는 제한 대상에서 제외
        return request.requestURI.startsWith("/actuator")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val key = resolveKey(request)
        val limiter = registry.rateLimiter(key)
        val acquired = limiter.acquirePermission()
        if (!acquired) {
            val nanos = limiter.reservePermission().coerceAtLeast(0)
            val retryAfterSeconds = (nanos / 1_000_000_000).coerceAtLeast(1)
            response.status = 429
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            response.setHeader("Retry-After", retryAfterSeconds.toString())
            response.writer.write(
                objectMapper.writeValueAsString(
                    mapOf(
                        "error" to "rate_limited",
                        "message" to "too many requests",
                        "retryAfterSeconds" to retryAfterSeconds,
                    ),
                ),
            )
            return
        }
        val metrics = limiter.metrics
        response.setHeader("X-RateLimit-Remaining", metrics.availablePermissions.toString())
        filterChain.doFilter(request, response)
    }

    private fun resolveKey(request: HttpServletRequest): String {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null && auth.isAuthenticated && auth.principal != "anonymousUser") {
            return "sub:${auth.principal}"
        }
        return "ip:${request.remoteAddr}"
    }
}
