package com.biuea.chat.support

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/** 수신한 텍스트 프레임을 콜백으로 넘기는 테스트용 WebSocket 핸들러. */
class CollectingHandler(private val onText: (String) -> Unit) : TextWebSocketHandler() {
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        onText(message.payload)
    }
}
