package com.biuea.chat.e2e

import com.biuea.chat.ChatSystemApplication
import com.biuea.chat.infrastructure.memory.InMemoryRoomRepository
import com.biuea.chat.support.RedisContainer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * 진짜 멀티 인스턴스 검증. 서로 다른 server.id 를 가진 두 개의 Spring Boot 인스턴스를
 * 같은 Redis 로 띄우고, 한 인스턴스에 붙은 유저가 다른 인스턴스에 붙은 유저에게
 * 실시간으로 메시지를 전달하는지 확인한다.
 *
 * - 서버 2대/N대: A(인스턴스1) → Redis(세션 레지스트리 조회 + 지향 전달) → B(인스턴스2) 수신
 * - 그룹 멀티 인스턴스: 방 멤버가 두 인스턴스에 흩어져 있어도 서버 단위 팬아웃으로 전원 수신
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiInstanceChatIntegrationTest {

    private lateinit var instance1: ConfigurableApplicationContext
    private lateinit var instance2: ConfigurableApplicationContext
    private var port1 = 0
    private var port2 = 0

    @BeforeAll
    fun bootstrap() {
        instance1 = boot("server-1")
        instance2 = boot("server-2")
        port1 = portOf(instance1)
        port2 = portOf(instance2)
    }

    @AfterAll
    fun shutdown() {
        instance1.close()
        instance2.close()
    }

    @Test
    fun `인스턴스1의 A가 보낸 메시지를 인스턴스2의 B가 받는다 (크로스 인스턴스 지향 전달)`() {
        val inboxB = LinkedBlockingQueue<String>()
        val a = connect(port1, userId = 1)                 // 인스턴스1
        val b = connect(port2, userId = 2, inbox = inboxB) // 인스턴스2

        awaitRegistration()
        a.sendMessage(TextMessage("""{"type":"direct","receiverId":2,"content":"cross A to B"}"""))

        val received = inboxB.poll(5, SECONDS)
        assertThat(received).isNotNull()
        assertThat(received).contains("cross A to B")

        a.close(); b.close()
    }

    @Test
    fun `방 멤버가 두 인스턴스에 흩어져 있어도 전원이 받는다 (그룹 멀티 인스턴스 팬아웃)`() {
        val roomId = 100L
        // 발신 인스턴스(인스턴스1)의 방 멤버십을 구성한다 (실서비스에서는 공유 저장소가 담당)
        val rooms = instance1.getBean(InMemoryRoomRepository::class.java)
        listOf(1L, 2L, 3L, 4L).forEach { rooms.join(roomId, it) }

        val inbox2 = LinkedBlockingQueue<String>()
        val inbox3 = LinkedBlockingQueue<String>()
        val inbox4 = LinkedBlockingQueue<String>()

        val sender = connect(port1, userId = 1)              // 인스턴스1 (발신)
        val u2 = connect(port1, userId = 2, inbox = inbox2)  // 인스턴스1 (로컬 전달)
        val u3 = connect(port2, userId = 3, inbox = inbox3)  // 인스턴스2 (원격 전달)
        val u4 = connect(port2, userId = 4, inbox = inbox4)  // 인스턴스2 (원격 전달)

        awaitRegistration()
        sender.sendMessage(TextMessage("""{"type":"room","roomId":100,"content":"hello room"}"""))

        assertThat(inbox2.poll(5, SECONDS)).contains("hello room") // 같은 인스턴스 로컬
        assertThat(inbox3.poll(5, SECONDS)).contains("hello room") // 다른 인스턴스로 팬아웃
        assertThat(inbox4.poll(5, SECONDS)).contains("hello room") // 다른 인스턴스로 팬아웃

        sender.close(); u2.close(); u3.close(); u4.close()
    }

    private fun boot(serverId: String): ConfigurableApplicationContext =
        // 커맨드라인 인자로 넘겨 application.yml 보다 높은 우선순위로 덮는다 (server.port=0 = 랜덤 포트)
        SpringApplicationBuilder(ChatSystemApplication::class.java)
            .run(
                "--server.id=$serverId",
                "--server.port=0",
                "--spring.data.redis.host=${RedisContainer.host}",
                "--spring.data.redis.port=${RedisContainer.port}",
            )

    private fun portOf(context: ConfigurableApplicationContext): Int =
        (context as ServletWebServerApplicationContext).webServer.port

    private fun connect(
        port: Int,
        userId: Long,
        inbox: BlockingQueue<String> = LinkedBlockingQueue(),
    ): WebSocketSession {
        val handler = object : TextWebSocketHandler() {
            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                inbox.add(message.payload)
            }
        }
        return StandardWebSocketClient()
            .execute(handler, "ws://localhost:$port/ws/chat?userId=$userId")
            .get(5, SECONDS)
    }

    // 접속 직후 각 인스턴스의 afterConnectionEstablished 가 세션 레지스트리에 반영될 시간을 준다
    private fun awaitRegistration() = Thread.sleep(400)
}
