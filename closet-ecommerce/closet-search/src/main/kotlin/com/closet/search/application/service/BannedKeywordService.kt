package com.closet.search.application.service

import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 금칙어 관리 서비스 (PD-39).
 *
 * Redis Set으로 금칙어를 관리하며, 로컬 캐시를 통해 조회 성능을 최적화한다.
 * 관리자 CRUD API를 제공한다.
 */
@Service
class BannedKeywordService(
    private val redisTemplate: StringRedisTemplate,
) {

    companion object {
        private const val BANNED_KEY = "search:banned_keywords"
    }

    private val localCache = ConcurrentHashMap.newKeySet<String>()
    @Volatile
    private var cacheLoaded = false

    /**
     * 금칙어 여부 확인.
     */
    fun isBanned(keyword: String): Boolean {
        ensureCacheLoaded()
        return localCache.contains(keyword.trim().lowercase())
    }

    /**
     * 금칙어 추가.
     */
    fun addBannedKeyword(keyword: String) {
        val trimmed = keyword.trim().lowercase()
        if (trimmed.isBlank()) return

        redisTemplate.opsForSet().add(BANNED_KEY, trimmed)
        localCache.add(trimmed)
        logger.info { "금칙어 추가: $trimmed" }
    }

    /**
     * 금칙어 삭제.
     */
    fun removeBannedKeyword(keyword: String) {
        val trimmed = keyword.trim().lowercase()
        redisTemplate.opsForSet().remove(BANNED_KEY, trimmed)
        localCache.remove(trimmed)
        logger.info { "금칙어 삭제: $trimmed" }
    }

    /**
     * 금칙어 목록 조회.
     */
    fun getAllBannedKeywords(): Set<String> {
        return redisTemplate.opsForSet().members(BANNED_KEY) ?: emptySet()
    }

    /**
     * 로컬 캐시 갱신.
     */
    fun refreshCache() {
        val keywords = redisTemplate.opsForSet().members(BANNED_KEY) ?: emptySet()
        localCache.clear()
        localCache.addAll(keywords)
        cacheLoaded = true
        logger.info { "금칙어 캐시 갱신 완료: ${keywords.size}건" }
    }

    private fun ensureCacheLoaded() {
        if (!cacheLoaded) {
            synchronized(this) {
                if (!cacheLoaded) {
                    refreshCache()
                }
            }
        }
    }
}
