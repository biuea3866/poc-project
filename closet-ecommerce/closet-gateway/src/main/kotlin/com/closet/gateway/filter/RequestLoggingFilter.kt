package com.closet.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class RequestLoggingFilter : GlobalFilter, Ordered {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        log.info(
            "[Gateway] {} {} -> {}",
            request.method,
            request.uri.path,
            exchange.getAttribute<Route>(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)?.id ?: "unknown"
        )
        val start = System.currentTimeMillis()
        return chain.filter(exchange).then(Mono.fromRunnable {
            log.info(
                "[Gateway] {} {} -> {} ({}ms)",
                request.method,
                request.uri.path,
                exchange.response.statusCode,
                System.currentTimeMillis() - start
            )
        })
    }

    override fun getOrder(): Int = -2
}
