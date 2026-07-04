package com.biuea.chat.presentation.ws

import com.biuea.chat.application.SendDirectMessageCommand
import com.biuea.chat.application.SendDirectMessageUseCase
import com.biuea.chat.application.SendRoomMessageCommand
import com.biuea.chat.application.SendRoomMessageUseCase
import com.biuea.chat.domain.connection.ConnectionRegistry
import com.biuea.chat.domain.routing.SessionLocator
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * 채팅 WebSocket 진입점. 접속 시 세션을 로컬 레지스트리와 세션 레지스트리에 등록하고,
 * 수신한 프레임을 UseCase 로 넘긴다.
 */
@Component
class ChatWebSocketHandler(
    private val connectionRegistry: ConnectionRegistry,
    private val sessionLocator: SessionLocator,
    private val sendDirectMessageUseCase: SendDirectMessageUseCase,
    private val sendRoomMessageUseCase: SendRoomMessageUseCase,
    private val objectMapper: ObjectMapper,
    @Value("\${server.id}") private val serverId: String,
) : TextWebSocketHandler() {
    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.userId()
        connectionRegistry.bind(WebSocketChatConnection(userId, session, objectMapper))
        sessionLocator.register(userId, serverId)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val inbound = objectMapper.readValue(message.payload, InboundPayload::class.java)
        val senderId = session.userId()
        when (inbound.type) {
            "direct" -> sendDirectMessageUseCase.execute(
                SendDirectMessageCommand(senderId, requireNotNull(inbound.receiverId), inbound.content),
            )
            "room" -> sendRoomMessageUseCase.execute(
                SendRoomMessageCommand(senderId, requireNotNull(inbound.roomId), inbound.content),
            )
            else -> throw IllegalArgumentException("unknown message type: ${inbound.type}")
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = session.userId()
        connectionRegistry.unbind(userId)
        sessionLocator.deregister(userId)
    }

    private fun WebSocketSession.userId(): Long {
        val query = uri?.query ?: error("userId query param is required")
        val raw = query.split("&")
            .firstOrNull { it.startsWith("userId=") }
            ?.substringAfter("=")
        return requireNotNull(raw?.toLongOrNull()) { "invalid userId query param" }
    }
}
