package com.biuea.chat.presentation.ws

import com.biuea.chat.support.RedisContainer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * 서버 1대 실제 구동 E2E. 두 클라이언트가 접속해 A 가 보낸 메시지를 B 가 실시간으로 받는지 검증한다.
 * 실제 임베디드 서버 + 실제 Redis(세션 레지스트리) 를 사용한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `A가 보낸 direct 메시지를 B가 실시간으로 받는다`() {
        val client = StandardWebSocketClient()
        val bInbox = LinkedBlockingQueue<String>()

        val bHandler = object : TextWebSocketHandler() {
            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                bInbox.add(message.payload)
            }
        }

        val bSession = client.execute(bHandler, "ws://localhost:$port/ws/chat?userId=2").get(5, SECONDS)
        val aSession = client.execute(TextWebSocketHandler(), "ws://localhost:$port/ws/chat?userId=1").get(5, SECONDS)

        // 서버 측 afterConnectionEstablished(레지스트리 등록)가 반영될 시간을 준다
        Thread.sleep(300)
        aSession.sendMessage(TextMessage("""{"type":"direct","receiverId":2,"content":"hi B"}"""))

        val received = bInbox.poll(5, SECONDS)
        assertThat(received).isNotNull()
        assertThat(received).contains("hi B")
        assertThat(received).contains("\"senderId\":1")

        aSession.close()
        bSession.close()
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { RedisContainer.host }
            registry.add("spring.data.redis.port") { RedisContainer.port }
        }
    }
}
