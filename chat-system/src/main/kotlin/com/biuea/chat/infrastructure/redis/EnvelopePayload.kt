package com.biuea.chat.infrastructure.redis

import com.biuea.chat.domain.message.ChatMessage
import com.biuea.chat.domain.message.MessageTarget
import com.biuea.chat.domain.routing.DeliveryEnvelope
import java.time.ZonedDateTime

/**
 * DeliveryEnvelope 의 Redis 전송용 JSON 표현. 도메인 객체를 그대로 직렬화하지 않고
 * 이 평면 DTO 로 변환해 브로커 경계 밖으로 Jackson 결합을 새지 않게 한다.
 */
data class EnvelopePayload(
    val recipientIds: List<Long>,
    val messageId: String,
    val senderId: Long,
    val targetType: String,
    val targetId: Long,
    val content: String,
    val occurredAt: String,
) {
    fun toEnvelope(): DeliveryEnvelope {
        val target = when (targetType) {
            DIRECT -> MessageTarget.Direct(targetId)
            ROOM -> MessageTarget.Room(targetId)
            else -> throw IllegalArgumentException("unknown targetType: $targetType")
        }
        val message = ChatMessage.reconstitute(
            messageId = messageId,
            senderId = senderId,
            target = target,
            content = content,
            occurredAt = ZonedDateTime.parse(occurredAt),
        )
        return DeliveryEnvelope(recipientIds, message)
    }

    companion object {
        private const val DIRECT = "direct"
        private const val ROOM = "room"

        fun from(envelope: DeliveryEnvelope): EnvelopePayload {
            val message = envelope.message
            val (targetType, targetId) = when (val target = message.target) {
                is MessageTarget.Direct -> DIRECT to target.receiverId
                is MessageTarget.Room -> ROOM to target.roomId
            }
            return EnvelopePayload(
                recipientIds = envelope.recipientIds,
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
