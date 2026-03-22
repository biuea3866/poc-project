package com.closet.display.application.service

import com.closet.display.domain.entity.RankingSnapshot
import com.closet.display.domain.enums.PeriodType
import com.closet.display.domain.repository.RankingSnapshotRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import java.time.LocalDateTime

class RankingServiceTest : BehaviorSpec({

    val rankingSnapshotRepository = mockk<RankingSnapshotRepository>()
    val redisTemplate = mockk<StringRedisTemplate>()
    val rankingService = RankingService(rankingSnapshotRepository, redisTemplate)

    Given("Redis에 랭킹 데이터가 없고 DB에 스냅샷이 존재할 때") {
        val now = LocalDateTime.now()
        val snapshots = listOf(
            RankingSnapshot(
                categoryId = 1L,
                productId = 10L,
                rankPosition = 1,
                score = 85.0,
                periodType = PeriodType.DAILY,
                snapshotDate = now
            ),
            RankingSnapshot(
                categoryId = 1L,
                productId = 20L,
                rankPosition = 2,
                score = 72.0,
                periodType = PeriodType.DAILY,
                snapshotDate = now
            ),
            RankingSnapshot(
                categoryId = 1L,
                productId = 30L,
                rankPosition = 3,
                score = 65.0,
                periodType = PeriodType.DAILY,
                snapshotDate = now
            )
        )

        val zSetOps = mockk<ZSetOperations<String, String>>()
        every { redisTemplate.opsForZSet() } returns zSetOps
        every { zSetOps.reverseRangeWithScores("ranking:1:DAILY", 0, 9) } returns emptySet()
        every { rankingSnapshotRepository.findByCategoryIdAndPeriodType(1L, PeriodType.DAILY) } returns snapshots

        When("getRanking을 호출하면") {
            val result = rankingService.getRanking(1L, PeriodType.DAILY, 10)

            Then("DB 스냅샷 기반으로 랭킹을 반환한다") {
                result.size shouldBe 3
                result[0].productId shouldBe 10L
                result[0].rankPosition shouldBe 1
                result[0].score shouldBe 85.0
                result[1].productId shouldBe 20L
                result[1].rankPosition shouldBe 2
                result[2].productId shouldBe 30L
                result[2].rankPosition shouldBe 3
            }
        }
    }

    Given("Redis에 랭킹 데이터가 존재할 때") {
        val zSetOps = mockk<ZSetOperations<String, String>>()
        every { redisTemplate.opsForZSet() } returns zSetOps

        val typedTuple1 = mockk<ZSetOperations.TypedTuple<String>>()
        every { typedTuple1.value } returns "10"
        every { typedTuple1.score } returns 95.0

        val typedTuple2 = mockk<ZSetOperations.TypedTuple<String>>()
        every { typedTuple2.value } returns "20"
        every { typedTuple2.score } returns 80.0

        every { zSetOps.reverseRangeWithScores("ranking:1:WEEKLY", 0, 4) } returns
            linkedSetOf(typedTuple1, typedTuple2)

        When("getRanking을 호출하면") {
            val result = rankingService.getRanking(1L, PeriodType.WEEKLY, 5)

            Then("Redis 기반으로 랭킹을 반환한다") {
                result.size shouldBe 2
                result[0].productId shouldBe 10L
                result[0].rankPosition shouldBe 1
                result[0].score shouldBe 95.0
                result[1].productId shouldBe 20L
                result[1].rankPosition shouldBe 2
                result[1].score shouldBe 80.0
            }
        }
    }
})
