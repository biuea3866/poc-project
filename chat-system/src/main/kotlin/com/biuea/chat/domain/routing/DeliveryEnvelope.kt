package com.biuea.chat.domain.routing

import com.biuea.chat.domain.message.ChatMessage

/**
 * 한 서버로 전달할 묶음. recipientIds 는 그 서버가 로컬로 분배할 수신자다.
 * 일대일이면 1명, 그룹이면 그 서버에 붙은 방 멤버 전원 (서버 단위 팬아웃).
 */
data class DeliveryEnvelope(
    val recipientIds: List<Long>,
    val message: ChatMessage,
)
