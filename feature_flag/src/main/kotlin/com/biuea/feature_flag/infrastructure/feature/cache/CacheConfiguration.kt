package com.biuea.feature_flag.infrastructure.feature.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration {

    @Bean
    fun cacheManager(): CacheManager {
        val caffeine = Caffeine.newBuilder()
            .recordStats()
            .expireAfterWrite(Duration.ofHours(1))
            .initialCapacity(10)
            .maximumSize(1000)

        val manager = CaffeineCacheManager()
            .apply {
                this.setCaffeine(caffeine)
                this.setAsyncCacheMode(true)
            }

        return manager
    }
}