package com.biuea.wiki.application

import com.biuea.wiki.domain.document.DocumentService
import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.infrastructure.kafka.KafkaTopic
import com.biuea.wiki.infrastructure.kafka.OutboxKafkaPublisher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

/**
 * NAW-128 AC6 검증
 *
 * AC6: 기존 AI 파이프라인 동작에 영향을 주지 않는다 (하위 호환)
 * — publishDocument 호출 시 OutboxKafkaPublisher를 통해 event.document 토픽에 발행된다
 */
@ExtendWith(MockitoExtension::class)
class PublishDocumentFacadeTest {

    @Mock
    private lateinit var documentService: DocumentService

    @Mock
    private lateinit var outboxKafkaPublisher: OutboxKafkaPublisher

    private lateinit var facade: PublishDocumentFacade

    @BeforeEach
    fun setUp() {
        facade = PublishDocumentFacade(documentService, outboxKafkaPublisher)
    }

    private fun createDocument(): Document {
        val doc = Document(
            title = "Test Document",
            content = "Test Content",
            status = DocumentStatus.COMPLETED,
            createdBy = 1L,
            updatedBy = 1L,
        )
        val revision = DocumentRevision.create(doc)
        doc.addRevision(revision)
        return doc
    }

    // AC6: publish 호출 시 OutboxKafkaPublisher를 통해 event.document 이벤트를 발행한다
    @Test
    fun `publish sends document event via OutboxKafkaPublisher`() {
        val document = createDocument()
        whenever(documentService.publishDocument(1L, 100L)).thenReturn(document)

        facade.publish(1L, 100L)

        verify(outboxKafkaPublisher).publish(
            topic = eq(KafkaTopic.EVENT_DOCUMENT),
            key = any(),
            payload = any(),
            aggregateType = eq("Document"),
            aggregateId = any(),
        )
    }

    // AC6: reanalyze 호출 시에도 OutboxKafkaPublisher를 통해 event.document 이벤트를 발행한다
    @Test
    fun `reanalyze sends document event via OutboxKafkaPublisher`() {
        val document = createDocument()
        whenever(documentService.publishDocument(1L)).thenReturn(document)

        facade.reanalyze(1L)

        verify(outboxKafkaPublisher).publish(
            topic = eq(KafkaTopic.EVENT_DOCUMENT),
            key = any(),
            payload = any(),
            aggregateType = eq("Document"),
            aggregateId = any(),
        )
    }

    // AC6: publish 반환값이 documentService.publishDocument 결과와 동일하다
    @Test
    fun `publish returns document from documentService`() {
        val document = createDocument()
        whenever(documentService.publishDocument(1L, 100L)).thenReturn(document)

        val result = facade.publish(1L, 100L)

        assertEquals(document, result)
    }
}
