package com.biuea.chat.e2e

import com.biuea.chat.support.ChatCluster
import com.biuea.chat.support.CollectingHandler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

/**
 * 1000 소켓 부하 검증. 3개 인스턴스에 1000명의 실제 WebSocket 수신자를 분산 접속시키고,
 * 한 발신자가 방에 메시지 1건을 보내면 서버 단위 팬아웃으로 1000명 전원이 받는지 확인한다.
 * 팬아웃 증폭(메시지 1건 → 1000건 전달)이 실제로 동작함을 실소켓으로 증명한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThousandSocketFanoutLoadTest {

    private lateinit var cluster: ChatCluster

    @BeforeAll
    fun bootstrap() {
        cluster = ChatCluster(listOf("server-1", "server-2", "server-3"))
    }

    @AfterAll
    fun shutdown() {
        cluster.close()
    }

    @Test
    fun `1000명 방에 보낸 메시지 1건이 3개 인스턴스의 1000 소켓 전원에게 전달된다`() {
        val roomId = 500L
        val receiverCount = 1000
        val senderId = 999_999L // ChatMessage 는 senderId > 0 을 요구한다

        // 방 멤버십(공유 Redis Set): 발신자 + 수신자 1000명
        val rooms = cluster.roomRepository()
        rooms.join(roomId, senderId)
        (1..receiverCount).forEach { rooms.join(roomId, it.toLong()) }

        val delivered = AtomicInteger(0)
        val latch = CountDownLatch(receiverCount)
        val sessions = mutableListOf<WebSocketSession>()

        // 수신자 1000명을 3개 인스턴스에 라운드로빈으로 분산 접속
        for (userId in 1..receiverCount) {
            val port = cluster.ports[userId % cluster.ports.size]
            sessions += ChatCluster.connect(port, userId.toLong(), CollectingHandler {
                delivered.incrementAndGet()
                latch.countDown()
            })
        }
        val sender = ChatCluster.connect(cluster.ports[0], senderId, CollectingHandler {})

        // 발신 전에 1000명 전원이 세션 레지스트리에 등록됐는지 확인 (미등록 = 오프라인 취급되어 누락)
        val locator = cluster.sessionLocator()
        ChatCluster.awaitUntil(30_000) { (1..receiverCount).all { locator.locate(it.toLong()) != null } }

        sender.sendMessage(TextMessage("""{"type":"room","roomId":500,"content":"fanout to 1000"}"""))

        val allReceived = latch.await(60, SECONDS)
        assertThat(delivered.get())
            .describedAs("전달된 소켓 수")
            .isEqualTo(receiverCount)
        assertThat(allReceived).isTrue()

        sessions.forEach { it.close() }
        sender.close()
    }
}
