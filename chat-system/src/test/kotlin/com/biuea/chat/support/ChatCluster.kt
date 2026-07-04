package com.biuea.chat.support

import com.biuea.chat.ChatSystemApplication
import com.biuea.chat.infrastructure.redis.RedisRoomRepository
import com.biuea.chat.infrastructure.redis.RedisSessionLocator
import java.util.concurrent.TimeUnit.SECONDS
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient

/**
 * 실제 Spring Boot 인스턴스 N대를 같은 Redis 로 띄우는 테스트 클러스터.
 * 각 인스턴스는 서로 다른 server.id 와 랜덤 포트를 가진다.
 */
class ChatCluster(serverIds: List<String>) {
    private val contexts: List<ConfigurableApplicationContext> = serverIds.map { boot(it) }

    val ports: List<Int> = contexts.map { (it as ServletWebServerApplicationContext).webServer.port }

    fun roomRepository(): RedisRoomRepository = contexts.first().getBean(RedisRoomRepository::class.java)

    fun sessionLocator(): RedisSessionLocator = contexts.first().getBean(RedisSessionLocator::class.java)

    fun close() = contexts.forEach { it.close() }

    private fun boot(serverId: String): ConfigurableApplicationContext =
        SpringApplicationBuilder(ChatSystemApplication::class.java).run(
            "--server.id=$serverId",
            "--server.port=0",
            "--spring.data.redis.host=${RedisContainer.host}",
            "--spring.data.redis.port=${RedisContainer.port}",
        )

    companion object {
        private val client = StandardWebSocketClient()

        fun connect(port: Int, userId: Long, handler: WebSocketHandler): WebSocketSession =
            client.execute(handler, "ws://localhost:$port/ws/chat?userId=$userId").get(10, SECONDS)

        /** 조건이 참이 될 때까지 최대 timeoutMillis 대기한다 (레지스트리 반영 대기용). */
        fun awaitUntil(timeoutMillis: Long, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                if (condition()) return
                Thread.sleep(100)
            }
            check(condition()) { "condition not met within ${timeoutMillis}ms" }
        }
    }
}
