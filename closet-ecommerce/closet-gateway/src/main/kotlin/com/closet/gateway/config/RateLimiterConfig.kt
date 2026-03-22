package com.closet.gateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
class RateLimiterConfig {
    @Bean
    fun userKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val ip = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
            Mono.just(ip)
        }
    }
}
