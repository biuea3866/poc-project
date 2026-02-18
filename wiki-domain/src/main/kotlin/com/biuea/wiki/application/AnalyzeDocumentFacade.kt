package com.biuea.wiki.application

import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.event.DocumentCreatedEvent
import com.biuea.wiki.domain.event.DocumentEventPublisher
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AnalyzeDocumentFacade(
    private val documentRepository: DocumentRepository,
    private val documentEventPublisher: DocumentEventPublisher,
) {
    @Transactional
    fun analyze(input: AnalyzeDocumentInput) {
        val document = documentRepository.findByIdAndStatus(input.documentId, DocumentStatus.ACTIVE)
            ?: throw IllegalArgumentException("활성화된 문서를 찾을 수 없습니다. id=${input.documentId}")

        check(document.createdBy == input.userId) { "문서 재분석 권한이 없습니다." }

        document.resetAiProcessing()
        documentRepository.save(document)

        val latestRevision = document.latestRevision()
            ?: throw IllegalStateException("리비전이 없습니다. documentId=${input.documentId}")

        documentEventPublisher.publishDocumentCreated(
            DocumentCreatedEvent(
                documentId = document.id,
                documentRevisionId = latestRevision.id,
                title = document.title,
                content = document.content,
                userId = document.createdBy,
            )
        )
    }
}

data class AnalyzeDocumentInput(
    val documentId: Long,
    val userId: Long,
)
