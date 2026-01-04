package com.example.cachepractice.redis.messagequeue

import org.springframework.context.annotation.Profile
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Service

/**
 * Redis Pub/Sub 서비스
 * 채널 구독 관리 및 메시지 발행
 */
@Service
@Profile("redis")
class RedisMessageQueueService(
    private val redisMessageListenerContainer: RedisMessageListenerContainer,
    private val messagePublisher: RedisMessagePublisher,
    private val messageSubscriber: RedisMessageSubscriber,
    private val chatMessageSubscriber: ChatMessageSubscriber,
    private val notificationSubscriber: NotificationSubscriber,
    private val eventSubscriber: EventSubscriber
) {

    /**
     * 채널 구독
     */
    fun subscribe(channel: String) {
        redisMessageListenerContainer.addMessageListener(
            messageSubscriber,
            ChannelTopic(channel)
        )
        println("채널 구독 시작: $channel")
    }

    /**
     * 채팅방 구독
     */
    fun subscribeChatRoom(roomId: String) {
        val channel = "chat:room:$roomId"
        redisMessageListenerContainer.addMessageListener(
            chatMessageSubscriber,
            ChannelTopic(channel)
        )
        println("채팅방 구독 시작: $roomId")
    }

    /**
     * 사용자 알림 구독
     */
    fun subscribeUserNotification(userId: Long) {
        val channel = "notification:user:$userId"
        redisMessageListenerContainer.addMessageListener(
            notificationSubscriber,
            ChannelTopic(channel)
        )
        println("사용자 알림 구독 시작: User $userId")
    }

    /**
     * 이벤트 타입 구독
     */
    fun subscribeEvent(eventType: String) {
        val channel = "event:$eventType"
        redisMessageListenerContainer.addMessageListener(
            eventSubscriber,
            ChannelTopic(channel)
        )
        println("이벤트 구독 시작: $eventType")
    }

    /**
     * 메시지 발행
     */
    fun publish(channel: String, message: Any) {
        messagePublisher.publish(channel, message)
    }

    /**
     * 채팅 메시지 발행
     */
    fun publishChatMessage(roomId: String, chatMessage: ChatMessage) {
        messagePublisher.publishChatMessage(roomId, chatMessage)
    }

    /**
     * 알림 발행
     */
    fun publishNotification(userId: Long, notification: Notification) {
        messagePublisher.publishNotification(userId, notification)
    }

    /**
     * 이벤트 발행
     */
    fun publishEvent(eventType: String, event: Event) {
        messagePublisher.publishEvent(eventType, event)
    }

    /**
     * 수신한 메시지 조회
     */
    fun getReceivedMessages(channel: String): List<Any> {
        return messageSubscriber.getReceivedMessages(channel)
    }

    fun getChatMessages(roomId: String): List<ChatMessage> {
        return chatMessageSubscriber.getChatMessages(roomId)
    }

    fun getNotifications(userId: Long): List<Notification> {
        return notificationSubscriber.getNotifications(userId)
    }

    fun getEvents(eventType: String): List<Event> {
        return eventSubscriber.getEvents(eventType)
    }

    /**
     * 메시지 초기화
     */
    fun clearAllMessages() {
        messageSubscriber.clearMessages()
        chatMessageSubscriber.clearMessages()
        notificationSubscriber.clearNotifications()
        eventSubscriber.clearEvents()
    }
}
