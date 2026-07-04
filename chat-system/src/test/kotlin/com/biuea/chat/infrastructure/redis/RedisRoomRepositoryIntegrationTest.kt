package com.biuea.chat.infrastructure.redis

import com.biuea.chat.support.RedisContainer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * 공유 방 멤버십(Redis Set)을 실제 컨테이너로 검증한다. 멀티 인스턴스에서
 * 모든 인스턴스가 같은 멤버 목록을 보는 전제를 담보한다.
 */
class RedisRoomRepositoryIntegrationTest : FunSpec({
    val factory = LettuceConnectionFactory(RedisContainer.host, RedisContainer.port).apply {
        afterPropertiesSet()
        start()
    }
    val template = StringRedisTemplate(factory).apply { afterPropertiesSet() }
    val repository = RedisRoomRepository(template)

    afterSpec { factory.destroy() }

    test("join 한 멤버들을 membersOf 가 돌려준다") {
        repository.join(500, 1)
        repository.join(500, 2)
        repository.join(500, 3)

        repository.membersOf(500) shouldBe setOf(1L, 2L, 3L)
    }

    test("leave 하면 멤버에서 빠진다") {
        repository.join(501, 1)
        repository.join(501, 2)
        repository.leave(501, 2)

        repository.membersOf(501) shouldBe setOf(1L)
    }

    test("멤버가 없는 방은 빈 집합이다") {
        repository.membersOf(999_999) shouldBe emptySet()
    }
})
