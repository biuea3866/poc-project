package com.closet.search.application.service

import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 인기 검색어 서비스 (US-705).
 *
 * Redis Sorted Set 기반 실시간 인기 검색어 관리.
 * ZINCRBY로 검색 시 카운트를 누적하고, Top 10을 score 내림차순으로 반환한다.
 *
 * 순위 변동 표시:
 * - 이전 스냅샷과 비교하여 NEW / UP / DOWN / SAME 표시
 * - 1시간 주기로 스냅샷을 갱신하며 sliding window를 적용한다.
 *
 * 금칙어 관리: BannedKeywordService를 통해 금칙어를 필터링한다.
 */
@Service
class PopularKeywordService(
    private val redisTemplate: StringRedisTemplate,
    private val bannedKeywordService: BannedKeywordService,
) {
    companion object {
        /** 현재 인기 검색어 카운트 키 */
        private const val POPULAR_KEY = "search:popular_keywords"

        /** 이전 스냅샷 (순위 변동 비교용) 키 */
        private const val PREVIOUS_SNAPSHOT_KEY = "search:popular_keywords:previous"

        /** 기본 Top N 개수 */
        private const val DEFAULT_TOP_SIZE = 10
    }

    /**
     * 검색어를 기록한다.
     * ZINCRBY로 score를 1.0씩 증가시킨다.
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
     * 인기 검색어 TOP N 조회 (순위 변동 포함).
     *
     * 현재 score 기준 내림차순으로 조회하고,
     * 이전 스냅샷과 비교하여 순위 변동(RankChange)을 계산한다.
     */
    fun getPopularKeywords(size: Int = DEFAULT_TOP_SIZE): List<PopularKeywordResponse> {
        val currentResults =
            redisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULAR_KEY, 0, (size - 1).toLong())
                ?: emptySet()

        // 이전 스냅샷 조회 (키워드 → 순위 맵)
        val previousRanks = getPreviousRanks()

        return currentResults.mapIndexed { index, tuple ->
            val keyword = tuple.value ?: ""
            val currentRank = index + 1
            val previousRank = previousRanks[keyword]

            val rankChange =
                when {
                    keyword.isBlank() -> RankChange.SAME
                    previousRank == null -> RankChange.NEW
                    previousRank > currentRank -> RankChange.UP
                    previousRank < currentRank -> RankChange.DOWN
                    else -> RankChange.SAME
                }

            PopularKeywordResponse(
                rank = currentRank,
                keyword = keyword,
                score = tuple.score?.toLong() ?: 0,
                rankChange = rankChange,
            )
        }.filter { it.keyword.isNotBlank() }
    }

    /**
     * 이전 스냅샷에서 키워드별 순위 맵을 가져온다.
     */
    private fun getPreviousRanks(): Map<String, Int> {
        val previousResults =
            redisTemplate.opsForZSet()
                .reverseRangeWithScores(PREVIOUS_SNAPSHOT_KEY, 0, (DEFAULT_TOP_SIZE - 1).toLong())
                ?: emptySet()

        return previousResults.mapIndexed { index, tuple ->
            (tuple.value ?: "") to (index + 1)
        }.toMap()
    }

    /**
     * 1시간 주기 스냅샷 갱신 (sliding window).
     *
     * 현재 인기 검색어를 이전 스냅샷으로 복사한 후, 현재 카운트를 초기화한다.
     * 이를 통해 최근 1시간 동안의 인기 검색어만 반영한다.
     */
    @Scheduled(fixedRate = 3_600_000) // 1시간
    fun refreshSnapshot() {
        // 현재 TOP N을 이전 스냅샷으로 복사
        val currentTopN =
            redisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULAR_KEY, 0, (DEFAULT_TOP_SIZE - 1).toLong())
                ?: emptySet()

        // 이전 스냅샷 초기화 후 다시 저장
        redisTemplate.delete(PREVIOUS_SNAPSHOT_KEY)
        currentTopN.forEach { tuple ->
            val keyword = tuple.value ?: return@forEach
            val score = tuple.score ?: return@forEach
            redisTemplate.opsForZSet().add(PREVIOUS_SNAPSHOT_KEY, keyword, score)
        }

        // 현재 카운트 초기화 (sliding window)
        redisTemplate.delete(POPULAR_KEY)

        logger.info { "인기 검색어 스냅샷 갱신 완료: ${currentTopN.size}건 보관" }
    }

    /**
     * Sliding window 정리 (저점수 항목 제거).
     */
    fun cleanupOldEntries() {
        val removed =
            redisTemplate.opsForZSet()
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
        redisTemplate.delete(PREVIOUS_SNAPSHOT_KEY)
        logger.info { "인기 검색어 초기화 완료" }
    }
}

/**
 * 순위 변동 타입.
 */
enum class RankChange {
    /** 신규 진입 */
    NEW,

    /** 순위 상승 */
    UP,

    /** 순위 하락 */
    DOWN,

    /** 순위 유지 */
    SAME,
}

/**
 * 인기 검색어 응답 DTO (US-705).
 */
data class PopularKeywordResponse(
    val rank: Int,
    val keyword: String,
    val score: Long,
    val rankChange: RankChange = RankChange.SAME,
)
