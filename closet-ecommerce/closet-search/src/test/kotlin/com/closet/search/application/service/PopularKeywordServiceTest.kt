package com.closet.search.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ZSetOperations

class PopularKeywordServiceTest : BehaviorSpec({

    val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    val bannedKeywordService = mockk<BannedKeywordService>()
    val zSetOps = mockk<ZSetOperations<String, String>>(relaxed = true)

    every { redisTemplate.opsForZSet() } returns zSetOps
    every { bannedKeywordService.isBanned(any()) } returns false

    val service =
        PopularKeywordService(
            redisTemplate = redisTemplate,
            bannedKeywordService = bannedKeywordService,
        )

    Given("인기 검색어 기록 시") {

        When("일반 검색어를 기록하면") {
            service.recordKeyword("맨투맨")

            Then("Redis ZINCRBY로 score가 1 증가한다") {
                verify(exactly = 1) {
                    zSetOps.incrementScore("search:popular_keywords", "맨투맨", 1.0)
                }
            }
        }

        When("빈 문자열을 기록하면") {
            service.recordKeyword("   ")

            Then("기록하지 않는다") {
                verify(exactly = 0) {
                    zSetOps.incrementScore("search:popular_keywords", "", any())
                }
            }
        }

        When("금칙어를 기록하면") {
            every { bannedKeywordService.isBanned("욕설") } returns true
            service.recordKeyword("욕설")

            Then("기록하지 않는다 (금칙어 차단)") {
                verify(exactly = 0) {
                    zSetOps.incrementScore("search:popular_keywords", "욕설", any())
                }
            }
        }
    }

    Given("인기 검색어 조회 시") {

        val typedTuple1 = mockk<ZSetOperations.TypedTuple<String>>()
        every { typedTuple1.value } returns "맨투맨"
        every { typedTuple1.score } returns 100.0

        val typedTuple2 = mockk<ZSetOperations.TypedTuple<String>>()
        every { typedTuple2.value } returns "후드티"
        every { typedTuple2.score } returns 80.0

        val typedTuple3 = mockk<ZSetOperations.TypedTuple<String>>()
        every { typedTuple3.value } returns "청바지"
        every { typedTuple3.score } returns 60.0

        every {
            zSetOps.reverseRangeWithScores("search:popular_keywords", 0, 9)
        } returns linkedSetOf(typedTuple1, typedTuple2, typedTuple3)

        // 이전 스냅샷: 맨투맨은 2위, 후드티는 1위 (순위 변동 비교용)
        val prevTuple1 = mockk<ZSetOperations.TypedTuple<String>>()
        every { prevTuple1.value } returns "후드티"
        every { prevTuple1.score } returns 90.0

        val prevTuple2 = mockk<ZSetOperations.TypedTuple<String>>()
        every { prevTuple2.value } returns "맨투맨"
        every { prevTuple2.score } returns 70.0

        every {
            zSetOps.reverseRangeWithScores("search:popular_keywords:previous", 0, 9)
        } returns linkedSetOf(prevTuple1, prevTuple2)

        When("Top 10 인기 검색어를 조회하면") {
            val result = service.getPopularKeywords(10)

            Then("순위별로 반환되며 순위 변동이 표시된다") {
                result shouldHaveSize 3
                result[0].rank shouldBe 1
                result[0].keyword shouldBe "맨투맨"
                result[0].score shouldBe 100L
                result[0].rankChange shouldBe RankChange.UP // 2위 → 1위

                result[1].rank shouldBe 2
                result[1].keyword shouldBe "후드티"
                result[1].rankChange shouldBe RankChange.DOWN // 1위 → 2위

                result[2].rank shouldBe 3
                result[2].keyword shouldBe "청바지"
                result[2].rankChange shouldBe RankChange.NEW // 이전에 없음
            }
        }
    }

    Given("인기 검색어 초기화 시") {

        every { redisTemplate.delete("search:popular_keywords") } returns true
        every { redisTemplate.delete("search:popular_keywords:previous") } returns true

        When("초기화 API를 호출하면") {
            service.resetPopularKeywords()

            Then("현재 키와 이전 스냅샷 키가 모두 삭제된다") {
                verify(exactly = 1) { redisTemplate.delete("search:popular_keywords") }
                verify(exactly = 1) { redisTemplate.delete("search:popular_keywords:previous") }
            }
        }
    }
})
