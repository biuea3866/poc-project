package com.biuea.delivery.infrastructure.lock

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Redisson 클라이언트 배선.
 *
 * `redisson-spring-boot-starter` 대신 core 를 쓰고 여기서 직접 만든다. starter 의 자동 설정은
 * `RedissonConnectionFactory` 를 `RedisConnectionFactory` 로 선점 등록해(`@AutoConfiguration(before = RedisAutoConfiguration)`)
 * 기존 `StringRedisTemplate` 의 전송 계층을 Lettuce → Redisson 으로 바꾼다. 그러면 비교 대상인
 * 스핀락([RedisDistributedLock])의 조건이 함께 변해 "락 메커니즘만 바꿔 재측정" 이 성립하지 않는다.
 *
 * 접속 좌표는 `spring.data.redis.*` 를 그대로 재사용해 스핀락과 같은 Redis 를 보게 한다.
 * 워치독 만료 시간은 Redisson 기본값(30초)을 쓴다 — 임계 구역이 조회 + 배차 트랜잭션 하나뿐이라
 * 30초를 넘길 일이 없고, 넘긴다면 그것 자체가 관측해야 할 이상 상황이다.
 */
@Configuration
class RedissonLockConfiguration {

    @Bean(destroyMethod = "shutdown")
    fun redissonClient(redisProperties: RedisProperties): RedissonClient {
        val config = Config()
        // Redisson 의 설정 메서드는 fluent(자기 타입 반환)라 코틀린 합성 프로퍼티 대입에 의존하지 않고 직접 호출한다.
        config.useSingleServer()
            .setAddress("$REDIS_SCHEME${redisProperties.host}:${redisProperties.port}")
            .setDatabase(redisProperties.database)
            .setPassword(redisProperties.password)
        return Redisson.create(config)
    }

    companion object {
        private const val REDIS_SCHEME = "redis://"
    }
}
