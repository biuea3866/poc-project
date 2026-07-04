package com.biuea.chat.e2e

import com.biuea.chat.infrastructure.redis.RedisRoomRepository
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * 실제 분리된 프로세스(컨테이너) 멀티 인스턴스 검증. docker-compose 로 Redis 1개 +
 * 앱 인스턴스 3개(server-1/2/3)를 각각 별도 컨테이너로 띄운다. 단일 JVM 테스트와 달리
 * 인스턴스들이 서로 다른 프로세스이며 실제 컨테이너 네트워크로 Redis 를 공유한다.
 *
 * 기본 test 에서는 제외되고 `./gradlew composeTest` 로만 실행된다 (Docker 필요).
 */
@Tag("dockercompose")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DockerComposeMultiInstanceTest {

    private lateinit var rooms: RedisRoomRepository
    private lateinit var redisFactory: LettuceConnectionFactory

    @BeforeAll
    fun up() {
        compose("up", "-d", "--build")
        // Redis 는 즉시 준비된다. 앱은 접속 재시도로 준비 완료를 기다린다
        // (docker-proxy 가 앱 바인딩 전에 포트를 열어 TCP 체크만으로는 조기 통과한다).
        waitPort(PORT_REDIS)

        // 앱 컨테이너와 같은 Redis 로 방 멤버십을 심기 위한 저장소
        redisFactory = LettuceConnectionFactory("localhost", PORT_REDIS).apply { afterPropertiesSet(); start() }
        rooms = RedisRoomRepository(StringRedisTemplate(redisFactory).apply { afterPropertiesSet() })
    }

    @AfterAll
    fun down() {
        if (::redisFactory.isInitialized) redisFactory.destroy()
        compose("down", "-v")
    }

    @Test
    fun `chat-1 컨테이너의 A가 chat-3 컨테이너의 B에게 전달한다 (분리 프로세스 크로스 인스턴스)`() {
        val inboxB = LinkedBlockingQueue<String>()
        val a = connect(PORT_CHAT_1, userId = 1)
        val b = connect(PORT_CHAT_3, userId = 3, inbox = inboxB)

        val received = sendUntilReceived(inboxB) {
            a.sendMessage(TextMessage("""{"type":"direct","receiverId":3,"content":"container A to C"}"""))
        }

        assertThat(received).contains("container A to C")
        a.close(); b.close()
    }

    @Test
    fun `방 멤버가 3개 컨테이너에 흩어져 있어도 전원이 받는다 (분리 프로세스 그룹 팬아웃)`() {
        val roomId = 700L
        listOf(1L, 2L, 3L).forEach { rooms.join(roomId, it) } // 공유 Redis 에 멤버십 심기

        val inbox2 = LinkedBlockingQueue<String>()
        val inbox3 = LinkedBlockingQueue<String>()
        val sender = connect(PORT_CHAT_1, userId = 1)
        val u2 = connect(PORT_CHAT_2, userId = 2, inbox = inbox2)
        val u3 = connect(PORT_CHAT_3, userId = 3, inbox = inbox3)

        sendUntilReceived(inbox2, inbox3) {
            sender.sendMessage(TextMessage("""{"type":"room","roomId":700,"content":"container fanout"}"""))
        }

        assertThat(inbox2.peek()).contains("container fanout")
        assertThat(inbox3.peek()).contains("container fanout")
        sender.close(); u2.close(); u3.close()
    }

    private fun connect(
        port: Int,
        userId: Long,
        inbox: LinkedBlockingQueue<String> = LinkedBlockingQueue(),
    ): WebSocketSession {
        val handler = object : TextWebSocketHandler() {
            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                inbox.add(message.payload)
            }
        }
        // 앱이 WS 핸드셰이크를 받을 준비가 될 때까지 재시도한다 (컨테이너 부팅 대기)
        val deadline = System.currentTimeMillis() + 90_000
        var last: Exception? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                return StandardWebSocketClient()
                    .execute(handler, "ws://localhost:$port/ws/chat?userId=$userId")
                    .get(5, SECONDS)
            } catch (e: Exception) {
                last = e
                Thread.sleep(1_000)
            }
        }
        throw AssertionError("localhost:$port WS 접속이 90초 내에 성공하지 않았다", last)
    }

    // 접속 직후 세션 레지스트리 반영 지연을 흡수하려 수신될 때까지 재발행한다 (전달은 멱등적으로 반복 가능)
    private fun sendUntilReceived(vararg inboxes: LinkedBlockingQueue<String>, send: () -> Unit): String {
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline) {
            send()
            Thread.sleep(500)
            if (inboxes.all { it.isNotEmpty() }) return inboxes.first().peek()
        }
        throw AssertionError("메시지가 20초 내에 전원에게 전달되지 않았다")
    }

    companion object {
        private const val PORT_CHAT_1 = 18081
        private const val PORT_CHAT_2 = 18082
        private const val PORT_CHAT_3 = 18083
        private const val PORT_REDIS = 16379

        private fun compose(vararg args: String) {
            val command = listOf("docker", "compose", "-f", "docker-compose.yml") + args
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            check(exit == 0) { "docker compose ${args.joinToString(" ")} 실패 ($exit):\n$output" }
        }

        private fun waitPort(port: Int, timeoutMillis: Long = 180_000) {
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                runCatching {
                    Socket().use { it.connect(InetSocketAddress("localhost", port), 1_000) }
                }.onSuccess { return }
                Thread.sleep(1_000)
            }
            throw AssertionError("localhost:$port 가 ${timeoutMillis}ms 내에 열리지 않았다")
        }
    }
}
