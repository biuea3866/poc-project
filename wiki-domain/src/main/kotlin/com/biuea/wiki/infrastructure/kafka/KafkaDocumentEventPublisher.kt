package com.biuea.wiki.infrastructure.kafka

import com.biuea.wiki.domain.event.AiProcessingFailedEvent
import com.biuea.wiki.domain.event.DocumentCreatedEvent
import com.biuea.wiki.domain.event.DocumentEventPublisher
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaDocumentEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : DocumentEventPublisher {

    override fun publishDocumentCreated(event: DocumentCreatedEvent) {
        kafkaTemplate.send(KafkaTopic.EVENT_DOCUMENT, event.documentId.toString(), event)
    }

    override fun publishAiProcessingFailed(event: AiProcessingFailedEvent) {
        kafkaTemplate.send(KafkaTopic.EVENT_AI_FAILED, event.documentId.toString(), event)
    }
}

/**
 * Kafka 토픽 네이밍 컨벤션
 *
 * event.{domain}          — 이벤트성. 여러 컨슈머 그룹이 각자의 비즈니스를 처리 (pub/sub)
 * queue.{domain}.{action} — 목적성. 단일 컨슈머 그룹이 하나의 비즈니스를 처리 (point-to-point)
 */
object KafkaTopic {
    // event: 문서가 발행됨 → 여러 컨슈머 그룹이 반응 가능 (SUMMARY 외 미래 확장)
    const val EVENT_DOCUMENT = "event.document"

    // event: AI 처리 실패 → 상태 업데이터, 알림 등 여러 그룹이 처리 가능
    const val EVENT_AI_FAILED = "event.ai.failed"

    // queue: TAGGER 에이전트만 소비 (SUMMARY 결과를 받아 태깅 수행)
    const val QUEUE_AI_TAGGING = "queue.ai.tagging"

    // queue: EMBEDDING 에이전트만 소비 (TAGGER 결과를 받아 임베딩 수행)
    const val QUEUE_AI_EMBEDDING = "queue.ai.embedding"
}
