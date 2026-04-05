package com.closet.search.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

class RecentKeywordServiceTest : BehaviorSpec({

    val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    val listOps = mockk<ListOperations<String, String>>(relaxed = true)

    every { redisTemplate.opsForList() } returns listOps

    val service = RecentKeywordService(redisTemplate = redisTemplate)

    Given("최근 검색어 저장 시") {

        When("일반 검색어를 저장하면") {
            service.saveRecentKeyword(memberId = 1L, keyword = "맨투맨")

            Then("중복 제거 후 리스트 앞에 추가되고 TTL이 설정된다") {
                val key = "search:recent:1"
                verify(exactly = 1) { listOps.remove(key, 0, "맨투맨") }
                verify(exactly = 1) { listOps.leftPush(key, "맨투맨") }
                verify(exactly = 1) { listOps.trim(key, 0, 19) }
                verify(exactly = 1) { redisTemplate.expire(key, Duration.ofDays(30)) }
            }
        }

        When("빈 문자열을 저장하면") {
            service.saveRecentKeyword(memberId = 1L, keyword = "   ")

            Then("저장하지 않는다") {
                verify(exactly = 0) { listOps.leftPush("search:recent:1", "") }
            }
        }
    }

    Given("최근 검색어 조회 시") {

        every {
            listOps.range("search:recent:1", 0, 19)
        } returns listOf("청바지", "후드티", "맨투맨")

        When("최근 검색어를 조회하면") {
            val result = service.getRecentKeywords(memberId = 1L, size = 20)

            Then("최신순으로 반환된다") {
                result shouldHaveSize 3
                result[0] shouldBe "청바지"
                result[1] shouldBe "후드티"
                result[2] shouldBe "맨투맨"
            }
        }
    }

    Given("최근 검색어 삭제 시") {

        When("특정 검색어를 삭제하면") {
            service.deleteRecentKeyword(memberId = 1L, keyword = "후드티")

            Then("해당 키워드만 리스트에서 제거된다") {
                verify(atLeast = 1) { listOps.remove("search:recent:1", 0, "후드티") }
            }
        }

        When("전체 검색어를 삭제하면") {
            service.deleteAllRecentKeywords(memberId = 1L)

            Then("Redis 키가 삭제된다") {
                verify(exactly = 1) { redisTemplate.delete("search:recent:1") }
            }
        }
    }
})
