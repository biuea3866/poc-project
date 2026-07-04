package com.biuea.chat.domain.connection

import com.biuea.chat.domain.message.ChatMessage

/**
 * 한 사용자와의 열린 연결. WebSocket 세션을 감싸는 추상화라 테스트에서 fake 로 대체할 수 있다.
 */
interface ChatConnection {
    val userId: Long
    fun send(message: ChatMessage)
}
