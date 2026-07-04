package com.biuea.chat.domain.message

import java.time.ZonedDateTime
import java.util.UUID

/**
 * 채팅 메시지. 생성은 팩토리로만 하고, 시간과 식별자는 내부에서 해결한다.
 */
class ChatMessage private constructor(
    val messageId: String,
    val senderId: Long,
    val target: MessageTarget,
    val content: String,
    val occurredAt: ZonedDateTime,
) {
    companion object {
        fun direct(senderId: Long, receiverId: Long, content: String): ChatMessage =
            create(senderId, MessageTarget.Direct(receiverId), content)

        fun room(senderId: Long, roomId: Long, content: String): ChatMessage =
            create(senderId, MessageTarget.Room(roomId), content)

        private fun create(senderId: Long, target: MessageTarget, content: String): ChatMessage {
            require(senderId > 0) { "senderId must be positive" }
            require(content.isNotBlank()) { "content must not be blank" }
            return ChatMessage(
                messageId = UUID.randomUUID().toString(),
                senderId = senderId,
                target = target,
                content = content,
                occurredAt = ZonedDateTime.now(),
            )
        }

        /** 다른 서버에서 전파된 메시지를 원본 식별자/시간 그대로 복원한다. */
        fun reconstitute(
            messageId: String,
            senderId: Long,
            target: MessageTarget,
            content: String,
            occurredAt: ZonedDateTime,
        ): ChatMessage = ChatMessage(messageId, senderId, target, content, occurredAt)
    }
}
