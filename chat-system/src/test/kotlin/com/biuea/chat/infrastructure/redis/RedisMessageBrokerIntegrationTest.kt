package com.biuea.chat.infrastructure.redis

import com.biuea.chat.domain.message.ChatMessage
import com.biuea.chat.domain.routing.DeliveryEnvelope
import com.biuea.chat.support.FakeConnectionRegistry
import com.biuea.chat.support.RecordingConnection
import com.biuea.chat.support.RedisContainer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer

/**
 * 서버 간 전파(서버 2대~다수 단계)를 실제 Redis Pub/Sub 로 검증한다.
 * 발신 서버가 forward 하면, 대상 서버의 채널 구독자가 받아 로컬 연결로 분배한다.
 */
class RedisMessageBrokerIntegrationTest : FunSpec({
    val factory = LettuceConnectionFactory(RedisContainer.host, RedisContainer.port).apply {
        afterPropertiesSet()
        start()
    }
    val template = StringRedisTemplate(factory).apply { afterPropertiesSet() }
    val objectMapper = jacksonObjectMapper()
    val broker = RedisMessageBroker(template, objectMapper)

    // 수신 측(다른 서버) 구성: server-B 채널 구독
    val registry = FakeConnectionRegistry()
    val subscriber = ServerChannelSubscriber(registry, objectMapper)
    val listenerContainer = RedisMessageListenerContainer().apply {
        setConnectionFactory(factory)
        afterPropertiesSet()
        addMessageListener(subscriber, ChannelTopic(RedisMessageBroker.channel("server-B")))
        start()
    }

    afterSpec {
        listenerContainer.stop()
        factory.destroy()
    }

    test("forward 하면 대상 서버 채널 구독자가 받아 로컬 수신자에게 전달한다") {
        val receiver = RecordingConnection(2).also { registry.bind(it) }
        val message = ChatMessage.direct(senderId = 1, receiverId = 2, content = "cross-server")

        // 구독 등록이 비동기라, 수신될 때까지 재발행하며 대기한다
        eventually(5.seconds) {
            broker.forward("server-B", DeliveryEnvelope(listOf(2), message))
            receiver.received.size shouldBeGreaterThanOrEqual 1
        }

        receiver.received.first().content shouldBe "cross-server"
        receiver.received.first().messageId shouldBe message.messageId
    }
})
