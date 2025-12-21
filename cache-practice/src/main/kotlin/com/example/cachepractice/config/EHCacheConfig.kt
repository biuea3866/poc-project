package com.example.cachepractice.config

import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.jcache.JCacheCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.cache.Caching
import javax.cache.spi.CachingProvider

/**
 * EHCache 설정
 *
 * EHCache 특징:
 * - JSR-107 (JCache) 표준 구현
 * - 힙 메모리, 오프힙 메모리, 디스크 계층 지원
 * - LRU, LFU 등 다양한 제거 정책
 * - 분산 캐시 지원 (Terracotta 서버 사용 시)
 *
 * Caffeine과의 차이:
 * - Caffeine: Window TinyLfu (적중률 최적화)
 * - EHCache: 다양한 계층 지원, 표준 준수
 */
@Configuration
@EnableCaching
@Profile("ehcache")
class EHCacheConfig {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * EHCache 기반 CacheManager 생성
     *
     * ehcache.xml 파일에서 설정을 로드
     */
    @Bean
    fun ehCacheCacheManager(): CacheManager {
        logger.info("EHCache CacheManager 초기화")

        val cachingProvider: CachingProvider = Caching.getCachingProvider()
        val cacheManager = cachingProvider.cacheManager

        return JCacheCacheManager(cacheManager)
    }
}
