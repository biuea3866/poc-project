package com.biuea.chat.infrastructure.memory

import com.biuea.chat.domain.connection.ChatConnection
import com.biuea.chat.domain.connection.ConnectionRegistry
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component

/**
 * 서버 1대 단계의 인메모리 세션 맵. userId 로 열린 연결을 보관한다.
 */
@Component
class InMemoryConnectionRegistry : ConnectionRegistry {
    private val connections = ConcurrentHashMap<Long, ChatConnection>()

    override fun bind(connection: ChatConnection) {
        connections[connection.userId] = connection
    }

    override fun unbind(userId: Long) {
        connections.remove(userId)
    }

    override fun find(userId: Long): ChatConnection? = connections[userId]
}
