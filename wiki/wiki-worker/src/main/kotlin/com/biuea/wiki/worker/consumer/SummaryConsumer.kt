package com.biuea.wiki.worker.consumer

import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.event.AiProcessingFailedEvent
import com.biuea.wiki.domain.event.AiTaggingRequestEvent
import com.biuea.wiki.domain.event.DocumentCreatedEvent
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.kafka.KafkaTopic
import com.biuea.wiki.worker.service.SummaryService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * SUMMARY 에이전트
 *
 * 소비: event.document (이벤트성 — 이 그룹 외 다른 그룹도 구독 가능)
 * 발행: queue.ai.tagging (목적성 — TAGGER 에이전트만 소비)
 */
@Component
class SummaryConsumer(
    private val summaryService: SummaryService,
    private val documentRepository: DocumentRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopic.EVENT_DOCUMENT],
        groupId = "wiki-worker-summary",
        containerFactory = "kafkaListenerContainerFactory",
    )
    @Transactional
    fun consume(payload: Map<String, Any>, ack: Acknowledgment) {
        val event = objectMapper.convertValue(payload, DocumentCreatedEvent::class.java)
        log.info("[SUMMARY] 시작 documentId=${event.documentId}")

        val document = documentRepository.findByIdAndStatus(event.documentId, DocumentStatus.ACTIVE)
            ?: run {
                log.warn("[SUMMARY] 문서 없음 documentId=${event.documentId}")
                ack.acknowledge()
                return
            }

        runCatching {
            document.startAiProcessing()
            documentRepository.save(document)

            val summary = summaryService.summarize(event.title, event.content)

            kafkaTemplate.send(
                KafkaTopic.QUEUE_AI_TAGGING,
                event.documentId.toString(),
                AiTaggingRequestEvent(
                    documentId = event.documentId,
                    documentRevisionId = event.documentRevisionId,
                    title = event.title,
                    summary = summary,
                )
            )
            log.info("[SUMMARY] 완료 → queue.ai.tagging 발행 documentId=${event.documentId}")
        }.onFailure { e ->
            log.error("[SUMMARY] 실패 documentId=${event.documentId}", e)
            kafkaTemplate.send(
                KafkaTopic.EVENT_AI_FAILED,
                event.documentId.toString(),
                AiProcessingFailedEvent(
                    documentId = event.documentId,
                    documentRevisionId = event.documentRevisionId,
                    agentType = "SUMMARY",
                    reason = e.message ?: "unknown",
                )
            )
        }

        ack.acknowledge()
    }
}
