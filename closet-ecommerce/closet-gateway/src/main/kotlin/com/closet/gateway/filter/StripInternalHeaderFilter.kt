package com.closet.gateway.filter

import mu.KotlinLogging
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

/**
 * 외부 요청에서 X-Internal-Api-Key 헤더를 제거하는 Gateway GlobalFilter.
 *
 * 외부 클라이언트가 X-Internal-Api-Key를 조작하여 내부 전용 API에
 * 접근하는 것을 방지한다. Gateway를 통과하는 모든 요청에서
 * 해당 헤더를 무조건 제거한다.
 *
 * 우선순위: 최상위 (order = -10) — 다른 모든 필터보다 먼저 실행
 */
@Component
class StripInternalHeaderFilter : GlobalFilter, Ordered {
    companion object {
        const val HEADER_INTERNAL_API_KEY = "X-Internal-Api-Key"
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val request = exchange.request

        // X-Internal-Api-Key 헤더가 있으면 제거
        if (request.headers.containsKey(HEADER_INTERNAL_API_KEY)) {
            logger.warn {
                "[Gateway] Stripped X-Internal-Api-Key header from external request: ${request.method} ${request.uri.path}"
            }
            val mutatedRequest =
                request.mutate()
                    .headers { it.remove(HEADER_INTERNAL_API_KEY) }
                    .build()
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
        }

        return chain.filter(exchange)
    }

    override fun getOrder(): Int = -10
}
