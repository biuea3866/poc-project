package com.biuea.wiki.worker.consumer

import com.biuea.wiki.domain.event.AiEmbeddingRequestEvent
import com.biuea.wiki.domain.event.AiProcessingFailedEvent
import com.biuea.wiki.domain.event.AiTaggingRequestEvent
import com.biuea.wiki.domain.tag.SaveTagCommand
import com.biuea.wiki.domain.tag.TagService
import com.biuea.wiki.domain.tag.entity.TagConstant
import com.biuea.wiki.infrastructure.kafka.KafkaTopic
import com.biuea.wiki.worker.service.TaggerService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * TAGGER 에이전트
 *
 * 소비: queue.ai.tagging (목적성 — 이 그룹만 소비)
 * 발행: queue.ai.embedding (목적성 — EMBEDDING 에이전트만 소비)
 */
@Component
class TaggerConsumer(
    private val taggerService: TaggerService,
    private val tagService: TagService,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopic.QUEUE_AI_TAGGING],
        groupId = "wiki-worker-tagger",
        containerFactory = "kafkaListenerContainerFactory",
    )
    @Transactional
    fun consume(payload: Map<String, Any>, ack: Acknowledgment) {
        val event = objectMapper.convertValue(payload, AiTaggingRequestEvent::class.java)
        log.info("[TAGGER] 시작 documentId=${event.documentId}")

        runCatching {
            val tagItems = taggerService.extractTags(event.title, event.summary)

            tagService.saveTags(
                SaveTagCommand(
                    tags = tagItems.map {
                        SaveTagCommand.TagInput(
                            name = it.name,
                            tagConstant = TagConstant.valueOf(it.tagConstant),
                        )
                    },
                    documentId = event.documentId,
                    documentRevisionId = event.documentRevisionId,
                )
            )

            kafkaTemplate.send(
                KafkaTopic.QUEUE_AI_EMBEDDING,
                event.documentId.toString(),
                AiEmbeddingRequestEvent(
                    documentId = event.documentId,
                    documentRevisionId = event.documentRevisionId,
                    tags = tagItems.map { AiEmbeddingRequestEvent.TagItem(it.name, it.tagConstant) },
                )
            )
            log.info("[TAGGER] 완료 → queue.ai.embedding 발행 documentId=${event.documentId}")
        }.onFailure { e ->
            log.error("[TAGGER] 실패 documentId=${event.documentId}", e)
            kafkaTemplate.send(
                KafkaTopic.EVENT_AI_FAILED,
                event.documentId.toString(),
                AiProcessingFailedEvent(
                    documentId = event.documentId,
                    documentRevisionId = event.documentRevisionId,
                    agentType = "TAGGER",
                    reason = e.message ?: "unknown",
                )
            )
        }

        ack.acknowledge()
    }
}
