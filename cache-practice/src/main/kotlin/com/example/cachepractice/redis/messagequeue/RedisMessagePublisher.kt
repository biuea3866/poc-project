package com.example.cachepractice.redis.messagequeue

import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

/**
 * Redis Pub/Sub - Publisher
 * 특정 채널로 메시지를 발행
 */
@Service
@Profile("redis")
class RedisMessagePublisher(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    /**
     * 채널로 메시지 발행
     */
    fun publish(channel: String, message: Any) {
        redisTemplate.convertAndSend(channel, message)
        println("메시지 발행 완료 - Channel: $channel, Message: $message")
    }

    /**
     * 채팅 메시지 발행
     */
    fun publishChatMessage(roomId: String, chatMessage: ChatMessage) {
        val channel = "chat:room:$roomId"
        publish(channel, chatMessage)
    }

    /**
     * 알림 메시지 발행
     */
    fun publishNotification(userId: Long, notification: Notification) {
        val channel = "notification:user:$userId"
        publish(channel, notification)
    }

    /**
     * 이벤트 발행
     */
    fun publishEvent(eventType: String, event: Event) {
        val channel = "event:$eventType"
        publish(channel, event)
    }
}

data class ChatMessage(
    val roomId: String,
    val senderId: Long,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Notification(
    val userId: Long,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Event(
    val eventType: String,
    val data: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)
