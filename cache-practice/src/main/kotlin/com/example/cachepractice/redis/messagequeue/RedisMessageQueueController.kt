package com.example.cachepractice.redis.messagequeue

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*

/**
 * Redis Pub/Sub 예제 API
 */
@RestController
@RequestMapping("/api/redis/message-queue")
@Profile("redis")
class RedisMessageQueueController(
    private val messageQueueService: RedisMessageQueueService
) {

    /**
     * 채널 구독
     * POST /api/redis/message-queue/subscribe
     */
    @PostMapping("/subscribe")
    fun subscribe(@RequestBody request: SubscribeRequest): Map<String, String> {
        messageQueueService.subscribe(request.channel)
        return mapOf(
            "message" to "채널 구독 시작",
            "channel" to request.channel
        )
    }

    /**
     * 메시지 발행
     * POST /api/redis/message-queue/publish
     */
    @PostMapping("/publish")
    fun publish(@RequestBody request: PublishRequest): Map<String, Any> {
        messageQueueService.publish(request.channel, request.message)
        return mapOf(
            "message" to "메시지 발행 완료",
            "channel" to request.channel,
            "publishedMessage" to request.message
        )
    }

    /**
     * 채팅방 구독
     * POST /api/redis/message-queue/chat/subscribe
     */
    @PostMapping("/chat/subscribe")
    fun subscribeChatRoom(@RequestBody request: ChatSubscribeRequest): Map<String, String> {
        messageQueueService.subscribeChatRoom(request.roomId)
        return mapOf(
            "message" to "채팅방 구독 시작",
            "roomId" to request.roomId
        )
    }

    /**
     * 채팅 메시지 발행
     * POST /api/redis/message-queue/chat/send
     */
    @PostMapping("/chat/send")
    fun sendChatMessage(@RequestBody chatMessage: ChatMessage): Map<String, Any> {
        messageQueueService.publishChatMessage(chatMessage.roomId, chatMessage)
        return mapOf(
            "message" to "채팅 메시지 전송 완료",
            "roomId" to chatMessage.roomId,
            "sender" to chatMessage.senderName,
            "content" to chatMessage.message
        )
    }

    /**
     * 채팅 메시지 조회
     * GET /api/redis/message-queue/chat/{roomId}
     */
    @GetMapping("/chat/{roomId}")
    fun getChatMessages(@PathVariable roomId: String): Map<String, Any> {
        val messages = messageQueueService.getChatMessages(roomId)
        return mapOf(
            "roomId" to roomId,
            "messageCount" to messages.size,
            "messages" to messages
        )
    }

    /**
     * 사용자 알림 구독
     * POST /api/redis/message-queue/notification/subscribe
     */
    @PostMapping("/notification/subscribe")
    fun subscribeNotification(@RequestBody request: NotificationSubscribeRequest): Map<String, Any> {
        messageQueueService.subscribeUserNotification(request.userId)
        return mapOf(
            "message" to "사용자 알림 구독 시작",
            "userId" to request.userId
        )
    }

    /**
     * 알림 발행
     * POST /api/redis/message-queue/notification/send
     */
    @PostMapping("/notification/send")
    fun sendNotification(@RequestBody notification: Notification): Map<String, Any> {
        messageQueueService.publishNotification(notification.userId, notification)
        return mapOf(
            "message" to "알림 발행 완료",
            "userId" to notification.userId,
            "type" to notification.type,
            "title" to notification.title
        )
    }

    /**
     * 알림 조회
     * GET /api/redis/message-queue/notification/{userId}
     */
    @GetMapping("/notification/{userId}")
    fun getNotifications(@PathVariable userId: Long): Map<String, Any> {
        val notifications = messageQueueService.getNotifications(userId)
        return mapOf(
            "userId" to userId,
            "notificationCount" to notifications.size,
            "notifications" to notifications
        )
    }

    /**
     * 이벤트 구독
     * POST /api/redis/message-queue/event/subscribe
     */
    @PostMapping("/event/subscribe")
    fun subscribeEvent(@RequestBody request: EventSubscribeRequest): Map<String, String> {
        messageQueueService.subscribeEvent(request.eventType)
        return mapOf(
            "message" to "이벤트 구독 시작",
            "eventType" to request.eventType
        )
    }

    /**
     * 이벤트 발행
     * POST /api/redis/message-queue/event/publish
     */
    @PostMapping("/event/publish")
    fun publishEvent(@RequestBody event: Event): Map<String, Any> {
        messageQueueService.publishEvent(event.eventType, event)
        return mapOf(
            "message" to "이벤트 발행 완료",
            "eventType" to event.eventType,
            "data" to event.data
        )
    }

    /**
     * 이벤트 조회
     * GET /api/redis/message-queue/event/{eventType}
     */
    @GetMapping("/event/{eventType}")
    fun getEvents(@PathVariable eventType: String): Map<String, Any> {
        val events = messageQueueService.getEvents(eventType)
        return mapOf(
            "eventType" to eventType,
            "eventCount" to events.size,
            "events" to events
        )
    }

    /**
     * 수신한 메시지 조회
     * GET /api/redis/message-queue/received/{channel}
     */
    @GetMapping("/received/{channel}")
    fun getReceivedMessages(@PathVariable channel: String): Map<String, Any> {
        val messages = messageQueueService.getReceivedMessages(channel)
        return mapOf(
            "channel" to channel,
            "messageCount" to messages.size,
            "messages" to messages
        )
    }

    /**
     * 모든 메시지 초기화
     * DELETE /api/redis/message-queue/clear
     */
    @DeleteMapping("/clear")
    fun clearAllMessages(): Map<String, String> {
        messageQueueService.clearAllMessages()
        return mapOf("message" to "모든 메시지 초기화 완료")
    }
}

data class SubscribeRequest(
    val channel: String
)

data class PublishRequest(
    val channel: String,
    val message: String
)

data class ChatSubscribeRequest(
    val roomId: String
)

data class NotificationSubscribeRequest(
    val userId: Long
)

data class EventSubscribeRequest(
    val eventType: String
)
