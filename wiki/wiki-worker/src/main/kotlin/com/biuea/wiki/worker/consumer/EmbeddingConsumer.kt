package com.biuea.wiki.worker.consumer

import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.event.AiEmbeddingRequestEvent
import com.biuea.wiki.domain.event.AiProcessingFailedEvent
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.kafka.KafkaTopic
import com.biuea.wiki.worker.service.EmbeddingService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * EMBEDDING 에이전트
 *
 * 소비: queue.ai.embedding (목적성 — 이 그룹만 소비)
 * 완료 시: ai_status = COMPLETED로 전이 (별도 발행 없음. 필요 시 event.ai.embedding 추가 가능)
 * 실패 시: event.ai.failed 발행
 */
@Component
class EmbeddingConsumer(
    private val embeddingService: EmbeddingService,
    private val documentRepository: DocumentRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopic.QUEUE_AI_EMBEDDING],
        groupId = "wiki-worker-embedding",
        containerFactory = "kafkaListenerContainerFactory",
    )
    @Transactional
    fun consume(payload: Map<String, Any>, ack: Acknowledgment) {
        val event = objectMapper.convertValue(payload, AiEmbeddingRequestEvent::class.java)
        log.info("[EMBEDDING] 시작 documentId=${event.documentId}")

        val document = documentRepository.findByIdAndStatus(event.documentId, DocumentStatus.ACTIVE)
            ?: run {
                log.warn("[EMBEDDING] 문서 없음 documentId=${event.documentId}")
                ack.acknowledge()
                return
            }

        runCatching {
            embeddingService.embed(
                documentId = event.documentId,
                documentRevisionId = event.documentRevisionId,
            )

            document.completeAiProcessing()
            documentRepository.save(document)
            log.info("[EMBEDDING] 완료 → ai_status=COMPLETED documentId=${event.documentId}")
        }.onFailure { e ->
            log.error("[EMBEDDING] 실패 documentId=${event.documentId}", e)
            document.failAiProcessing()
            documentRepository.save(document)
            kafkaTemplate.send(
                KafkaTopic.EVENT_AI_FAILED,
                event.documentId.toString(),
                AiProcessingFailedEvent(
                    documentId = event.documentId,
                    documentRevisionId = event.documentRevisionId,
                    agentType = "EMBEDDING",
                    reason = e.message ?: "unknown",
                )
            )
        }

        ack.acknowledge()
    }
}
