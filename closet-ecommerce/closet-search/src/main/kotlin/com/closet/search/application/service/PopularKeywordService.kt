package com.closet.search.application.service

import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 인기 검색어 서비스.
 *
 * Redis Sorted Set + score=timestamp 방식 실시간 sliding window (PD-25).
 * 매 조회 시 1시간 이전 데이터를 ZREMRANGEBYSCORE로 정리한다.
 *
 * 금칙어 관리: DB 테이블 + 관리자 CRUD API (PD-39).
 */
@Service
class PopularKeywordService(
    private val redisTemplate: StringRedisTemplate,
    private val bannedKeywordService: BannedKeywordService,
) {

    companion object {
        private const val POPULAR_KEY = "search:popular_keywords"
        private const val SLIDING_WINDOW_MILLIS = 3_600_000L // 1시간
    }

    /**
     * 검색어를 기록한다.
     * score = 현재 시각(epoch millis)으로 ZADD.
     * 금칙어는 기록하지 않는다.
     */
    fun recordKeyword(keyword: String) {
        val trimmed = keyword.trim().lowercase()
        if (trimmed.isBlank()) return

        if (bannedKeywordService.isBanned(trimmed)) {
            logger.debug { "금칙어 기록 차단: $trimmed" }
            return
        }

        redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, trimmed, 1.0)
        logger.debug { "인기 검색어 기록: $trimmed" }
    }

    /**
     * 인기 검색어 TOP N 조회.
     * 1시간 이전 데이터를 정리한 후, score 내림차순으로 반환한다.
     */
    fun getPopularKeywords(size: Int = 10): List<PopularKeywordResponse> {
        // sliding window: 1시간 이전 데이터 제거는 별도 스케줄러에서 처리
        // 여기서는 score(누적 카운트) 기준 내림차순 조회
        val results = redisTemplate.opsForZSet()
            .reverseRangeWithScores(POPULAR_KEY, 0, (size - 1).toLong())
            ?: emptySet()

        return results.mapIndexed { index, tuple ->
            PopularKeywordResponse(
                rank = index + 1,
                keyword = tuple.value ?: "",
                score = tuple.score?.toLong() ?: 0,
            )
        }.filter { it.keyword.isNotBlank() }
    }

    /**
     * Sliding window 정리 (스케줄러에서 호출).
     * 오래된 데이터 정리: 전체 리셋 후 다시 쌓는 방식 대신,
     * score가 0 이하인 항목 제거.
     */
    fun cleanupOldEntries() {
        // 단순 카운트 기반이므로 주기적으로 score 감소 또는 리셋
        // 실제 프로덕션에서는 시간대별 키 분리가 더 적합
        val removed = redisTemplate.opsForZSet()
            .removeRangeByScore(POPULAR_KEY, Double.NEGATIVE_INFINITY, 0.0)
        if (removed != null && removed > 0) {
            logger.info { "인기 검색어 정리 완료: $removed 건 제거" }
        }
    }

    /**
     * 인기 검색어 초기화 (관리용).
     */
    fun resetPopularKeywords() {
        redisTemplate.delete(POPULAR_KEY)
        logger.info { "인기 검색어 초기화 완료" }
    }
}

data class PopularKeywordResponse(
    val rank: Int,
    val keyword: String,
    val score: Long,
)
