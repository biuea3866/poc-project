package com.biuea.chat.infrastructure.redis

import com.biuea.chat.domain.routing.DeliveryEnvelope
import com.biuea.chat.domain.routing.MessageBroker
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * 서버 간 전파의 Redis Pub/Sub 구현. 수신자가 붙은 서버의 채널(server:{id})로만 발행한다.
 */
@Component
class RedisMessageBroker(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : MessageBroker {
    override fun forward(serverId: String, envelope: DeliveryEnvelope) {
        val json = objectMapper.writeValueAsString(EnvelopePayload.from(envelope))
        redisTemplate.convertAndSend(channel(serverId), json)
    }

    companion object {
        fun channel(serverId: String) = "server:$serverId"
    }
}
