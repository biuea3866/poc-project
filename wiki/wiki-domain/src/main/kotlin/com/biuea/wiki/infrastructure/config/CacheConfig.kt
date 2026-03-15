package com.biuea.wiki.infrastructure.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager(
            "userDetails",
            "documents",
            "documentList",
            "tags"
        )
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
        )
        return cacheManager
    }
}
