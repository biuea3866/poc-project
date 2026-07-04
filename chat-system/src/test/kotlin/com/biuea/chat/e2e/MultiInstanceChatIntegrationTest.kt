package com.biuea.chat.e2e

import com.biuea.chat.ChatSystemApplication
import com.biuea.chat.infrastructure.redis.RedisRoomRepository
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
 * 진짜 멀티 인스턴스 검증. server.id 가 다른 3개의 Spring Boot 인스턴스를 같은 Redis 로 띄운다.
 * 방 멤버십은 공유 저장소(RedisRoomRepository)에만 두므로, 어느 인스턴스도 손수 채우지 않는다.
 *
 * - 크로스 인스턴스 지향 전달(N대): 인스턴스1의 유저 → 세션 레지스트리 조회 → 인스턴스3의 유저 수신
 * - 그룹 멀티 인스턴스 팬아웃: 방 멤버가 3개 인스턴스에 흩어져도 서버 단위 팬아웃으로 전원 수신
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiInstanceChatIntegrationTest {

    private val instances = mutableListOf<ConfigurableApplicationContext>()
    private val ports = mutableListOf<Int>()

    @BeforeAll
    fun bootstrap() {
        listOf("server-1", "server-2", "server-3").forEach { serverId ->
            val context = boot(serverId)
            instances.add(context)
            ports.add((context as ServletWebServerApplicationContext).webServer.port)
        }
    }

    @AfterAll
    fun shutdown() {
        instances.forEach { it.close() }
    }

    @Test
    fun `인스턴스1의 A가 보낸 메시지를 인스턴스3의 C가 받는다 (N대 크로스 인스턴스 지향 전달)`() {
        val inboxC = LinkedBlockingQueue<String>()
        val a = connect(ports[0], userId = 1)                 // 인스턴스1
        val c = connect(ports[2], userId = 3, inbox = inboxC) // 인스턴스3

        awaitRegistration()
        a.sendMessage(TextMessage("""{"type":"direct","receiverId":3,"content":"A to C across 3 nodes"}"""))

        val received = inboxC.poll(5, SECONDS)
        assertThat(received).isNotNull()
        assertThat(received).contains("A to C across 3 nodes")

        a.close(); c.close()
    }

    @Test
    fun `방 멤버가 3개 인스턴스에 흩어져 있어도 전원이 받는다 (그룹 멀티 인스턴스 팬아웃)`() {
        val roomId = 200L
        // 공유 저장소에 한 번만 등록한다. 세 인스턴스가 같은 Redis Set 을 본다.
        val rooms = instances.first().getBean(RedisRoomRepository::class.java)
        listOf(1L, 2L, 3L, 4L, 5L).forEach { rooms.join(roomId, it) }

        val inbox2 = LinkedBlockingQueue<String>()
        val inbox3 = LinkedBlockingQueue<String>()
        val inbox4 = LinkedBlockingQueue<String>()
        val inbox5 = LinkedBlockingQueue<String>()

        val sender = connect(ports[0], userId = 1)              // 인스턴스1 (발신)
        val u2 = connect(ports[0], userId = 2, inbox = inbox2)  // 인스턴스1 (로컬)
        val u3 = connect(ports[1], userId = 3, inbox = inbox3)  // 인스턴스2 (원격)
        val u4 = connect(ports[2], userId = 4, inbox = inbox4)  // 인스턴스3 (원격)
        val u5 = connect(ports[2], userId = 5, inbox = inbox5)  // 인스턴스3 (원격, 같은 서버)

        awaitRegistration()
        sender.sendMessage(TextMessage("""{"type":"room","roomId":200,"content":"hello everyone"}"""))

        assertThat(inbox2.poll(5, SECONDS)).contains("hello everyone") // 인스턴스1 로컬
        assertThat(inbox3.poll(5, SECONDS)).contains("hello everyone") // 인스턴스2 팬아웃
        assertThat(inbox4.poll(5, SECONDS)).contains("hello everyone") // 인스턴스3 팬아웃
        assertThat(inbox5.poll(5, SECONDS)).contains("hello everyone") // 인스턴스3 팬아웃

        sender.close(); u2.close(); u3.close(); u4.close(); u5.close()
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
    private fun awaitRegistration() = Thread.sleep(500)
}
