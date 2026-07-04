package com.biuea.chat.infrastructure.redis

import com.biuea.chat.support.RedisContainer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * 세션 레지스트리(서버 다수 단계)의 Redis 왕복을 실제 컨테이너로 검증한다.
 */
class RedisSessionLocatorIntegrationTest : FunSpec({
    val factory = LettuceConnectionFactory(RedisContainer.host, RedisContainer.port).apply {
        afterPropertiesSet()
        start()
    }
    val template = StringRedisTemplate(factory).apply { afterPropertiesSet() }
    val locator = RedisSessionLocator(template)

    afterSpec { factory.destroy() }

    test("register 후 locate 하면 등록한 serverId 를 돌려준다") {
        locator.register(100, "server-7")

        locator.locate(100) shouldBe "server-7"
    }

    test("deregister 하면 locate 결과가 null 이다") {
        locator.register(101, "server-1")
        locator.deregister(101)

        locator.locate(101) shouldBe null
    }

    test("등록되지 않은 사용자는 오프라인(null)이다") {
        locator.locate(999_999) shouldBe null
    }
})
