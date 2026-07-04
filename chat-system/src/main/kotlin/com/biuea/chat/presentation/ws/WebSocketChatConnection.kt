package com.biuea.chat.presentation.ws

import com.biuea.chat.domain.connection.ChatConnection
import com.biuea.chat.domain.message.ChatMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

/**
 * WebSocket 세션을 감싼 연결. 메시지를 JSON 프레임으로 직렬화해 클라이언트로 보낸다.
 */
class WebSocketChatConnection(
    override val userId: Long,
    private val session: WebSocketSession,
    private val objectMapper: ObjectMapper,
) : ChatConnection {
    override fun send(message: ChatMessage) {
        if (!session.isOpen) return
        val json = objectMapper.writeValueAsString(OutboundPayload.from(message))
        synchronized(session) {
            session.sendMessage(TextMessage(json))
        }
    }
}
