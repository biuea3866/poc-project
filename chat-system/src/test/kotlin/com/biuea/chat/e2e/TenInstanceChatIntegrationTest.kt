package com.biuea.chat.e2e

import com.biuea.chat.support.ChatCluster
import com.biuea.chat.support.CollectingHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.web.socket.TextMessage

/**
 * 실제 10대 인스턴스 구동. 인스턴스가 늘어도 세션 레지스트리 기반 지향 전달과
 * 서버 단위 팬아웃이 그대로 동작함을 확인한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenInstanceChatIntegrationTest {

    private lateinit var cluster: ChatCluster

    @BeforeAll
    fun bootstrap() {
        cluster = ChatCluster((1..10).map { "server-$it" })
    }

    @AfterAll
    fun shutdown() {
        cluster.close()
    }

    @Test
    fun `인스턴스1의 A가 인스턴스10의 B에게 지향 전달한다 (10대 크로스 인스턴스)`() {
        val inboxB = LinkedBlockingQueue<String>()
        val locator = cluster.sessionLocator()

        val a = ChatCluster.connect(cluster.ports[0], userId = 1, handler = CollectingHandler {})
        val b = ChatCluster.connect(cluster.ports[9], userId = 10, handler = CollectingHandler { inboxB.add(it) })

        ChatCluster.awaitUntil(10_000) { locator.locate(10) != null }
        a.sendMessage(TextMessage("""{"type":"direct","receiverId":10,"content":"node1 to node10"}"""))

        assertThat(inboxB.poll(5, SECONDS)).contains("node1 to node10")

        a.close(); b.close()
    }

    @Test
    fun `방 멤버가 10개 인스턴스에 하나씩 흩어져 있어도 전원이 받는다`() {
        val roomId = 400L
        cluster.roomRepository().let { rooms -> (1L..10L).forEach { rooms.join(roomId, it) } }

        val received = ConcurrentHashMap.newKeySet<Long>()
        val locator = cluster.sessionLocator()

        // user i 를 인스턴스 i 에 접속시킨다 (user 1 = 발신자)
        val sessions = (1L..10L).map { userId ->
            ChatCluster.connect(cluster.ports[(userId - 1).toInt()], userId, CollectingHandler { received.add(userId) })
        }

        ChatCluster.awaitUntil(15_000) { (2L..10L).all { locator.locate(it) != null } }
        sessions.first().sendMessage(TextMessage("""{"type":"room","roomId":400,"content":"ten node fanout"}"""))

        // 발신자(1) 제외 나머지 9명이 서로 다른 9개 인스턴스에서 모두 수신
        ChatCluster.awaitUntil(10_000) { received.containsAll((2L..10L).toList()) }
        assertThat(received).containsExactlyInAnyOrderElementsOf((2L..10L).toList())

        sessions.forEach { it.close() }
    }
}
