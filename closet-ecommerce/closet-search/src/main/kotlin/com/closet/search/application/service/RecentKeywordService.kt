package com.closet.search.application.service

import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * 최근 검색어 서비스 (PD-48).
 *
 * Redis List 타입으로 search:recent:{memberId} 키 관리.
 * 최대 20개 유지, 중복 제거, 30일 TTL.
 */
@Service
class RecentKeywordService(
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val KEY_PREFIX = "search:recent:"
        private const val MAX_SIZE = 20L
        private val TTL = Duration.ofDays(30)
    }

    /**
     * 최근 검색어 저장.
     * 중복 제거 후 최신을 리스트 앞에 추가한다.
     */
    fun saveRecentKeyword(
        memberId: Long,
        keyword: String,
    ) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return

        val key = "$KEY_PREFIX$memberId"

        // 중복 제거
        redisTemplate.opsForList().remove(key, 0, trimmed)

        // 리스트 앞에 추가
        redisTemplate.opsForList().leftPush(key, trimmed)

        // MAX_SIZE 초과분 제거
        redisTemplate.opsForList().trim(key, 0, MAX_SIZE - 1)

        // TTL 갱신
        redisTemplate.expire(key, TTL)

        logger.debug { "최근 검색어 저장: memberId=$memberId, keyword=$trimmed" }
    }

    /**
     * 최근 검색어 목록 조회.
     */
    fun getRecentKeywords(
        memberId: Long,
        size: Int = 20,
    ): List<String> {
        val key = "$KEY_PREFIX$memberId"
        val results = redisTemplate.opsForList().range(key, 0, (size - 1).toLong()) ?: emptyList()
        return results
    }

    /**
     * 특정 최근 검색어 삭제.
     */
    fun deleteRecentKeyword(
        memberId: Long,
        keyword: String,
    ) {
        val key = "$KEY_PREFIX$memberId"
        redisTemplate.opsForList().remove(key, 0, keyword.trim())
        logger.debug { "최근 검색어 삭제: memberId=$memberId, keyword=${keyword.trim()}" }
    }

    /**
     * 최근 검색어 전체 삭제.
     */
    fun deleteAllRecentKeywords(memberId: Long) {
        val key = "$KEY_PREFIX$memberId"
        redisTemplate.delete(key)
        logger.debug { "최근 검색어 전체 삭제: memberId=$memberId" }
    }
}
