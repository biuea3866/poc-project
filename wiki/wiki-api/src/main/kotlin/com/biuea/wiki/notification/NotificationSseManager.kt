package com.biuea.wiki.notification

import com.biuea.wiki.domain.notification.entity.Notification
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class NotificationSseManager(
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(NotificationSseManager::class.java)
    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    fun subscribe(userId: Long): SseEmitter {
        val emitter = SseEmitter(300_000L) // 5 minutes timeout
        emitters.getOrPut(userId) { CopyOnWriteArrayList() }.add(emitter)

        emitter.onCompletion { removeEmitter(userId, emitter) }
        emitter.onTimeout { removeEmitter(userId, emitter) }
        emitter.onError { removeEmitter(userId, emitter) }

        // Send initial connection confirmation
        try {
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"userId\":$userId}")
            )
        } catch (e: Exception) {
            logger.warn("Failed to send initial SSE event for user $userId: ${e.message}")
            removeEmitter(userId, emitter)
        }

        logger.debug("SSE subscription added for user $userId, total emitters: ${emitters[userId]?.size}")
        return emitter
    }

    fun sendToUser(userId: Long, notification: Notification) {
        val userEmitters = emitters[userId] ?: return
        val payload = objectMapper.writeValueAsString(
            NotificationSsePayload(
                id = notification.id,
                type = notification.type.name,
                refId = notification.refId,
                message = notification.message,
                createdAt = notification.createdAt.toString(),
            )
        )
        val staleEmitters = mutableListOf<SseEmitter>()
        userEmitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("notification")
                        .data(payload)
                )
            } catch (e: Exception) {
                logger.warn("Failed to send SSE notification to user $userId: ${e.message}")
                staleEmitters.add(emitter)
            }
        }
        staleEmitters.forEach { removeEmitter(userId, it) }
    }

    private fun removeEmitter(userId: Long, emitter: SseEmitter) {
        emitters[userId]?.remove(emitter)
        if (emitters[userId]?.isEmpty() == true) {
            emitters.remove(userId)
        }
        logger.debug("SSE emitter removed for user $userId")
    }

    data class NotificationSsePayload(
        val id: Long,
        val type: String,
        val refId: Long,
        val message: String,
        val createdAt: String,
    )
}
