package com.closet.display.application.service

import com.closet.display.application.dto.RankingResponse
import com.closet.display.domain.entity.RankingSnapshot
import com.closet.display.domain.enums.PeriodType
import com.closet.display.domain.repository.RankingSnapshotRepository
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class RankingService(
    private val rankingSnapshotRepository: RankingSnapshotRepository,
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val RANKING_KEY_PREFIX = "ranking"
        private const val SALES_WEIGHT = 0.5
        private const val REVIEW_WEIGHT = 0.3
        private const val VIEW_WEIGHT = 0.2
    }

    /**
     * 랭킹 업데이트 (Redis ZSET + DB 스냅샷 저장)
     * Score = 판매량 x 0.5 + 리뷰수 x 0.3 + 조회수 x 0.2
     */
    @Transactional
    fun updateRanking(
        categoryId: Long,
        periodType: PeriodType,
        // productId -> (salesCount, reviewCount, viewCount)
        productScores: Map<Long, Triple<Long, Long, Long>>,
    ) {
        val redisKey = buildRedisKey(categoryId, periodType)
        val now = ZonedDateTime.now()

        val scores =
            productScores.map { (productId, counts) ->
                val (salesCount, reviewCount, viewCount) = counts
                val score = salesCount * SALES_WEIGHT + reviewCount * REVIEW_WEIGHT + viewCount * VIEW_WEIGHT
                productId to score
            }.sortedByDescending { it.second }

        // Redis ZSET 업데이트
        val ops = redisTemplate.opsForZSet()
        redisTemplate.delete(redisKey)
        scores.forEach { (productId, score) ->
            ops.add(redisKey, productId.toString(), score)
        }

        // DB 스냅샷 저장
        val snapshots =
            scores.mapIndexed { index, (productId, score) ->
                RankingSnapshot(
                    categoryId = categoryId,
                    productId = productId,
                    rankPosition = index + 1,
                    score = score,
                    periodType = periodType,
                    snapshotDate = now,
                )
            }
        rankingSnapshotRepository.saveAll(snapshots)
        logger.info { "랭킹 업데이트 완료: categoryId=$categoryId, periodType=$periodType, count=${scores.size}" }
    }

    /**
     * 랭킹 조회 (Redis 우선, fallback DB)
     */
    fun getRanking(
        categoryId: Long,
        periodType: PeriodType,
        limit: Int,
    ): List<RankingResponse> {
        val redisKey = buildRedisKey(categoryId, periodType)
        val ops = redisTemplate.opsForZSet()

        // Redis에서 조회 (score 내림차순)
        val redisResults = ops.reverseRangeWithScores(redisKey, 0, (limit - 1).toLong())

        if (!redisResults.isNullOrEmpty()) {
            return redisResults.mapIndexed { index, typedTuple ->
                RankingResponse(
                    id = 0,
                    categoryId = categoryId,
                    productId = typedTuple.value!!.toLong(),
                    rankPosition = index + 1,
                    score = typedTuple.score ?: 0.0,
                    periodType = periodType,
                    snapshotDate = ZonedDateTime.now(),
                )
            }
        }

        // Fallback: DB에서 조회
        return rankingSnapshotRepository
            .findByCategoryIdAndPeriodTypeOrderBySnapshotDateDescRankPositionAsc(categoryId, periodType)
            .take(limit)
            .map { RankingResponse.from(it) }
    }

    private fun buildRedisKey(
        categoryId: Long,
        periodType: PeriodType,
    ): String {
        return "$RANKING_KEY_PREFIX:$categoryId:${periodType.name}"
    }
}
