package com.example.cachepractice.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.util.concurrent.TimeUnit

/**
 * Caffeine 캐시 설정
 *
 * Caffeine 특징:
 * - Window TinyLfu 제거 정책: 고급 캐시 제거 알고리즘으로 높은 적중률
 * - 비동기 로딩: 캐시 스템피드 방지를 위한 자동 잠금 메커니즘
 * - 고성능: 동시성에 최적화된 설계
 */
@Configuration
@EnableCaching
@Profile("caffeine", "default")
class CaffeineCacheConfig {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val PRODUCT_CACHE = "products"
        const val PRODUCT_LIST_CACHE = "productList"
        const val LOOK_ASIDE_PRODUCTS = "lookAsideProducts"
    }

    /**
     * Caffeine 기반 CacheManager 생성
     *
     * 설정:
     * - maximumSize: 최대 1000개 항목
     * - expireAfterWrite: 쓰기 후 10분 후 만료
     * - recordStats: 캐시 통계 수집 (모니터링용)
     */
    @Bean
    @Primary
    fun caffeineCacheManager(): CacheManager {
        logger.info("Caffeine CacheManager 초기화")

        val caffeine = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats() // 캐시 통계 활성화

        val cacheManager = CaffeineCacheManager()
        cacheManager.setCaffeine(caffeine)
        cacheManager.setCacheNames(listOf(PRODUCT_CACHE, PRODUCT_LIST_CACHE, LOOK_ASIDE_PRODUCTS))

        return cacheManager
    }
}
