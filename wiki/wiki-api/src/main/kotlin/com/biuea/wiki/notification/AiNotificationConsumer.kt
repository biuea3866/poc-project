package com.biuea.wiki.notification

import com.biuea.wiki.domain.event.AiProcessingCompletedEvent
import com.biuea.wiki.domain.event.AiProcessingFailedEvent
import com.biuea.wiki.domain.notification.entity.NotificationType
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.kafka.KafkaTopic
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * AI 완료/실패 이벤트를 수신하여 인앱 알림을 생성하는 컨슈머
 *
 * event.ai.completed — AI 처리 완료 시 알림 생성
 * event.ai.failed    — AI 처리 실패 시 알림 생성
 */
@Component
class AiNotificationConsumer(
    private val notificationAppService: NotificationAppService,
    private val documentRepository: DocumentRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopic.EVENT_AI_COMPLETED],
        groupId = "wiki-api-notification",
        containerFactory = "notificationKafkaListenerContainerFactory",
    )
    fun onAiCompleted(payload: Map<String, Any>, ack: Acknowledgment) {
        runCatching {
            val event = objectMapper.convertValue(payload, AiProcessingCompletedEvent::class.java)
            log.info("[NOTIFICATION] AI 완료 알림 생성 documentId=${event.documentId} userId=${event.targetUserId}")

            notificationAppService.createNotification(
                type = NotificationType.AI_COMPLETED,
                targetUserId = event.targetUserId,
                refId = event.documentId,
                message = "문서 AI 처리가 완료되었습니다.",
            )
        }.onFailure { e ->
            log.error("[NOTIFICATION] AI 완료 알림 생성 실패: ${e.message}", e)
        }
        ack.acknowledge()
    }

    @KafkaListener(
        topics = [KafkaTopic.EVENT_AI_FAILED],
        groupId = "wiki-api-notification",
        containerFactory = "notificationKafkaListenerContainerFactory",
    )
    fun onAiFailed(payload: Map<String, Any>, ack: Acknowledgment) {
        runCatching {
            val event = objectMapper.convertValue(payload, AiProcessingFailedEvent::class.java)
            log.error("[NOTIFICATION] AI 실패 알림 생성 documentId=${event.documentId} agent=${event.agentType}")

            // Lookup the document's owner (createdBy) for notification
            val document = documentRepository.findById(event.documentId).orElse(null)
            if (document == null) {
                log.warn("[NOTIFICATION] 문서를 찾을 수 없습니다 documentId=${event.documentId}")
                ack.acknowledge()
                return
            }

            notificationAppService.createNotification(
                type = NotificationType.AI_FAILED,
                targetUserId = document.createdBy,
                refId = event.documentId,
                message = "문서 AI 처리에 실패했습니다. (${event.agentType})",
            )
        }.onFailure { e ->
            log.error("[NOTIFICATION] AI 실패 알림 생성 실패: ${e.message}", e)
        }
        ack.acknowledge()
    }
}
