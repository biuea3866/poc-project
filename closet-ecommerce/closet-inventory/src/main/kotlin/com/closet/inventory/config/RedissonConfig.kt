package com.closet.inventory.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    @Value("\${spring.data.redis.host:localhost}")
    private val redisHost: String,

    @Value("\${spring.data.redis.port:6379}")
    private val redisPort: Int,
) {

    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        config.useSingleServer()
            .setAddress("redis://$redisHost:$redisPort")
        config.codec = JsonJacksonCodec()
        return Redisson.create(config)
    }
}
