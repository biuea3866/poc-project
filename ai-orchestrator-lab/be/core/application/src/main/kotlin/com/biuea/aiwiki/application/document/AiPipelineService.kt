package com.biuea.aiwiki.application.document

import com.biuea.aiwiki.domain.document.model.AiStatus
import com.biuea.aiwiki.domain.document.port.DocumentRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AiPipelineService(
    private val documentRepository: DocumentRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Async
    fun processDocument(documentId: Long) {
        val document = requireNotNull(documentRepository.findById(documentId)) { "문서를 찾을 수 없습니다." }

        try {
            // PENDING → PROCESSING
            document.startProcessing()
            documentRepository.save(document)
            eventPublisher.publishEvent(AiStatusChangedEvent(documentId, AiStatus.PROCESSING))

            // Simulate AI processing (1 second delay)
            Thread.sleep(1000)

            // PROCESSING → COMPLETED with stub data
            document.completeAnalysis(
                summary = "AI generated summary",
                tags = listOf("tag1", "tag2"),
            )
            documentRepository.save(document)
            eventPublisher.publishEvent(AiStatusChangedEvent(documentId, AiStatus.COMPLETED))
        } catch (e: Exception) {
            document.failAnalysis()
            documentRepository.save(document)
            eventPublisher.publishEvent(AiStatusChangedEvent(documentId, AiStatus.FAILED))
        }
    }
}

data class AiStatusChangedEvent(
    val documentId: Long,
    val status: AiStatus,
)
