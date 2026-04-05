package com.closet.search.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.StringRedisTemplate

class BannedKeywordServiceTest : BehaviorSpec({

    val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    val setOps = mockk<SetOperations<String, String>>(relaxed = true)

    every { redisTemplate.opsForSet() } returns setOps

    // 초기 캐시 로드용 stub
    every { setOps.members("search:banned_keywords") } returns setOf("욕설", "비속어", "금칙어1")

    val service = BannedKeywordService(redisTemplate = redisTemplate)

    Given("금칙어 여부 확인 시") {

        When("금칙어에 해당하는 키워드를 확인하면") {
            val result = service.isBanned("욕설")

            Then("true가 반환된다") {
                result shouldBe true
            }
        }

        When("금칙어가 아닌 키워드를 확인하면") {
            val result = service.isBanned("맨투맨")

            Then("false가 반환된다") {
                result shouldBe false
            }
        }

        When("대소문자/공백 변환 후 금칙어에 해당하면") {
            val result = service.isBanned("  욕설  ")

            Then("trim 후 비교하여 true가 반환된다") {
                result shouldBe true
            }
        }
    }

    Given("금칙어 추가 시") {

        When("새로운 금칙어를 추가하면") {
            service.addBannedKeyword("새금칙어")

            Then("Redis Set에 추가되고 로컬 캐시도 갱신된다") {
                verify(exactly = 1) { setOps.add("search:banned_keywords", "새금칙어") }
                service.isBanned("새금칙어") shouldBe true
            }
        }

        When("빈 문자열을 추가하면") {
            service.addBannedKeyword("   ")

            Then("추가하지 않는다") {
                verify(exactly = 0) { setOps.add("search:banned_keywords", "") }
            }
        }
    }

    Given("금칙어 삭제 시") {

        When("기존 금칙어를 삭제하면") {
            service.removeBannedKeyword("비속어")

            Then("Redis Set에서 삭제되고 로컬 캐시도 갱신된다") {
                verify(exactly = 1) { setOps.remove("search:banned_keywords", "비속어") }
                service.isBanned("비속어") shouldBe false
            }
        }
    }

    Given("금칙어 목록 조회 시") {

        every { setOps.members("search:banned_keywords") } returns setOf("욕설", "비속어", "금칙어1")

        When("전체 금칙어를 조회하면") {
            val result = service.getAllBannedKeywords()

            Then("Redis Set의 모든 멤버가 반환된다") {
                result.size shouldBe 3
            }
        }
    }

    Given("캐시 갱신 시") {

        every { setOps.members("search:banned_keywords") } returns setOf("새로운금칙어")

        When("캐시를 갱신하면") {
            service.refreshCache()

            Then("Redis의 최신 금칙어로 로컬 캐시가 교체된다") {
                service.isBanned("새로운금칙어") shouldBe true
                service.isBanned("욕설") shouldBe false  // 이전 캐시 데이터 제거됨
            }
        }
    }
})
