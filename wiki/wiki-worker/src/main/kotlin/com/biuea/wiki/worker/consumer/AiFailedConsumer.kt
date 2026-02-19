package com.biuea.wiki.worker.consumer

import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.event.AiProcessingFailedEvent
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.kafka.KafkaTopic
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * AI 실패 처리 컨슈머
 *
 * 소비: event.ai.failed (이벤트성 — 여러 컨슈머 그룹이 반응 가능)
 * 역할: ai_status = FAILED로 전이. 추후 알림/재시도 로직 추가 가능.
 */
@Component
class AiFailedConsumer(
    private val documentRepository: DocumentRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopic.EVENT_AI_FAILED],
        groupId = "wiki-worker-ai-status",
        containerFactory = "kafkaListenerContainerFactory",
    )
    @Transactional
    fun consume(payload: Map<String, Any>, ack: Acknowledgment) {
        val event = objectMapper.convertValue(payload, AiProcessingFailedEvent::class.java)
        log.error("[AI_FAILED] agentType=${event.agentType} documentId=${event.documentId} reason=${event.reason}")

        val document = documentRepository.findByIdAndStatus(event.documentId, DocumentStatus.ACTIVE)
        document?.let {
            it.failAiProcessing()
            documentRepository.save(it)
            log.info("[AI_FAILED] ai_status=FAILED 처리 완료 documentId=${event.documentId}")
        }

        ack.acknowledge()
    }
}
