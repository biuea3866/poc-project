package com.closet.common.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val logger = KotlinLogging.logger {}

/**
 * 내부 서비스 간 API 호출 보호 필터.
 *
 * X-Internal-Api-Key 헤더 값이 설정된 키와 일치하는지 검증한다.
 * internal.api.enabled=true 일 때만 빈으로 등록된다.
 *
 * 적용 대상: 내부 전용 API 경로 (예: /internal/로 시작하는 모든 경로)
 */
@Component
@ConditionalOnProperty(name = ["internal.api.enabled"], havingValue = "true")
class InternalApiKeyFilter(
    @Value("\${internal.api.key}")
    private val internalApiKey: String,
    @Value("\${internal.api.paths:/internal/**}")
    private val internalPaths: String,
) : OncePerRequestFilter() {
    companion object {
        const val HEADER_INTERNAL_API_KEY = "X-Internal-Api-Key"
    }

    private val pathPatterns: List<String> by lazy {
        internalPaths.split(",").map { it.trim() }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = request.requestURI

        // 내부 API 경로가 아니면 통과
        if (!isInternalPath(path)) {
            filterChain.doFilter(request, response)
            return
        }

        val apiKey = request.getHeader(HEADER_INTERNAL_API_KEY)

        if (apiKey.isNullOrBlank() || apiKey != internalApiKey) {
            logger.warn { "Internal API key validation failed: path=$path" }
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            response.writer.write(
                """{"success":false,"error":{"code":"C004","message":"내부 API 인증이 필요합니다"}}""",
            )
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isInternalPath(path: String): Boolean {
        return pathPatterns.any { pattern ->
            matchPath(pattern, path)
        }
    }

    // Ant 스타일 패턴 매칭 (간소화 버전)
    // /internal/ + ** 패턴은 /internal/xxx, /internal/xxx/yyy 등 매칭
    private fun matchPath(
        pattern: String,
        path: String,
    ): Boolean {
        if (pattern.endsWith("/**")) {
            val prefix = pattern.removeSuffix("/**")
            return path.startsWith(prefix)
        }
        // 단순 prefix 매칭
        return path.startsWith(pattern.replace("**", ""))
    }
}
