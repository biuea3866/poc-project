package com.biuea.chat.infrastructure.redis

import com.biuea.chat.domain.connection.ConnectionRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component

/**
 * 이 서버의 채널(server:{myId})로 전파된 envelope 를 받아, 로컬 연결에 분배한다.
 */
@Component
class ServerChannelSubscriber(
    private val connectionRegistry: ConnectionRegistry,
    private val objectMapper: ObjectMapper,
) : MessageListener {
    override fun onMessage(message: Message, pattern: ByteArray?) {
        val payload = objectMapper.readValue(message.body, EnvelopePayload::class.java)
        val envelope = payload.toEnvelope()
        envelope.recipientIds.forEach { userId ->
            connectionRegistry.find(userId)?.send(envelope.message)
        }
    }
}
