package com.biuea.chat.presentation.ws

import com.biuea.chat.domain.message.ChatMessage
import com.biuea.chat.domain.message.MessageTarget

/** 클라이언트가 보내는 메시지. type 은 "direct" 또는 "room". */
data class InboundPayload(
    val type: String,
    val receiverId: Long? = null,
    val roomId: Long? = null,
    val content: String,
)

/** 클라이언트로 내보내는 메시지. */
data class OutboundPayload(
    val messageId: String,
    val senderId: Long,
    val targetType: String,
    val targetId: Long,
    val content: String,
    val occurredAt: String,
) {
    companion object {
        fun from(message: ChatMessage): OutboundPayload {
            val (targetType, targetId) = when (val target = message.target) {
                is MessageTarget.Direct -> "direct" to target.receiverId
                is MessageTarget.Room -> "room" to target.roomId
            }
            return OutboundPayload(
                messageId = message.messageId,
                senderId = message.senderId,
                targetType = targetType,
                targetId = targetId,
                content = message.content,
                occurredAt = message.occurredAt.toString(),
            )
        }
    }
}
