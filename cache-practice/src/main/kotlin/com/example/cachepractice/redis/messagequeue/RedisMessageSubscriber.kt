package com.example.cachepractice.redis.messagequeue

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Redis Pub/Sub - Subscriber
 * 특정 채널을 구독하고 메시지를 수신
 */
@Component
@Profile("redis")
class RedisMessageSubscriber : MessageListener {

    private val objectMapper = ObjectMapper()
    private val receivedMessages = ConcurrentHashMap<String, MutableList<Any>>()

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val channel = String(message.channel)
        val body = String(message.body)

        println("메시지 수신 - Channel: $channel, Body: $body")

        // 채널별 메시지 저장
        receivedMessages.computeIfAbsent(channel) { mutableListOf() }.add(body)
    }

    fun getReceivedMessages(channel: String): List<Any> {
        return receivedMessages[channel]?.toList() ?: emptyList()
    }

    fun clearMessages() {
        receivedMessages.clear()
    }
}

/**
 * 채팅 메시지 전용 구독자
 */
@Component
@Profile("redis")
class ChatMessageSubscriber : MessageListener {

    private val objectMapper = ObjectMapper()
    private val chatMessages = ConcurrentHashMap<String, MutableList<ChatMessage>>()

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val channel = String(message.channel)
        val body = String(message.body)

        try {
            val chatMessage = objectMapper.readValue<ChatMessage>(body)
            println("채팅 메시지 수신 - Room: ${chatMessage.roomId}, Sender: ${chatMessage.senderName}, Message: ${chatMessage.message}")

            chatMessages.computeIfAbsent(channel) { mutableListOf() }.add(chatMessage)
        } catch (e: Exception) {
            println("채팅 메시지 파싱 실패: ${e.message}")
        }
    }

    fun getChatMessages(roomId: String): List<ChatMessage> {
        val channel = "chat:room:$roomId"
        return chatMessages[channel]?.toList() ?: emptyList()
    }

    fun clearMessages() {
        chatMessages.clear()
    }
}

/**
 * 알림 메시지 전용 구독자
 */
@Component
@Profile("redis")
class NotificationSubscriber : MessageListener {

    private val objectMapper = ObjectMapper()
    private val notifications = ConcurrentHashMap<Long, MutableList<Notification>>()

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val channel = String(message.channel)
        val body = String(message.body)

        try {
            val notification = objectMapper.readValue<Notification>(body)
            println("알림 수신 - User: ${notification.userId}, Type: ${notification.type}, Title: ${notification.title}")

            notifications.computeIfAbsent(notification.userId) { mutableListOf() }.add(notification)
        } catch (e: Exception) {
            println("알림 메시지 파싱 실패: ${e.message}")
        }
    }

    fun getNotifications(userId: Long): List<Notification> {
        return notifications[userId]?.toList() ?: emptyList()
    }

    fun clearNotifications() {
        notifications.clear()
    }
}

/**
 * 이벤트 전용 구독자
 */
@Component
@Profile("redis")
class EventSubscriber : MessageListener {

    private val objectMapper = ObjectMapper()
    private val events = ConcurrentHashMap<String, MutableList<Event>>()

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val channel = String(message.channel)
        val body = String(message.body)

        try {
            val event = objectMapper.readValue<Event>(body)
            println("이벤트 수신 - Type: ${event.eventType}, Data: ${event.data}")

            events.computeIfAbsent(event.eventType) { mutableListOf() }.add(event)
        } catch (e: Exception) {
            println("이벤트 메시지 파싱 실패: ${e.message}")
        }
    }

    fun getEvents(eventType: String): List<Event> {
        return events[eventType]?.toList() ?: emptyList()
    }

    fun clearEvents() {
        events.clear()
    }
}
