package com.biuea.wiki.application

import com.biuea.wiki.domain.document.DocumentService
import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.event.DocumentCreatedEvent
import com.biuea.wiki.infrastructure.kafka.KafkaTopic
import com.biuea.wiki.infrastructure.kafka.OutboxKafkaPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 문서 발행 퍼사드.
 *
 * 문서를 ACTIVE 상태로 전환하고, OutboxKafkaPublisher를 통해
 * Kafka 이벤트를 발행한다. Kafka 발행 실패 시 outbox 테이블에 적재하여
 * at-least-once delivery를 보장한다.
 */
@Component
class PublishDocumentFacade(
    private val documentService: DocumentService,
    private val outboxKafkaPublisher: OutboxKafkaPublisher,
) {
    @Transactional
    fun publish(documentId: Long, userId: Long): Document {
        val document = documentService.publishDocument(documentId, userId)

        val event = DocumentCreatedEvent(
            documentId = document.id,
            documentRevisionId = document.latestRevisionId,
            title = document.title,
            content = document.content,
        )

        outboxKafkaPublisher.publish(
            topic = KafkaTopic.EVENT_DOCUMENT,
            key = document.id.toString(),
            payload = event,
            aggregateType = "Document",
            aggregateId = document.id.toString(),
        )

        return document
    }

    /**
     * 수동 재분석 요청 시 호출.
     * 문서 상태를 PENDING으로 리셋 후 이벤트를 재발행한다.
     */
    @Transactional
    fun reanalyze(documentId: Long): Document {
        val document = documentService.publishDocument(documentId)

        val event = DocumentCreatedEvent(
            documentId = document.id,
            documentRevisionId = document.latestRevisionId,
            title = document.title,
            content = document.content,
        )

        outboxKafkaPublisher.publish(
            topic = KafkaTopic.EVENT_DOCUMENT,
            key = document.id.toString(),
            payload = event,
            aggregateType = "Document",
            aggregateId = document.id.toString(),
        )

        return document
    }
}
